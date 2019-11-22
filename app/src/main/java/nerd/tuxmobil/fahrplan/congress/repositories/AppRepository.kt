package nerd.tuxmobil.fahrplan.congress.repositories

import android.content.Context
import android.text.format.Time
import info.metadude.android.eventfahrplan.database.extensions.toContentValues
import info.metadude.android.eventfahrplan.database.repositories.AlarmsDatabaseRepository
import info.metadude.android.eventfahrplan.database.repositories.HighlightsDatabaseRepository
import info.metadude.android.eventfahrplan.database.repositories.LecturesDatabaseRepository
import info.metadude.android.eventfahrplan.database.repositories.MetaDatabaseRepository
import info.metadude.android.eventfahrplan.database.sqliteopenhelper.AlarmsDBOpenHelper
import info.metadude.android.eventfahrplan.database.sqliteopenhelper.HighlightDBOpenHelper
import info.metadude.android.eventfahrplan.database.sqliteopenhelper.LecturesDBOpenHelper
import info.metadude.android.eventfahrplan.database.sqliteopenhelper.MetaDBOpenHelper
import info.metadude.android.eventfahrplan.network.repositories.ScheduleNetworkRepository
import info.metadude.android.eventfahrplan.sessionize.SessionizeNetworkRepository
import info.metadude.android.eventfahrplan.sessionize.SessionizeResult
import info.metadude.kotlin.library.sessionize.ApiModule
import info.metadude.kotlin.library.sessionize.gridtable.models.ConferenceDay
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import nerd.tuxmobil.fahrplan.congress.BuildConfig
import nerd.tuxmobil.fahrplan.congress.dataconverters.*
import nerd.tuxmobil.fahrplan.congress.exceptions.ExceptionHandling
import nerd.tuxmobil.fahrplan.congress.logging.Logging
import nerd.tuxmobil.fahrplan.congress.models.Alarm
import nerd.tuxmobil.fahrplan.congress.models.Lecture
import nerd.tuxmobil.fahrplan.congress.models.Meta
import nerd.tuxmobil.fahrplan.congress.net.FetchScheduleResult
import nerd.tuxmobil.fahrplan.congress.net.HttpStatus
import nerd.tuxmobil.fahrplan.congress.net.ParseScheduleResult
import nerd.tuxmobil.fahrplan.congress.preferences.SharedPreferencesRepository
import nerd.tuxmobil.fahrplan.congress.serialization.ScheduleChanges
import okhttp3.OkHttpClient

object AppRepository {

    const val ALL_DAYS = -1

    private lateinit var context: Context

    private lateinit var logging: Logging

    private val parentJob = SupervisorJob()
    private val parentJobs = mutableMapOf<String, Job>()
    private lateinit var executionContext: ExecutionContext
    private lateinit var exceptionHandling: ExceptionHandling
    private lateinit var networkScope: NetworkScope

    private lateinit var alarmsDBOpenHelper: AlarmsDBOpenHelper
    private lateinit var alarmsDatabaseRepository: AlarmsDatabaseRepository

    private lateinit var highlightDBOpenHelper: HighlightDBOpenHelper
    private lateinit var highlightsDatabaseRepository: HighlightsDatabaseRepository

    private lateinit var lecturesDBOpenHelper: LecturesDBOpenHelper
    private lateinit var lecturesDatabaseRepository: LecturesDatabaseRepository

    private lateinit var metaDBOpenHelper: MetaDBOpenHelper
    private lateinit var metaDatabaseRepository: MetaDatabaseRepository

    private lateinit var scheduleNetworkRepository: ScheduleNetworkRepository
    private lateinit var sharedPreferencesRepository: SharedPreferencesRepository
    private var sessionizeNetworkRepository: SessionizeNetworkRepository? = null

    fun initialize(
            context: Context,
            logging: Logging,
            executionContext: ExecutionContext,
            exceptionHandling: ExceptionHandling
    ) {
        this.context = context
        this.logging = logging
        this.executionContext = executionContext
        this.exceptionHandling = exceptionHandling
        initializeScopes()
        initializeDatabases()
        initializeRepositories()
    }

    private fun initializeScopes() {
        val defaultExceptionHandler = CoroutineExceptionHandler(exceptionHandling::onExceptionHandling)
        networkScope = NetworkScope(executionContext, parentJob, defaultExceptionHandler)
    }

    private fun initializeDatabases() {
        alarmsDBOpenHelper = AlarmsDBOpenHelper(context)
        alarmsDatabaseRepository = AlarmsDatabaseRepository(alarmsDBOpenHelper)
        highlightDBOpenHelper = HighlightDBOpenHelper(context)
        highlightsDatabaseRepository = HighlightsDatabaseRepository(highlightDBOpenHelper)
        lecturesDBOpenHelper = LecturesDBOpenHelper(context)
        lecturesDatabaseRepository = LecturesDatabaseRepository(lecturesDBOpenHelper)
        metaDBOpenHelper = MetaDBOpenHelper(context)
        metaDatabaseRepository = MetaDatabaseRepository(metaDBOpenHelper)
    }

