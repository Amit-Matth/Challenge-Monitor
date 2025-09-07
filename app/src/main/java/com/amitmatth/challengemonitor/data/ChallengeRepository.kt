package com.amitmatth.challengemonitor.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.ChallengeDailyLog
import com.amitmatth.challengemonitor.model.DailyLogDisplayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChallengeRepository(context: Context) {

    private val dbHelper = ChallengeDbHelper(context)
    private val systemLogDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _allChallenges = MutableLiveData<List<Challenge>>()
    val allChallenges: LiveData<List<Challenge>> = _allChallenges

    private val _completedChallenges = MutableLiveData<List<Challenge>>()
    val completedChallenges: LiveData<List<Challenge>> = _completedChallenges

    private val _loggedChallenges = MutableLiveData<List<Challenge>>() 
    val loggedChallenges: LiveData<List<Challenge>> = _loggedChallenges

    private val _notLoggedChallenges = MutableLiveData<List<Challenge>>() 
    val notLoggedChallenges: LiveData<List<Challenge>> = _notLoggedChallenges

    private val _skippedChallengesForDate = MutableLiveData<List<Challenge>>() 
    val skippedChallengesForDate: LiveData<List<Challenge>> = _skippedChallengesForDate

    private val _concludingChallengesForDate = MutableLiveData<List<Challenge>>() 
    val concludingChallengesForDate: LiveData<List<Challenge>> = _concludingChallengesForDate

    private val _streakChallenges = MutableLiveData<List<Challenge>>() 
    val streakChallenges: LiveData<List<Challenge>> = _streakChallenges

    suspend fun fetchAllChallenges() {
        withContext(Dispatchers.IO) {
            val challengesList = dbHelper.getAllChallenges()
            _allChallenges.postValue(challengesList)
        }
    }

    suspend fun insertChallenge(challenge: Challenge): Long {
        return withContext(Dispatchers.IO) {
            val challengeId = dbHelper.insertChallenge(challenge)
            fetchAllChallenges() 
            challengeId
        }
    }

    suspend fun updateChallenge(challenge: Challenge): Int {
        return withContext(Dispatchers.IO) {
            val rowsAffected = dbHelper.updateChallenge(challenge)
            rowsAffected
        }
    }

    suspend fun deleteChallenge(challenge: Challenge) {
        withContext(Dispatchers.IO) {
            dbHelper.deleteChallengeById(challenge.id)
            fetchAllChallenges() 
        }
    }

    suspend fun getChallengeById(challengeId: Long): Challenge? {
        return withContext(Dispatchers.IO) {
            dbHelper.getChallenge(challengeId)
        }
    }

    suspend fun getDailyLogsForChallenge(challengeId: Long): List<ChallengeDailyLog> {
        return withContext(Dispatchers.IO) {
            dbHelper.getDailyLogsForChallenge(challengeId)
        }
    }

    suspend fun addDailyLog(log: ChallengeDailyLog): Long {
        return withContext(Dispatchers.IO) {
            dbHelper.addDailyLog(log)
        }
    }

    suspend fun hasActionableLogForDate(challengeId: Long, date: String): Boolean {
        return withContext(Dispatchers.IO) {
            dbHelper.hasActionableLogForDate(challengeId, date)
        }
    }
    
    suspend fun fetchAllLogsForDate(dateString: String): List<DailyLogDisplayItem> {
        return withContext(Dispatchers.IO) {
            dbHelper.getAllChallengeLogsForDate(dateString)
        }
    }

    suspend fun fetchCompletedChallenges() { 
        withContext(Dispatchers.IO) {
            _completedChallenges.postValue(dbHelper.getCompletedChallenges())
        }
    }

    suspend fun fetchLoggedChallenges(date: String) {
        withContext(Dispatchers.IO) {
            _loggedChallenges.postValue(dbHelper.getLoggedChallengesForDate(date))
        }
    }

    suspend fun fetchNotLoggedChallenges(date: String) {
        withContext(Dispatchers.IO) {
            _notLoggedChallenges.postValue(dbHelper.getUnloggedChallengesForDate(date))
        }
    }

    suspend fun fetchSkippedChallengesForDate(date: String) {
        withContext(Dispatchers.IO) {
            _skippedChallengesForDate.postValue(dbHelper.getChallengesSkippedForDate(date))
        }
    }

    suspend fun fetchConcludingChallengesForDate(date: String) {
        withContext(Dispatchers.IO) {
            _concludingChallengesForDate.postValue(dbHelper.getConcludingChallengesForDate(date))
        }
    }

    suspend fun fetchAllChallengesForStreaks() {
        withContext(Dispatchers.IO) {
            _streakChallenges.postValue(dbHelper.getAllChallengesForStreaks())
        }
    }

    suspend fun updateChallengeProgressAndStatus(challengeId: Long, dateOfLogUpdate: String) {
        withContext(Dispatchers.IO) {
            val challenge = dbHelper.getChallenge(challengeId)
            if (challenge == null) {
                Log.e("ChallengeRepository", "updateChallengeProgressAndStatus: Challenge not found with ID $challengeId")
                return@withContext
            }

            val logs = dbHelper.getDailyLogsForChallenge(challengeId)
            val scorableStatuses = setOf(
                ChallengeDbHelper.STATUS_FOLLOWED,
                ChallengeDbHelper.STATUS_NOT_FOLLOWED,
                ChallengeDbHelper.STATUS_SKIPPED
            )
            val loggedScorableDaysCount = logs.filter { it.status in scorableStatuses }
                .distinctBy { it.logDate } 
                .count()

            var challengeWasModified = false
            if (challenge.daysLogged != loggedScorableDaysCount) {
                challenge.daysLogged = loggedScorableDaysCount
                challengeWasModified = true
            }

            val isNowComplete = challenge.isActive && (loggedScorableDaysCount >= challenge.durationDays)
            var newStatusIsInactive = false

            if (isNowComplete) {
                challenge.isActive = false
                newStatusIsInactive = true
                challengeWasModified = true
                
                val completionLog = ChallengeDailyLog(
                    challengeId = challengeId,
                    logDate = systemLogDateFormat.format(Date()),
                    status = ChallengeDbHelper.STATUS_COMPLETED,
                    notes = "Challenge marked as completed by system."
                )
                dbHelper.addDailyLog(completionLog)
                Log.d("ChallengeRepository", "Challenge ID $challengeId met completion criteria. Marked as inactive.")
            }

            if (challengeWasModified) {
                dbHelper.updateChallenge(challenge)
                Log.d("ChallengeRepository", "Challenge ID $challengeId updated. Days Logged: ${challenge.daysLogged}, Active: ${challenge.isActive}")
            }

            fetchLoggedChallenges(dateOfLogUpdate)
            fetchNotLoggedChallenges(dateOfLogUpdate)
            fetchSkippedChallengesForDate(dateOfLogUpdate)
            fetchConcludingChallengesForDate(dateOfLogUpdate)

            if (newStatusIsInactive) {
                fetchCompletedChallenges()
            }
            fetchAllChallenges()
            fetchAllChallengesForStreaks()
        }
    }

    suspend fun fetchChallengesForDateWithSpecificStatus(date: String, status: String): List<Challenge> {
        return withContext(Dispatchers.IO) {
            dbHelper.getChallengesForDateWithSpecificStatus(date, status)
        }
    }

    suspend fun fetchDistinctDatesWithSpecificLogStatus(status: String, olderThanDate: String? = null): List<String> {
        return withContext(Dispatchers.IO) {
            dbHelper.getDistinctDatesWithSpecificLogStatus(status, olderThanDate)
        }
    }
}