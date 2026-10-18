package app.vigil.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import app.vigil.domain.Category
import app.vigil.domain.EventKind
import app.vigil.domain.Rigor
import app.vigil.domain.Step
import app.vigil.domain.Trigger
import app.vigil.domain.VowState
import app.vigil.domain.VowType
import app.vigil.domain.Wax
import app.vigil.domain.enumOr
import app.vigil.domain.steps
import kotlinx.coroutines.flow.Flow

/**
 * A vow: what is given up or taken on, for how long, and what stands between you and breaking it.
 * Days are counted from [startAt] in whole 24-hour spans, so a vow sealed at dawn ends at dawn.
 */
@Entity(tableName = "vows")
data class Vow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The catalog entry it came from, or empty for one's own. */
    val preset: String = "",
    val title: String,
    /** In your own words: why. */
    val why: String = "",
    /** [Category] name. */
    val category: String,
    /** [VowType] name. */
    val type: String,
    /** Length in days; 0 for a vow with no end ("until I lay it down"). */
    val days: Int,
    /** For [VowType.LIMIT]: no more than [limit] [unit] a day. */
    val limit: Int = 0,
    val unit: String = "",
    /** For [VowType.WINDOW]: not from [windowFrom] to [windowTo], minutes after midnight. */
    val windowFrom: Int = -1,
    val windowTo: Int = -1,
    val createdAt: Long,
    val startAt: Long,
    /** [Rigor] name. */
    val rigor: String,
    /** The ladder of release, [Step] names in order. */
    val ladder: String,
    /** Minutes the candle of waiting burns. */
    val waitMin: Int = 10,
    /** Letters a farewell letter must have. */
    val letterMin: Int = 140,
    /** The act that buys the vow back. */
    val ransom: String = "",
    /** Who is told when the vow is broken, and how to reach them (a phone number, or empty to pick a messenger). */
    val witness: String = "",
    val witnessPhone: String = "",
    /** What is pledged if the vow is broken. */
    val tithe: String = "",
    /** "Sleep on it": the hour, minutes after midnight, from which a delayed breaking is allowed. */
    val delayAt: Int = 9 * 60,
    /** The voice recording of [why], a file name in the app's storage. */
    val voice: String = "",
    /** A letter to yourself, opened only when the vow is fulfilled. */
    val testament: String = "",
    /** [Wax] name. */
    val wax: String,
    /** The illuminated initial. */
    val initial: String,
    /** Penance added to the end. */
    val extraDays: Int = 0,
    /** Indulgences spent (novices only). */
    val indulgences: Int = 0,
    /** Cracks in the seal. */
    val cracks: Int = 0,
    /** [VowState] name. */
    val state: String = VowState.ACTIVE.name,
    val endedAt: Long? = null,
    /** Breaking was put off until tomorrow: allowed from then. */
    val breakAt: Long? = null,
    /** The ending (fulfilled or burned) was shown. */
    val endSeen: Boolean = false,
    val seed: Int,
) {
    val categoryOf: Category get() = enumOr(category, Category.SPIRIT)
    val typeOf: VowType get() = enumOr(type, VowType.ABSTAIN)
    val rigorOf: Rigor get() = enumOr(rigor, Rigor.MONK)
    val stateOf: VowState get() = enumOr(state, VowState.ACTIVE)
    val waxOf: Wax get() = enumOr(wax, Wax.VERMILION)
    val ladderOf: List<Step> get() = steps(ladder)
    val active: Boolean get() = stateOf == VowState.ACTIVE
}

/** Something that happened to a vow; see [EventKind]. [day] is the vow's own day, 1-based. */
@Entity(tableName = "events", indices = [Index("vowId")])
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vowId: Long,
    val at: Long,
    val day: Int,
    /** [EventKind] name. */
    val kind: String,
    val text: String = "",
    /** A tithe that was paid. */
    val settled: Boolean = false,
) {
    val kindOf: EventKind get() = enumOr(kind, EventKind.KEPT)
}

/** A temptation: how strong, what set it off, and whether it was held. */
@Entity(tableName = "urges", indices = [Index("vowId")])
data class Urge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vowId: Long,
    val at: Long,
    /** 1..5 */
    val strength: Int = 3,
    /** [Trigger] name, or empty. */
    val trigger: String = "",
    val held: Boolean = true,
) {
    val triggerOf: Trigger? get() = Trigger.entries.firstOrNull { it.name == trigger }
}

@Dao
abstract class VigilDao {
    @Query("SELECT * FROM vows ORDER BY startAt DESC")
    abstract fun observeVows(): Flow<List<Vow>>

    @Query("SELECT * FROM events ORDER BY at DESC")
    abstract fun observeEvents(): Flow<List<Event>>

    @Query("SELECT * FROM urges ORDER BY at DESC")
    abstract fun observeUrges(): Flow<List<Urge>>

    @Query("SELECT * FROM vows ORDER BY startAt DESC") abstract suspend fun vows(): List<Vow>
    @Query("SELECT * FROM events ORDER BY at DESC") abstract suspend fun events(): List<Event>
    @Query("SELECT * FROM urges ORDER BY at DESC") abstract suspend fun urges(): List<Urge>
    @Query("SELECT * FROM vows WHERE id = :id") abstract suspend fun vow(id: Long): Vow?

    @Insert abstract suspend fun insert(v: Vow): Long
    @Update abstract suspend fun update(v: Vow)
    @Insert abstract suspend fun insert(e: Event): Long
    @Update abstract suspend fun update(e: Event)
    @Insert abstract suspend fun insert(u: Urge): Long

    @Query("DELETE FROM events WHERE vowId = :id") protected abstract suspend fun deleteEvents(id: Long)
    @Query("DELETE FROM urges WHERE vowId = :id") protected abstract suspend fun deleteUrges(id: Long)
    @Delete protected abstract suspend fun deleteVow(v: Vow)

    @Query("DELETE FROM events WHERE vowId = :id AND day = :day AND kind IN (:kinds)")
    abstract suspend fun clearDay(id: Long, day: Int, kinds: List<String>)

    @Transaction
    open suspend fun delete(v: Vow) {
        deleteEvents(v.id)
        deleteUrges(v.id)
        deleteVow(v)
    }

    @Query("DELETE FROM vows") protected abstract suspend fun clearVows()
    @Query("DELETE FROM events") protected abstract suspend fun clearEvents()
    @Query("DELETE FROM urges") protected abstract suspend fun clearUrges()

    @Transaction
    open suspend fun clear() {
        clearEvents()
        clearUrges()
        clearVows()
    }
}

@Database(entities = [Vow::class, Event::class, Urge::class], version = 1, exportSchema = true)
abstract class VigilDatabase : RoomDatabase() {
    abstract fun dao(): VigilDao

    companion object {
        fun create(context: Context): VigilDatabase =
            Room.databaseBuilder(context.applicationContext, VigilDatabase::class.java, "vigil.db").build()
    }
}
