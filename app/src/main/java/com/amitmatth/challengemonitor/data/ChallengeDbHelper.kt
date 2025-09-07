package com.amitmatth.challengemonitor.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.ChallengeDailyLog
import com.amitmatth.challengemonitor.model.DailyLogDisplayItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.database.sqlite.transaction

class ChallengeDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ChallengeMonitor.db"
        private const val TABLE_CHALLENGES = "challenges"
        private const val COLUMN_CHALLENGE_ID = "_id"
        private const val COLUMN_CHALLENGE_DAYSLOGGED = "days_logged"
        private const val COLUMN_CHALLENGE_TITLE = "title"
        private const val COLUMN_CHALLENGE_DESCRIPTION = "description"
        private const val COLUMN_CHALLENGE_START_DATE = "start_date"
        private const val COLUMN_CHALLENGE_END_DATE = "end_date"
        private const val COLUMN_CHALLENGE_DURATION_DAYS = "duration_days"
        private const val COLUMN_CHALLENGE_IS_ACTIVE = "is_active"

        private const val TABLE_DAILY_LOG = "challenge_daily_log"
        private const val COLUMN_LOG_ID = "_id"
        private const val COLUMN_LOG_CHALLENGE_ID_FK = "challenge_id"
        private const val COLUMN_LOG_DATE = "log_date"
        private const val COLUMN_LOG_STATUS = "status"
        private const val COLUMN_LOG_NOTES = "notes"
        private const val COLUMN_LOG_LAST_UPDATED = "last_updated_time"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_FOLLOWED = "FOLLOWED"
        const val STATUS_NOT_FOLLOWED = "NOT_FOLLOWED"
        const val STATUS_SKIPPED = "SKIPPED"
        const val STATUS_CREATED = "CREATED"
        const val STATUS_EDITED = "EDITED"
        const val STATUS_DELETED = "DELETED"
        const val STATUS_COMPLETED = "COMPLETED"
    }

    private fun dateTimeFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    }

    private fun timeOnlyFormat(): SimpleDateFormat {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createChallengesTable = ("CREATE TABLE " + TABLE_CHALLENGES + "("
                + COLUMN_CHALLENGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_CHALLENGE_TITLE + " TEXT NOT NULL," + COLUMN_CHALLENGE_DESCRIPTION + " TEXT," + COLUMN_CHALLENGE_START_DATE + " TEXT NOT NULL," + COLUMN_CHALLENGE_END_DATE + " TEXT NOT NULL," + COLUMN_CHALLENGE_DURATION_DAYS + " INTEGER NOT NULL," + COLUMN_CHALLENGE_DAYSLOGGED + " INTEGER NOT NULL," + COLUMN_CHALLENGE_IS_ACTIVE + " INTEGER DEFAULT 1" + ")")

        val createDailyLogTable = ("CREATE TABLE " + TABLE_DAILY_LOG + "("
                + COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_LOG_CHALLENGE_ID_FK + " INTEGER NOT NULL," + COLUMN_LOG_DATE + " TEXT NOT NULL," + COLUMN_LOG_STATUS + " TEXT NOT NULL," + COLUMN_LOG_NOTES + " TEXT," + COLUMN_LOG_LAST_UPDATED + " TEXT NOT NULL,"
                + "FOREIGN KEY(" + COLUMN_LOG_CHALLENGE_ID_FK + ") REFERENCES " + TABLE_CHALLENGES + "(" + COLUMN_CHALLENGE_ID + ") ON DELETE CASCADE" + ")")

        db.execSQL(createChallengesTable)
        db.execSQL(createDailyLogTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DAILY_LOG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHALLENGES")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        db?.execSQL("PRAGMA foreign_keys=ON;")
    }

    fun insertChallenge(challenge: Challenge): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CHALLENGE_TITLE, challenge.title)
            put(COLUMN_CHALLENGE_DESCRIPTION, challenge.description)
            put(COLUMN_CHALLENGE_START_DATE, challenge.startDate)
            put(COLUMN_CHALLENGE_END_DATE, challenge.endDate)
            put(COLUMN_CHALLENGE_DURATION_DAYS, challenge.durationDays)
            put(COLUMN_CHALLENGE_IS_ACTIVE, if (challenge.isActive) 1 else 0)
            put(COLUMN_CHALLENGE_DAYSLOGGED, challenge.daysLogged)
        }
        val id = db.insert(TABLE_CHALLENGES, null, values)
        return id
    }

    fun updateChallenge(challenge: Challenge): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CHALLENGE_TITLE, challenge.title)
            put(COLUMN_CHALLENGE_DESCRIPTION, challenge.description)
            put(COLUMN_CHALLENGE_START_DATE, challenge.startDate)
            put(COLUMN_CHALLENGE_END_DATE, challenge.endDate)
            put(COLUMN_CHALLENGE_DURATION_DAYS, challenge.durationDays)
            put(COLUMN_CHALLENGE_IS_ACTIVE, if (challenge.isActive) 1 else 0)
            put(COLUMN_CHALLENGE_DAYSLOGGED, challenge.daysLogged)
        }
        return db.update(
            TABLE_CHALLENGES,
            values,
            "$COLUMN_CHALLENGE_ID = ?",
            arrayOf(challenge.id.toString())
        )
    }

    fun getChallenge(id: Long): Challenge? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_CHALLENGES,
            null,
            "$COLUMN_CHALLENGE_ID=?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        var challenge: Challenge? = null
        cursor.use {
            if (it.moveToFirst()) {
                challenge = cursorToChallenge(it)
            }
        }
        return challenge
    }

    fun getAllChallenges(): List<Challenge> {
        val challenges = mutableListOf<Challenge>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CHALLENGES ORDER BY $COLUMN_CHALLENGE_IS_ACTIVE DESC, $COLUMN_CHALLENGE_START_DATE DESC",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challenges.add(cursorToChallenge(it))
                } while (it.moveToNext())
            }
        }
        return challenges
    }

    fun deleteChallengeById(challengeId: Long): Int {
        val db = this.writableDatabase
        return db.delete(
            TABLE_CHALLENGES,
            "$COLUMN_CHALLENGE_ID = ?",
            arrayOf(challengeId.toString())
        )
    }

    fun addDailyLog(log: ChallengeDailyLog): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LOG_CHALLENGE_ID_FK, log.challengeId)
            put(COLUMN_LOG_DATE, log.logDate)
            put(COLUMN_LOG_STATUS, log.status)
            put(COLUMN_LOG_NOTES, log.notes)
            put(COLUMN_LOG_LAST_UPDATED, dateTimeFormat().format(Date()))
        }
        return db.insert(TABLE_DAILY_LOG, null, values)
    }

    fun getDailyLogsForChallenge(challengeId: Long): List<ChallengeDailyLog> {
        val logs = mutableListOf<ChallengeDailyLog>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_DAILY_LOG,
            null,
            "$COLUMN_LOG_CHALLENGE_ID_FK = ?",
            arrayOf(challengeId.toString()),
            null,
            null,
            "$COLUMN_LOG_DATE ASC, $COLUMN_LOG_LAST_UPDATED ASC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    logs.add(cursorToDailyLog(it))
                } while (it.moveToNext())
            }
        }
        return logs
    }

    fun hasActionableLogForDate(challengeId: Long, date: String): Boolean {
        val db = this.readableDatabase
        val actionableStatuses = arrayOf(
            STATUS_FOLLOWED,
            STATUS_NOT_FOLLOWED,
            STATUS_SKIPPED,
            STATUS_CREATED,
            STATUS_EDITED
        )
        val selection =
            "$COLUMN_LOG_CHALLENGE_ID_FK = ? AND $COLUMN_LOG_DATE = ? AND $COLUMN_LOG_STATUS IN (${
                actionableStatuses.joinToString(
                    separator = ",",
                    transform = { "'${'$'}{it}'" })
            })"
        val cursor = db.query(
            TABLE_DAILY_LOG,
            arrayOf("COUNT(*)"),
            selection,
            arrayOf(challengeId.toString(), date),
            null,
            null,
            null
        )
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        return count > 0
    }

    fun getLoggedChallengesForDate(date: String): List<Challenge> {
        val challenges = mutableListOf<Challenge>()
        val db = this.readableDatabase

        val query = """
        SELECT DISTINCT c.*
        FROM $TABLE_CHALLENGES c
        JOIN $TABLE_DAILY_LOG dl 
          ON c.$COLUMN_CHALLENGE_ID = dl.$COLUMN_LOG_CHALLENGE_ID_FK
        WHERE c.$COLUMN_CHALLENGE_IS_ACTIVE = 1
          AND c.$COLUMN_CHALLENGE_START_DATE <= ?
          AND c.$COLUMN_CHALLENGE_END_DATE >= ?
          AND dl.$COLUMN_LOG_DATE = ?
          AND dl.$COLUMN_LOG_STATUS IN (?, ?, ?) 
          AND dl.$COLUMN_LOG_LAST_UPDATED = (
              SELECT MAX(dlSub.$COLUMN_LOG_LAST_UPDATED)
              FROM $TABLE_DAILY_LOG dlSub
              WHERE dlSub.$COLUMN_LOG_CHALLENGE_ID_FK = c.$COLUMN_CHALLENGE_ID 
                AND dlSub.$COLUMN_LOG_DATE = ?
          )
        ORDER BY c.$COLUMN_CHALLENGE_START_DATE DESC
    """

        val cursor = db.rawQuery(
            query,
            arrayOf(
                date, date, date,
                STATUS_FOLLOWED, STATUS_NOT_FOLLOWED, STATUS_SKIPPED,
                date
            )
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challenges.add(cursorToChallenge(it))
                } while (it.moveToNext())
            }
        }
        return challenges
    }


    fun getUnloggedChallengesForDate(date: String): List<Challenge> {
        val challenges = mutableListOf<Challenge>()
        val db = this.readableDatabase

        val query = """
        SELECT c.*
        FROM $TABLE_CHALLENGES c
        WHERE c.$COLUMN_CHALLENGE_IS_ACTIVE = 1
          AND c.$COLUMN_CHALLENGE_START_DATE <= ?
          AND c.$COLUMN_CHALLENGE_END_DATE >= ?
          AND NOT EXISTS (
              SELECT 1
              FROM $TABLE_DAILY_LOG dl
              WHERE dl.$COLUMN_LOG_CHALLENGE_ID_FK = c.$COLUMN_CHALLENGE_ID
                AND dl.$COLUMN_LOG_DATE = ?
                AND dl.$COLUMN_LOG_STATUS IN (?, ?, ?)
          )
        ORDER BY c.$COLUMN_CHALLENGE_START_DATE DESC
    """

        val cursor = db.rawQuery(
            query,
            arrayOf(
                date, date, date,
                STATUS_FOLLOWED, STATUS_NOT_FOLLOWED, STATUS_SKIPPED
            )
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challenges.add(cursorToChallenge(it))
                } while (it.moveToNext())
            }
        }
        return challenges
    }


    fun getChallengesSkippedForDate(date: String): List<Challenge> {
        val challenges = mutableListOf<Challenge>()
        val db = this.readableDatabase
        val query = """
        SELECT DISTINCT c.*
        FROM $TABLE_CHALLENGES c
        JOIN $TABLE_DAILY_LOG dl ON c.$COLUMN_CHALLENGE_ID = dl.$COLUMN_LOG_CHALLENGE_ID_FK
        WHERE dl.$COLUMN_LOG_DATE = ?
          AND dl.$COLUMN_LOG_STATUS = ?
          AND dl.$COLUMN_LOG_LAST_UPDATED = (
              SELECT MAX(dlSub.$COLUMN_LOG_LAST_UPDATED)
              FROM $TABLE_DAILY_LOG dlSub
              WHERE dlSub.$COLUMN_LOG_CHALLENGE_ID_FK = c.$COLUMN_CHALLENGE_ID
                AND dlSub.$COLUMN_LOG_DATE = ?
          )
        ORDER BY c.$COLUMN_CHALLENGE_START_DATE DESC
    """
        val cursor = db.rawQuery(query, arrayOf(date, STATUS_SKIPPED, date))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challenges.add(cursorToChallenge(it))
                } while (it.moveToNext())
            }
        }
        return challenges
    }

    fun getConcludingChallengesForDate(date: String): List<Challenge> {
        val challenges = mutableListOf<Challenge>()
        val db = this.readableDatabase
        val query = """
            SELECT c.*
            FROM $TABLE_CHALLENGES c
            WHERE c.$COLUMN_CHALLENGE_START_DATE <= ?
              AND c.$COLUMN_CHALLENGE_END_DATE = ?
            ORDER BY c.$COLUMN_CHALLENGE_TITLE ASC
        """
        val cursor = db.rawQuery(query, arrayOf(date, date))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challenges.add(cursorToChallenge(it))
                } while (it.moveToNext())
            }
        }
        return challenges
    }

    fun getCompletedChallenges(): List<Challenge> {
        val challenges = mutableListOf<Challenge>()
        val db = this.readableDatabase
        val query = """
            SELECT c.* FROM $TABLE_CHALLENGES c
            WHERE c.$COLUMN_CHALLENGE_IS_ACTIVE = 0
            ORDER BY c.$COLUMN_CHALLENGE_END_DATE DESC 
        """
        val cursor = db.rawQuery(query, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challenges.add(cursorToChallenge(it))
                } while (it.moveToNext())
            }
        }
        return challenges
    }

    fun getAllChallengeLogsForDate(dateString: String): List<DailyLogDisplayItem> {
        val logsList = mutableListOf<DailyLogDisplayItem>()
        val db = this.readableDatabase
        val query = """
            SELECT
                dl.$COLUMN_LOG_ID,
                dl.$COLUMN_LOG_CHALLENGE_ID_FK,
                c.$COLUMN_CHALLENGE_TITLE,
                dl.$COLUMN_LOG_STATUS,
                dl.$COLUMN_LOG_NOTES,
                dl.$COLUMN_LOG_LAST_UPDATED
            FROM $TABLE_DAILY_LOG dl
            INNER JOIN $TABLE_CHALLENGES c ON dl.$COLUMN_LOG_CHALLENGE_ID_FK = c.$COLUMN_CHALLENGE_ID
            WHERE dl.$COLUMN_LOG_DATE = ?
            ORDER BY dl.$COLUMN_LOG_LAST_UPDATED ASC 
        """
        val cursor = db.rawQuery(query, arrayOf(dateString))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val logFullDate = try {
                        dateTimeFormat().parse(
                            it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_LOG_LAST_UPDATED
                                )
                            )
                        )
                    } catch (_: Exception) {
                        Date(0)
                    }
                    val logTime = timeOnlyFormat().format(logFullDate)

                    logsList.add(
                        DailyLogDisplayItem(
                            logId = it.getLong(it.getColumnIndexOrThrow(COLUMN_LOG_ID)),
                            challengeId = it.getLong(
                                it.getColumnIndexOrThrow(
                                    COLUMN_LOG_CHALLENGE_ID_FK
                                )
                            ),
                            challengeTitle = it.getString(
                                it.getColumnIndexOrThrow(
                                    COLUMN_CHALLENGE_TITLE
                                )
                            ),
                            status = it.getString(it.getColumnIndexOrThrow(COLUMN_LOG_STATUS)),
                            notes = it.getString(it.getColumnIndexOrThrow(COLUMN_LOG_NOTES)),
                            logTime = logTime,
                            logFullDateTime = logFullDate
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return logsList
    }

    fun getAllChallengesForStreaks(): List<Challenge> {
        val challenges = mutableListOf<Challenge>()
        val db = this.readableDatabase
        val query = """
            SELECT * FROM $TABLE_CHALLENGES
            ORDER BY $COLUMN_CHALLENGE_IS_ACTIVE DESC, $COLUMN_CHALLENGE_START_DATE DESC
        """
        val cursor = db.rawQuery(query, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challenges.add(cursorToChallenge(it))
                } while (it.moveToNext())
            }
        }
        return challenges
    }

    fun markUnloggedChallengesAsSkipped(currentDate: String): List<Long> {
        val db = this.writableDatabase
        val unloggedChallengesQuery = """
            SELECT c.$COLUMN_CHALLENGE_ID
            FROM $TABLE_CHALLENGES c
            WHERE c.$COLUMN_CHALLENGE_IS_ACTIVE = 1
              AND c.$COLUMN_CHALLENGE_START_DATE <= ?
              AND c.$COLUMN_CHALLENGE_END_DATE >= ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM $TABLE_DAILY_LOG dl
                  WHERE dl.$COLUMN_LOG_CHALLENGE_ID_FK = c.$COLUMN_CHALLENGE_ID
                    AND dl.$COLUMN_LOG_DATE = ?
                    AND dl.$COLUMN_LOG_STATUS IN (?, ?, ?, ?, ?) 
              )
        """

        val cursor = db.rawQuery(
            unloggedChallengesQuery,
            arrayOf(
                currentDate,
                currentDate,
                currentDate,
                STATUS_FOLLOWED,
                STATUS_NOT_FOLLOWED,
                STATUS_SKIPPED,
                STATUS_CREATED,
                STATUS_EDITED
            )
        )
        val challengeIdsToSkip = mutableListOf<Long>()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challengeIdsToSkip.add(it.getLong(it.getColumnIndexOrThrow(COLUMN_CHALLENGE_ID)))
                } while (it.moveToNext())
            }
        }

        if (challengeIdsToSkip.isNotEmpty()) {
            db.transaction {
                try {
                    for (challengeId in challengeIdsToSkip) {
                        val values = ContentValues().apply {
                            put(COLUMN_LOG_CHALLENGE_ID_FK, challengeId)
                            put(COLUMN_LOG_DATE, currentDate)
                            put(COLUMN_LOG_STATUS, STATUS_SKIPPED)
                            put(COLUMN_LOG_NOTES, "Automatically skipped")
                            put(COLUMN_LOG_LAST_UPDATED, dateTimeFormat().format(Date()))
                        }
                        insert(TABLE_DAILY_LOG, null, values)
                    }
                } finally {
                }
            }
        }
        return challengeIdsToSkip
    }

    fun getChallengesForDateWithSpecificStatus(date: String, status: String): List<Challenge> {
        val challenges = mutableListOf<Challenge>()
        val db = this.readableDatabase
        val query = """
            SELECT DISTINCT c.*
            FROM $TABLE_CHALLENGES c
            JOIN $TABLE_DAILY_LOG dl ON c.$COLUMN_CHALLENGE_ID = dl.$COLUMN_LOG_CHALLENGE_ID_FK
            WHERE dl.$COLUMN_LOG_DATE = ? 
              AND dl.$COLUMN_LOG_STATUS = ?
              AND dl.$COLUMN_LOG_LAST_UPDATED = (
                  SELECT MAX(dlSub.$COLUMN_LOG_LAST_UPDATED)
                  FROM $TABLE_DAILY_LOG dlSub
                  WHERE dlSub.$COLUMN_LOG_CHALLENGE_ID_FK = c.$COLUMN_CHALLENGE_ID 
                    AND dlSub.$COLUMN_LOG_DATE = ? 
              )
            ORDER BY c.$COLUMN_CHALLENGE_TITLE ASC 
        """
        val cursor = db.rawQuery(query, arrayOf(date, status, date))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    challenges.add(cursorToChallenge(it))
                } while (it.moveToNext())
            }
        }
        return challenges
    }

    fun getDistinctDatesWithSpecificLogStatus(
        status: String,
        olderThanDate: String? = null
    ): List<String> {
        val dates = mutableListOf<String>()
        val db = this.readableDatabase
        val selectionArgs = mutableListOf(status)

        var dateCondition = ""
        if (olderThanDate != null) {
            dateCondition = "AND dl.$COLUMN_LOG_DATE < ? "
            selectionArgs.add(olderThanDate)
        }

        val query = """
            SELECT DISTINCT dl.$COLUMN_LOG_DATE
            FROM $TABLE_DAILY_LOG dl
            WHERE dl.$COLUMN_LOG_STATUS = ? 
              $dateCondition
              AND dl.$COLUMN_LOG_LAST_UPDATED = (
                  SELECT MAX(dlSub.$COLUMN_LOG_LAST_UPDATED)
                  FROM $TABLE_DAILY_LOG dlSub
                  WHERE dlSub.$COLUMN_LOG_CHALLENGE_ID_FK = dl.$COLUMN_LOG_CHALLENGE_ID_FK 
                    AND dlSub.$COLUMN_LOG_DATE = dl.$COLUMN_LOG_DATE
              )
            ORDER BY dl.$COLUMN_LOG_DATE DESC
        """

        val cursor = db.rawQuery(query, selectionArgs.toTypedArray())
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    dates.add(it.getString(it.getColumnIndexOrThrow(COLUMN_LOG_DATE)))
                } while (it.moveToNext())
            }
        }
        return dates
    }

    private fun cursorToChallenge(cursor: Cursor): Challenge {
        return Challenge(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHALLENGE_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHALLENGE_TITLE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHALLENGE_DESCRIPTION)),
            startDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHALLENGE_START_DATE)),
            endDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHALLENGE_END_DATE)),
            durationDays = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHALLENGE_DURATION_DAYS)),
            isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHALLENGE_IS_ACTIVE)) == 1,
            daysLogged = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHALLENGE_DAYSLOGGED))
        )
    }

    private fun cursorToDailyLog(cursor: Cursor): ChallengeDailyLog {
        val notesIndex = cursor.getColumnIndex(COLUMN_LOG_NOTES)
        val notes =
            if (notesIndex != -1 && !cursor.isNull(notesIndex)) cursor.getString(notesIndex) else null

        return ChallengeDailyLog(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID)),
            challengeId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOG_CHALLENGE_ID_FK)),
            logDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATE)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_STATUS)),
            notes = notes,
            lastUpdatedTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_LAST_UPDATED))
        )
    }
}