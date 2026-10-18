package app.vigil.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import app.vigil.MainActivity
import app.vigil.R
import app.vigil.container
import app.vigil.data.Vow
import app.vigil.domain.Vows
import app.vigil.domain.gendered
import app.vigil.localized
import app.vigil.ui.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The candle on the home screen: the vow furthest along, its day, and a candle burnt down that far. */
class CandleWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, CandleWidget::class.java))
            if (ids.isEmpty()) return
            val c = context.container
            val s = c.settings.now()
            val now = System.currentTimeMillis()
            val l = context.localized()
            val vows = c.db.dao().vows().filter { it.active }
            // The one with an end that has gone furthest, else an open-ended one, else the next to begin.
            val v: Vow? = vows.filter { Vows.started(it, now) }.maxByOrNull { Vows.progress(it, now) + if (Vows.total(it) == null) 0f else 1f }
                ?: vows.minByOrNull { it.startAt }
            val views = RemoteViews(context.packageName, R.layout.widget_candle)
            if (v == null) {
                views.setTextViewText(R.id.title, l.getString(R.string.widget_none))
                views.setTextViewText(R.id.day, gendered(l.getString(R.string.widget_none_sub), s.gender))
                views.setImageViewBitmap(R.id.candle, candle(0f, lit = false))
            } else {
                views.setTextViewText(R.id.title, v.title)
                views.setTextViewText(R.id.day, if (Vows.started(v, now)) T.dayOf(l, v, now) else l.getString(R.string.widget_waiting))
                views.setImageViewBitmap(R.id.candle, candle(Vows.progress(v, now), lit = Vows.started(v, now)))
            }
            views.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(context, 7, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
            manager.updateAppWidget(ids, views)
        }

        /** A wax candle burnt down by [burnt] (0..1), lit or not. */
        fun candle(burnt: Float, lit: Boolean): Bitmap {
            val w = 120
            val h = 220
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val cv = Canvas(b)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val bottom = h - 14f
            val full = 150f
            val bodyH = (full * (1f - 0.82f * burnt.coerceIn(0f, 1f))).coerceAtLeast(22f)
            val top = bottom - bodyH
            val cx = w / 2f
            if (lit) {
                p.shader = RadialGradient(cx, top - 26f, 58f, intArrayOf(0x66FFC75A, 0x00FFC75A), null, Shader.TileMode.CLAMP)
                cv.drawCircle(cx, top - 26f, 58f, p)
            }
            // The holder.
            p.shader = null
            p.color = 0xFF7A5A2E.toInt()
            cv.drawOval(RectF(cx - 46f, bottom - 8f, cx + 46f, bottom + 10f), p)
            // The body.
            p.shader = LinearGradient(cx - 20f, 0f, cx + 20f, 0f, intArrayOf(0xFFE9DCC0.toInt(), 0xFFFFF7E6.toInt(), 0xFFD3C3A2.toInt()), null, Shader.TileMode.CLAMP)
            cv.drawRect(cx - 20f, top, cx + 20f, bottom, p)
            cv.drawOval(RectF(cx - 20f, top - 5f, cx + 20f, top + 5f), p)
            // A drip.
            p.shader = null
            p.color = 0xFFF6EBD3.toInt()
            cv.drawRoundRect(RectF(cx + 8f, top - 2f, cx + 15f, top + bodyH * 0.35f), 4f, 4f, p)
            // The wick.
            p.color = 0xFF3A2A1C.toInt()
            cv.drawRect(cx - 1.5f, top - 10f, cx + 1.5f, top, p)
            if (lit) {
                val f = Path().apply {
                    moveTo(cx, top - 50f)
                    cubicTo(cx + 15f, top - 30f, cx + 12f, top - 10f, cx, top - 8f)
                    cubicTo(cx - 12f, top - 10f, cx - 15f, top - 30f, cx, top - 50f)
                }
                p.shader = LinearGradient(cx, top - 50f, cx, top - 8f, intArrayOf(0xFFFFF4C2.toInt(), 0xFFFFB238.toInt(), 0xFFE0602A.toInt()), null, Shader.TileMode.CLAMP)
                cv.drawPath(f, p)
            }
            return b
        }
    }
}