    private fun initializeRepositories() {
        scheduleNetworkRepository = ScheduleNetworkRepository()
        sharedPreferencesRepository = SharedPreferencesRepository(context)
    }

    private fun loadingFailed(@Suppress("SameParameterValue") requestIdentifier: String) {
        parentJobs.remove(requestIdentifier)
    }

    fun cancelLoading() {
        parentJobs.values.forEach(Job::cancel)
        parentJobs.clear()
    }

    fun loadSchedule(hostName: String,
                     okHttpClient: OkHttpClient,
                     onFetchingDone: (fetchScheduleResult: FetchScheduleResult) -> Unit,
                     onParsingDone: (parseScheduleResult: ParseScheduleResult) -> Unit) {

        if (sessionizeNetworkRepository == null) {
            val sessionizeService = ApiModule.provideSessionizeService(hostName, okHttpClient)
            sessionizeNetworkRepository = SessionizeNetworkRepository(sessionizeService, BuildConfig.SESSIONIZE_API_KEY)
        }
        val requestIdentifier = "loadSchedule"
        if (sessionizeNetworkRepository == null) {
            onFetchingDone(FetchScheduleResult.createError(HttpStatus.HTTP_COULD_NOT_CONNECT, hostName))
            loadingFailed(requestIdentifier)
            return
        }

        parentJobs[requestIdentifier] = networkScope.launchNamed(requestIdentifier) {

            suspend fun notifyFetchingDone(fetchScheduleResult: FetchScheduleResult) {
                executionContext.withUiContext {
                    onFetchingDone(fetchScheduleResult)
                }
            }

            suspend fun notifyParsingDone(parseScheduleResult: ParseScheduleResult) {
                executionContext.withUiContext {
                    onParsingDone(parseScheduleResult)
                }
            }

            when (val result = sessionizeNetworkRepository!!.loadConferenceDays()) {
                is SessionizeResult.Values -> {
                    val version = "${result.conferenceDays.hashCode()}"
                    storeConferenceDays(result.conferenceDays, version)
                    notifyFetchingDone(FetchScheduleResult.createSuccess(hostName))
                    notifyParsingDone(ParseScheduleResult(true, version))
                }
                is SessionizeResult.Error -> {
                    logging.e(javaClass.name, result.toString())
                    loadingFailed(requestIdentifier)
                    notifyFetchingDone(FetchScheduleResult(httpStatus = result.httpStatus, hostName = hostName))
                }
                is SessionizeResult.Exception -> {
                    logging.e(javaClass.name, result.toString())
                    loadingFailed(requestIdentifier)
                    result.throwable.printStackTrace()
                    notifyFetchingDone(FetchScheduleResult.createException(
                            result.throwable.toHttpStatus(), hostName, result.throwable.message))
                }
            }
        }
    }

    private fun storeConferenceDays(conferenceDays: List<ConferenceDay>, version: String) {
        val metaAppModel = conferenceDays.toMetaAppModel()
        updateMeta(metaAppModel.copy(version = version))
        val eventAppModels = conferenceDays.toEventAppModels()
        val oldLectures = loadLecturesForAllDays()
        val hasChanged = ScheduleChanges.hasScheduleChanged(eventAppModels, oldLectures)
        if (hasChanged) {
            resetChangesSeenFlag()
        }
        updateLectures(eventAppModels)
    }

    fun loadSchedule(url: String,
                     eTag: String,
                     okHttpClient: OkHttpClient,
                     onFetchingDone: (fetchScheduleResult: FetchScheduleResult) -> Unit,
                     onParsingDone: (parseScheduleResult: ParseScheduleResult) -> Unit
    ) {
        check(onFetchingDone != {}) { "Nobody registered to receive FetchScheduleResult." }
        // Fetching
        scheduleNetworkRepository.fetchSchedule(okHttpClient, url, eTag) { fetchScheduleResult ->
            val fetchResult = fetchScheduleResult.toAppFetchScheduleResult()
            onFetchingDone.invoke(fetchResult)

            if (fetchResult.isSuccessful) {
                check(onParsingDone != {}) { "Nobody registered to receive ParseScheduleResult." }
                // Parsing
                parseSchedule(fetchResult.scheduleXml, fetchResult.eTag, onParsingDone)
            }
        }
    }

    private fun parseSchedule(scheduleXml: String,
                              eTag: String,
                              onParsingDone: (parseScheduleResult: ParseScheduleResult) -> Unit) {
        scheduleNetworkRepository.parseSchedule(scheduleXml, eTag,
                onUpdateLectures = { lectures ->
                    val oldLectures = loadLecturesForAllDays()
                    val newLectures = lectures.toLecturesAppModel2().sanitize()
                    val hasChanged = ScheduleChanges.hasScheduleChanged(newLectures, oldLectures)
                    if (hasChanged) {
                        resetChangesSeenFlag()
                    }
                    updateLectures(newLectures)
                },
                onUpdateMeta = { meta ->
                    updateMeta(meta.toMetaAppModel())
                },
                onParsingDone = { result: Boolean, version: String ->
                    onParsingDone(ParseScheduleResult(result, version))
                })
    }

