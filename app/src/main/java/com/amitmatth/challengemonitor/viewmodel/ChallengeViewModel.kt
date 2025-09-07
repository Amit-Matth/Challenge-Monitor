package com.amitmatth.challengemonitor.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amitmatth.challengemonitor.data.ChallengeDbHelper
import com.amitmatth.challengemonitor.data.ChallengeRepository
import com.amitmatth.challengemonitor.model.Challenge
import com.amitmatth.challengemonitor.model.ChallengeDailyLog
import com.amitmatth.challengemonitor.model.DailyLogDisplayItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChallengeRepository = ChallengeRepository(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    val allChallenges: LiveData<List<Challenge>> = repository.allChallenges


    private val _currentChallenge = MutableLiveData<Challenge?>()
    val currentChallenge: LiveData<Challenge?> = _currentChallenge

    private val _currentChallengeDailyLogs = MutableLiveData<List<ChallengeDailyLog>>()
    val currentChallengeDailyLogs: LiveData<List<ChallengeDailyLog>> = _currentChallengeDailyLogs

    private val _dailyLogsForSelectedDate = MutableLiveData<List<DailyLogDisplayItem>>()
    val dailyLogsForSelectedDate: LiveData<List<DailyLogDisplayItem>> = _dailyLogsForSelectedDate

    val completedChallenges: LiveData<List<Challenge>> = repository.completedChallenges
    val loggedChallenges: LiveData<List<Challenge>> = repository.loggedChallenges
    val notLoggedChallenges: LiveData<List<Challenge>> = repository.notLoggedChallenges
    val concludingChallengesForDate: LiveData<List<Challenge>> =
        repository.concludingChallengesForDate
    val streakChallenges: LiveData<List<Challenge>> = repository.streakChallenges

    val skippedChallengesForDate: LiveData<List<Challenge>> = repository.skippedChallengesForDate

    private val _todayFollowedChallenges = MutableLiveData<List<Challenge>>()
    val todayFollowedChallenges: LiveData<List<Challenge>> = _todayFollowedChallenges

    private val _historicalFollowedChallengesByDate =
        MutableLiveData<Map<String, List<Challenge>>>()
    val historicalFollowedChallengesByDate: LiveData<Map<String, List<Challenge>>> =
        _historicalFollowedChallengesByDate

    private val _todayUnFollowedChallenges = MutableLiveData<List<Challenge>>()
    val todayUnFollowedChallenges: LiveData<List<Challenge>> = _todayUnFollowedChallenges

    private val _historicalUnFollowedChallengesByDate =
        MutableLiveData<Map<String, List<Challenge>>>()
    val historicalUnFollowedChallengesByDate: LiveData<Map<String, List<Challenge>>> =
        _historicalUnFollowedChallengesByDate

    fun refreshChallenges() {
        viewModelScope.launch {
            repository.fetchAllChallenges()
        }
    }

    suspend fun insertChallenge(challenge: Challenge): Long {
        return withContext(Dispatchers.IO) {
            val newChallengeId = repository.insertChallenge(challenge)
            val currentDateStr = dateFormat.format(Date())

            val creationLog = ChallengeDailyLog(
                challengeId = newChallengeId,
                logDate = currentDateStr,
                status = ChallengeDbHelper.STATUS_CREATED,
                notes = "Challenge Created"
            )
            repository.addDailyLog(creationLog)

            try {
                val startDateCal = Calendar.getInstance()
                val parsedStartDate = dateFormat.parse(challenge.startDate)

                if (parsedStartDate != null) {
                    startDateCal.time = parsedStartDate
                    for (i in 0 until challenge.durationDays) {
                        val logDateCal = startDateCal.clone() as Calendar
                        logDateCal.add(Calendar.DAY_OF_YEAR, i)
                        val dateStr = dateFormat.format(logDateCal.time)
                        val pendingLog = ChallengeDailyLog(
                            challengeId = newChallengeId,
                            logDate = dateStr,
                            status = ChallengeDbHelper.STATUS_PENDING,
                            notes = null
                        )
                        repository.addDailyLog(pendingLog)
                    }
                    Log.d(
                        "ChallengeViewModel",
                        "Successfully pre-populated ${challenge.durationDays} pending logs for challenge ID $newChallengeId."
                    )
                } else {
                    Log.e(
                        "ChallengeViewModel",
                        "Could not parse start date '${challenge.startDate}' for challenge ID $newChallengeId. Logs not pre-populated."
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "ChallengeViewModel",
                    "Error pre-populating daily logs for new challenge ID $newChallengeId: ${e.message}",
                    e
                )
            }

            repository.updateChallengeProgressAndStatus(newChallengeId, currentDateStr)
            fetchFollowedData(currentDateStr)
            fetchUnFollowedData(currentDateStr)

            newChallengeId
        }
    }

    suspend fun updateChallenge(challenge: Challenge): Int {
        return withContext(Dispatchers.IO) {
            val rowsAffected = repository.updateChallenge(challenge)
            if (rowsAffected > 0) {
                val currentDateStr = dateFormat.format(Date())
                val editLog = ChallengeDailyLog(
                    challengeId = challenge.id,
                    logDate = currentDateStr,
                    status = ChallengeDbHelper.STATUS_EDITED,
                    notes = "Challenge Edited: ${challenge.title}"
                )
                repository.addDailyLog(editLog)

                repository.updateChallengeProgressAndStatus(challenge.id, currentDateStr)

                if (_currentChallenge.value?.id == challenge.id) {
                    val updatedChallenge = repository.getChallengeById(challenge.id)
                    _currentChallenge.postValue(updatedChallenge)
                    fetchDailyLogsForChallenge(challenge.id)
                }

                fetchFollowedData(currentDateStr)
                fetchUnFollowedData(currentDateStr)
            }
            rowsAffected
        }
    }

    fun deleteChallenge(challenge: Challenge) {
        viewModelScope.launch {
            val currentDateStr = dateFormat.format(Date())

            val deleteLog = ChallengeDailyLog(
                challengeId = challenge.id,
                logDate = currentDateStr,
                status = ChallengeDbHelper.STATUS_DELETED,
                notes = "Challenge Deleted: ${challenge.title}"
            )
            repository.addDailyLog(deleteLog)

            repository.deleteChallenge(challenge)

            if (_currentChallenge.value?.id == challenge.id) {
                _currentChallenge.postValue(null)
                _currentChallengeDailyLogs.postValue(emptyList())
            }

            refreshFilteredListsOnLogChange(currentDateStr)
            refreshStreakChallenges()
        }
    }

    fun fetchChallengeById(challengeId: Long) {
        Log.d(
            "ChallengeViewModel",
            "fetchChallengeById: Clearing old data before fetching ID: $challengeId"
        )
        _currentChallenge.postValue(null)
        _currentChallengeDailyLogs.postValue(emptyList())

        viewModelScope.launch {
            Log.d(
                "ChallengeViewModel",
                "fetchChallengeById: Fetching new data for ID: $challengeId"
            )
            val challenge = repository.getChallengeById(challengeId)
            Log.d(
                "ChallengeViewModel",
                "fetchChallengeById: Fetched challenge: ${challenge?.title} (ID: ${challenge?.id}) for requested ID: $challengeId"
            )
            _currentChallenge.postValue(challenge)

            if (challenge != null) {
                fetchDailyLogsForChallenge(challengeId)
            }
        }
    }

    fun fetchDailyLogsForChallenge(challengeId: Long) {
        viewModelScope.launch {
            val logs = repository.getDailyLogsForChallenge(challengeId)
            _currentChallengeDailyLogs.postValue(logs)
        }
    }

    fun fetchLogsForDate(dateString: String) {
        viewModelScope.launch {
            try {
                val logs = repository.fetchAllLogsForDate(dateString)
                _dailyLogsForSelectedDate.postValue(logs)
            } catch (e: Exception) {
                Log.e(
                    "ChallengeViewModel",
                    "Error fetching logs for date $dateString: ${e.message}",
                    e
                )
                _dailyLogsForSelectedDate.postValue(emptyList())
            }
        }
    }

    suspend fun hasActionableLogForDate(challengeId: Long, date: String): Boolean {
        return repository.hasActionableLogForDate(challengeId, date)
    }

    fun markChallengeDayStatus(
        challengeId: Long,
        dateStr: String,
        status: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val log = ChallengeDailyLog(
                challengeId = challengeId,
                logDate = dateStr,
                status = status,
                notes = notes
            )
            repository.addDailyLog(log)


            repository.updateChallengeProgressAndStatus(challengeId, dateStr)


            if (_currentChallenge.value?.id == challengeId) {
                fetchDailyLogsForChallenge(challengeId)
                val updatedChallenge = repository.getChallengeById(challengeId)
                _currentChallenge.postValue(updatedChallenge)
            }

            fetchFollowedData(dateStr)
            fetchUnFollowedData(dateStr)

            refreshStreakChallenges()
        }
    }

    private fun refreshFilteredListsOnLogChange(date: String) {
        viewModelScope.launch {
            repository.fetchLoggedChallenges(date)
            repository.fetchNotLoggedChallenges(date)
            repository.fetchSkippedChallengesForDate(date)
            repository.fetchConcludingChallengesForDate(date)
            fetchFollowedData(date)
            fetchUnFollowedData(date)
            repository.fetchAllChallengesForStreaks()
        }
    }

    fun refreshCompletedChallenges() {
        viewModelScope.launch {
            repository.fetchCompletedChallenges()
        }
    }

    fun refreshLoggedChallenges(date: String) {
        viewModelScope.launch {
            repository.fetchLoggedChallenges(date)
        }
    }

    fun refreshNotLoggedChallenges(date: String) {
        viewModelScope.launch {
            repository.fetchNotLoggedChallenges(date)
        }
    }

    fun refreshSkippedChallengesForDate(date: String) {
        viewModelScope.launch {
            repository.fetchSkippedChallengesForDate(date)
        }
    }

    fun refreshConcludingChallengesForDate(date: String) {
        viewModelScope.launch {
            repository.fetchConcludingChallengesForDate(date)
        }
    }

    fun refreshStreakChallenges() {
        viewModelScope.launch {
            repository.fetchAllChallengesForStreaks()
        }
    }

    fun fetchFollowedData(currentDate: String) {
        viewModelScope.launch {
            val todayFollowed = repository.fetchChallengesForDateWithSpecificStatus(
                currentDate,
                ChallengeDbHelper.STATUS_FOLLOWED
            )
            _todayFollowedChallenges.postValue(todayFollowed)

            val historicalDates = repository.fetchDistinctDatesWithSpecificLogStatus(
                ChallengeDbHelper.STATUS_FOLLOWED,
                olderThanDate = currentDate
            )
            val historicalMap = mutableMapOf<String, List<Challenge>>()
            for (date in historicalDates) {
                val challengesForDate = repository.fetchChallengesForDateWithSpecificStatus(
                    date,
                    ChallengeDbHelper.STATUS_FOLLOWED
                )
                if (challengesForDate.isNotEmpty()) {
                    val parsedDate = dateFormat.parse(date)
                    val displayableDate = parsedDate?.let { displayDateFormat.format(it) } ?: date
                    historicalMap[displayableDate] = challengesForDate
                }
            }
            _historicalFollowedChallengesByDate.postValue(historicalMap)
        }
    }

    fun fetchUnFollowedData(currentDate: String) {
        viewModelScope.launch {
            val todayUnFollowed = repository.fetchChallengesForDateWithSpecificStatus(
                currentDate,
                ChallengeDbHelper.STATUS_NOT_FOLLOWED
            )
            _todayUnFollowedChallenges.postValue(todayUnFollowed)

            val historicalDates = repository.fetchDistinctDatesWithSpecificLogStatus(
                ChallengeDbHelper.STATUS_NOT_FOLLOWED,
                olderThanDate = currentDate
            )
            val historicalMap = mutableMapOf<String, List<Challenge>>()
            for (date in historicalDates) {
                val challengesForDate = repository.fetchChallengesForDateWithSpecificStatus(
                    date,
                    ChallengeDbHelper.STATUS_NOT_FOLLOWED
                )
                if (challengesForDate.isNotEmpty()) {
                    val parsedDate = dateFormat.parse(date)
                    val displayableDate = parsedDate?.let { displayDateFormat.format(it) } ?: date
                    historicalMap[displayableDate] = challengesForDate
                }
            }
            _historicalUnFollowedChallengesByDate.postValue(historicalMap)
        }
    }

    suspend fun getLogsForChallengeId(challengeId: Long): List<ChallengeDailyLog> {
        return repository.getDailyLogsForChallenge(challengeId)
    }

    suspend fun getLatestStatusForDate(challengeId: Long, dateStr: String): String? {
        val logs = repository.getDailyLogsForChallenge(challengeId)
            .filter { it.logDate == dateStr }
            .sortedByDescending { it.lastUpdatedTime }
        return logs.firstOrNull()?.status
    }
}