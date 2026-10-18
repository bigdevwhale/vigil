package app.vigil.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.vigil.MainActivity
import app.vigil.R
import app.vigil.container
import app.vigil.data.Settings
import app.vigil.data.Vow
import app.vigil.domain.Vows
import app.vigil.domain.gendered
import app.vigil.localized
import app.vigil.ui.T
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Three kinds of notice:
 * the morning rule (today's vows read out, with a warning on the hard days),
 * the evening examen (how was the day), and the moments of a vow (it begins, it is fulfilled,
 * the night you asked to sleep on it is over).
 */
object Alarms {
    private const val CHANNEL_RULE = "rule_v1"
    private const val CHANNEL_MOMENT = "moment_v1"
    private const val ID_MORNING = 1
    private const val ID_EXAMEN = 2
    private const val ID_MOMENT = 3
    internal const val MORNING = "app.vigil.notify.MORNING"
    internal const val EXAMEN = "app.vigil.notify.EXAMEN"
    internal const val MOMENT = "app.vigil.notify.MOMENT"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val l = context.localized()
        if (nm.getNotificationChannel(CHANNEL_RULE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_RULE, l.getString(R.string.channel_rule), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = l.getString(R.string.channel_rule_desc)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_MOMENT) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_MOMENT, l.getString(R.string.channel_moment), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = l.getString(R.string.channel_moment_desc)
                }
            )
        }
    }

    private fun pi(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code,
        Intent(context, AlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** The next time the clock shows [minutes] after midnight. */
    private fun nextAt(minutes: Int, zone: ZoneId): Long {
        val now = LocalDateTime.now(zone)
        var t = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.atZone(zone).toInstant().toEpochMilli()
    }

    suspend fun reschedule(context: Context) {
        val c = context.container
        val s = c.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val vows = c.db.dao().vows().filter { it.active }
        // None of these needs to-the-second precision; inexact alarms avoid the exact-alarm permission.
        fun arm(action: String, code: Int, at: Long?) {
            val p = pi(context, action, code)
            if (at == null || !s.onboarded || vows.isEmpty()) am.cancel(p) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        }
        arm(MORNING, 1, if (s.morning) nextAt(s.morningAt, zone) else null)
        arm(EXAMEN, 2, if (s.examen) nextAt(s.examenAt, zone) else null)
        val moments = vows.flatMap { v -> listOfNotNull(v.startAt, Vows.endAt(v), v.breakAt) }.filter { it > now }
        arm(MOMENT, 3, moments.minOrNull()?.plus(1000))
    }

    private fun open(context: Context, code: Int) = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun post(context: Context, channel: String, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, 100 + id))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    private fun g(s: Settings, text: String) = gendered(text, s.gender)

    /** The morning rule: each vow and its day, and a word of warning on the hard days. */
    suspend fun morning(context: Context) {
        val c = context.container
        val s = c.settings.now()
        if (!s.morning || !s.onboarded) return
        val now = System.currentTimeMillis()
        val vows = c.db.dao().vows().filter { it.active && Vows.started(it, now) }
        if (vows.isEmpty()) return
        val l = context.localized()
        val lines = vows.map { v -> v.title + " · " + T.dayOf(l, v, now) }
        val demon = vows.firstNotNullOfOrNull { v -> Vows.demon(v, now)?.let { v to it } }
        val text = buildString {
            append(lines.joinToString("\n"))
            if (demon != null) append("\n\n").append(g(s, T.demon(l, demon.second)))
        }
        post(context, CHANNEL_RULE, ID_MORNING, g(s, l.getString(R.string.notif_morning_title)), text)
    }

    /** The evening examen, if a vow's day hasn't been answered yet. */
    suspend fun examen(context: Context) {
        val c = context.container
        val s = c.settings.now()
        if (!s.examen || !s.onboarded) return
        val now = System.currentTimeMillis()
        val events = c.db.dao().events()
        val open = c.db.dao().vows().filter { it.active && Vows.started(it, now) && !Vows.examined(it, events, now) }
        if (open.isEmpty()) return
        val l = context.localized()
        post(context, CHANNEL_RULE, ID_EXAMEN, g(s, l.getString(R.string.notif_examen_title)), g(s, l.getString(R.string.notif_examen_text, open.joinToString(", ") { it.title })))
    }

    /** A vow begins, is fulfilled, or the night of "sleep on it" is over; each said once. */
    suspend fun moment(context: Context) {
        val c = context.container
        val s = c.settings.now()
        if (!s.onboarded) return
        val now = System.currentTimeMillis()
        c.keeper.sweep(now)
        val l = context.localized()
        val window = 6 * 3_600_000L
        val said = mutableSetOf<String>()
        c.db.dao().vows().forEach { v: Vow ->
            val end = Vows.endAt(v)
            when {
                end != null && v.endedAt == end && !v.endSeen && now - end in 0..window && "end:${v.id}" !in s.seen -> {
                    said += "end:${v.id}"
                    post(context, CHANNEL_MOMENT, ID_MOMENT, g(s, l.getString(R.string.notif_end_title, v.title)), g(s, l.getString(R.string.notif_end_text)))
                }
                v.active && now - v.startAt in 0..window && v.startAt > v.createdAt + 60_000 && "start:${v.id}" !in s.seen -> {
                    said += "start:${v.id}"
                    post(context, CHANNEL_MOMENT, ID_MOMENT + 1, g(s, l.getString(R.string.notif_start_title, v.title)), g(s, l.getString(R.string.notif_start_text)))
                }
                v.active && Vows.delayOver(v, now) && "break:${v.id}:${v.breakAt}" !in s.seen -> {
                    said += "break:${v.id}:${v.breakAt}"
                    post(context, CHANNEL_MOMENT, ID_MOMENT + 2, g(s, l.getString(R.string.notif_break_title)), g(s, l.getString(R.string.notif_break_text, v.title)))
                }
            }
        }
        if (said.isNotEmpty()) c.settings.update { it.copy(seen = it.seen + said) }
    }
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf(Alarms.MORNING, Alarms.EXAMEN, Alarms.MOMENT)) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                when (action) {
                    Alarms.MORNING -> Alarms.morning(context)
                    Alarms.EXAMEN -> Alarms.examen(context)
                    else -> Alarms.moment(context)
                }
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}