    /**
     * Loads all lectures from the database which take place on all days.
     */
    fun loadLecturesForAllDays() =
            loadLecturesForDayIndex(ALL_DAYS)

    /**
     * Loads all lectures from the database which take place on the specified [day][dayIndex].
     * All days can be loaded if -1 is passed as the [day][dayIndex].
     */
    fun loadLecturesForDayIndex(dayIndex: Int): List<Lecture> {
        val lectures = if (dayIndex == ALL_DAYS) {
            logging.d(javaClass.name, "Loading lectures for all days.")
            readLecturesOrderedByDateUtc()
        } else {
            logging.d(javaClass.name, "Loading lectures for day $dayIndex.")
            readLecturesForDayIndexOrderedByDateUtc(dayIndex)
        }
        logging.d(javaClass.name, "Got ${lectures.size} rows.")

        val highlights = readHighlights()
        for (highlight in highlights) {
            logging.d(javaClass.name, "$highlight")
            for (lecture in lectures) {
                if (lecture.lectureId == "" + highlight.eventId) {
                    lecture.highlight = highlight.isHighlight
                }
            }
        }
        return lectures.toList()
    }

    @JvmOverloads
    fun readAlarms(eventId: String = "") = if (eventId.isEmpty()) {
        alarmsDatabaseRepository.query().toAlarmsAppModel()
    } else {
        alarmsDatabaseRepository.query(eventId).toAlarmsAppModel()
    }

    @JvmOverloads
    fun deleteAlarmForAlarmId(alarmId: Int, closeSQLiteOpenHelper: Boolean = true) =
            alarmsDatabaseRepository.deleteForAlarmId(alarmId, closeSQLiteOpenHelper)

    fun deleteAlarmForEventId(eventId: String) =
            alarmsDatabaseRepository.deleteForEventId(eventId)

    fun updateAlarm(alarm: Alarm) {
        val alarmDatabaseModel = alarm.toAlarmDatabaseModel()
        val values = alarmDatabaseModel.toContentValues()
        alarmsDatabaseRepository.insert(values, alarm.eventId)
    }

    private fun readHighlights() =
            highlightsDatabaseRepository.query().toHighlightsAppModel()

    fun updateHighlight(lecture: Lecture) {
        val highlightDatabaseModel = lecture.toHighlightDatabaseModel()
        val values = highlightDatabaseModel.toContentValues()
        highlightsDatabaseRepository.insert(values, lecture.lectureId)
    }

    fun readLectureByLectureId(lectureId: String) =
            lecturesDatabaseRepository.queryLectureByLectureId(lectureId).first().toLectureAppModel()

    private fun readLecturesForDayIndexOrderedByDateUtc(dayIndex: Int) =
            lecturesDatabaseRepository.queryLecturesForDayIndexOrderedByDateUtc(dayIndex).toLecturesAppModel()

    private fun readLecturesOrderedByDateUtc() =
            lecturesDatabaseRepository.queryLecturesOrderedByDateUtc().toLecturesAppModel()

    fun readDateInfos() =
            readLecturesOrderedByDateUtc().toDateInfos()

    private fun updateLectures(lectures: List<Lecture>) {
        val lecturesDatabaseModel = lectures.toLecturesDatabaseModel()
        val list = lecturesDatabaseModel.map { it.toContentValues() }
        lecturesDatabaseRepository.insert(list)
    }

    fun readMeta() =
            metaDatabaseRepository.query().toMetaAppModel()

    private fun updateMeta(meta: Meta) {
        val metaDatabaseModel = meta.toMetaDatabaseModel()
        val values = metaDatabaseModel.toContentValues()
        metaDatabaseRepository.insert(values)
    }

    fun readScheduleUrl(): String {
        val alternateScheduleUrl = sharedPreferencesRepository.getScheduleUrl()
        return if (alternateScheduleUrl.isEmpty()) {
            BuildConfig.SCHEDULE_URL
        } else {
            alternateScheduleUrl
        }
    }

    fun readScheduleLastFetchingTime() =
            sharedPreferencesRepository.getScheduleLastFetchedAt()

    fun updateScheduleLastFetchingTime() = with(Time()) {
        setToNow()
        sharedPreferencesRepository.setScheduleLastFetchedAt(toMillis(true))
    }

    fun sawScheduleChanges() =
            sharedPreferencesRepository.getChangesSeen()

    fun updateScheduleChangesSeen(changesSeen: Boolean) =
            sharedPreferencesRepository.setChangesSeen(changesSeen)

    private fun resetChangesSeenFlag() =
            updateScheduleChangesSeen(false)

}
