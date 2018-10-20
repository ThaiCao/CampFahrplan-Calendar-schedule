package nerd.tuxmobil.fahrplan.congress.utils;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.text.format.Time;
import android.widget.Toast;

import org.ligi.tracedroid.logging.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nerd.tuxmobil.fahrplan.congress.MyApp;
import nerd.tuxmobil.fahrplan.congress.R;
import nerd.tuxmobil.fahrplan.congress.alarms.AlarmReceiver;
import nerd.tuxmobil.fahrplan.congress.alarms.AlarmServices;
import nerd.tuxmobil.fahrplan.congress.alarms.AlarmUpdater;
import nerd.tuxmobil.fahrplan.congress.dataconverters.AlarmExtensions;
import nerd.tuxmobil.fahrplan.congress.extensions.Contexts;
import nerd.tuxmobil.fahrplan.congress.models.Alarm;
import nerd.tuxmobil.fahrplan.congress.models.DateInfo;
import nerd.tuxmobil.fahrplan.congress.models.DateInfos;
import nerd.tuxmobil.fahrplan.congress.models.Highlight;
import nerd.tuxmobil.fahrplan.congress.models.Lecture;
import nerd.tuxmobil.fahrplan.congress.models.SchedulableAlarm;
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository;
import nerd.tuxmobil.fahrplan.congress.wiki.WikiEventUtils;


public class FahrplanMisc {

    private static final String LOG_TAG = "FahrplanMisc";
    private static final int ALL_DAYS = -1;
    private static final DateFormat TIME_TEXT_DATE_FORMAT =
            SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);

    public static void loadDays(Context context) {
        MyApp.dateInfos = new DateInfos();
        List<DateInfo> dateInfos = AppRepository.Companion.getInstance(context).readDateInfos();
        for (DateInfo dateInfo : dateInfos) {
            if (!MyApp.dateInfos.contains(dateInfo)) {
                MyApp.dateInfos.add(dateInfo);
            }
        }
        for (DateInfo dateInfo : MyApp.dateInfos) {
            MyApp.LogDebug(LOG_TAG, "DateInfo: " + dateInfo);
        }
    }

    public static String getCalendarDescription(final Context context, final Lecture lecture) {
        StringBuilder sb = new StringBuilder();
        sb.append(lecture.description);
        sb.append("\n\n");
        String links = lecture.getLinks();
        if (WikiEventUtils.linksContainWikiLink(links)) {
            links = links.replaceAll("\\),", ")<br>");
            links = StringUtils.getHtmlLinkFromMarkdown(links);
            sb.append(links);
        } else {
            String eventOnline = context.getString(R.string.event_online);
            sb.append(eventOnline);
            sb.append(": ");
            String eventUrl = new EventUrlComposer(lecture).getEventUrl();
            sb.append(eventUrl);
        }
        return sb.toString();
    }

    public static long getLectureStartTime(@NonNull Lecture lecture) {
        long when;
        if (lecture.dateUTC > 0) {
            when = lecture.dateUTC;
        } else {
            Time time = lecture.getTime();
            when = time.normalize(true);
        }
        return when;
    }

    @SuppressLint("NewApi")
    public static void addToCalender(Context context, Lecture l) {
        Intent intent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI);

        intent.putExtra(CalendarContract.Events.TITLE, l.title);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, l.room);

        long startTime = getLectureStartTime(l);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTime + (l.duration * 60000));
        final String description = getCalendarDescription(context, l);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, description);
        try {
            context.startActivity(intent);
            return;
        } catch (ActivityNotFoundException e) {
        }
        intent.setAction(Intent.ACTION_EDIT);
        try {
            context.startActivity(intent);
            return;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.add_to_calendar_failed, Toast.LENGTH_LONG).show();
        }
    }

    public static void deleteAlarm(@NonNull Context context, @NonNull Lecture lecture) {
        AppRepository appRepository = AppRepository.Companion.getInstance(context);
        String eventId = lecture.lecture_id;
        List<Alarm> alarms = appRepository.readAlarms(eventId);
        if (!alarms.isEmpty()) {
            // Delete any previous alarms of this event.
            Alarm alarm = alarms.get(0);
            SchedulableAlarm schedulableAlarm = AlarmExtensions.toSchedulableAlarm(alarm);
            AlarmServices.discardEventAlarm(context, schedulableAlarm);
            appRepository.deleteAlarmForEventId(eventId);
        }
        lecture.has_alarm = false;
    }

    public static void addAlarm(@NonNull Context context,
                                @NonNull Lecture lecture,
                                int alarmTimesIndex) {
        Log.d(FahrplanMisc.class.getName(), "Add alarm for lecture: " + lecture.lecture_id +
                ", alarmTimesIndex: " + alarmTimesIndex);
        String[] alarm_times = context.getResources().getStringArray(R.array.alarm_time_values);
        List<String> alarmTimeStrings = new ArrayList<>(Arrays.asList(alarm_times));
        List<Integer> alarmTimes = new ArrayList<>(alarmTimeStrings.size());
        for (String alarmTimeString : alarmTimeStrings) {
            alarmTimes.add(Integer.parseInt(alarmTimeString));
        }
        long when;
        Time time;
        long startTime;
        long startTimeInSeconds = lecture.dateUTC;

        if (startTimeInSeconds > 0) {
            when = startTimeInSeconds;
            startTime = startTimeInSeconds;
            time = new Time();
        } else {
            time = lecture.getTime();
            startTime = time.normalize(true);
            when = time.normalize(true);
        }
        long alarmTimeDiffInSeconds = alarmTimes.get(alarmTimesIndex) * 60 * 1000L;
        when -= alarmTimeDiffInSeconds;

        // DEBUG
        // when = System.currentTimeMillis() + (30 * 1000);

        time.set(when);
        MyApp.LogDebug("addAlarm",
                "Alarm time: " + time.format("%Y-%m-%dT%H:%M:%S%z") + ", in seconds: " + when);

        String eventId = lecture.lecture_id;
        String eventTitle = lecture.title;
        int alarmTimeInMin = alarmTimes.get(alarmTimesIndex);
        String timeText = TIME_TEXT_DATE_FORMAT.format(new Date(when));
        int day = lecture.day;

        Alarm alarm = new Alarm(alarmTimeInMin, day, startTime, eventId, eventTitle, when, timeText);
        SchedulableAlarm schedulableAlarm = AlarmExtensions.toSchedulableAlarm(alarm);
        AlarmServices.scheduleEventAlarm(context, schedulableAlarm, true);
        AppRepository.Companion.getInstance(context).updateAlarm(alarm);
        lecture.has_alarm = true;
    }

    public static long setUpdateAlarm(Context context, boolean initial) {
        final AlarmManager alarmManager = Contexts.getAlarmManager(context);
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.setAction(AlarmReceiver.ALARM_UPDATE);
        final PendingIntent pendingintent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

        MyApp.LogDebug(LOG_TAG, "set update alarm");
        Time t = new Time();
        t.setToNow();
        final long now = t.toMillis(true);

        AlarmUpdater alarmUpdater = new AlarmUpdater(MyApp.conferenceTimeFrame,
                new AlarmUpdater.OnAlarmUpdateListener() {

                    @Override
                    public void onCancelAlarm() {
                        MyApp.LogDebug(LOG_TAG, "cancel alarm post congress");
                        alarmManager.cancel(pendingintent);
                    }

                    @Override
                    public void onRescheduleAlarm(long interval, long nextFetch) {
                        MyApp.LogDebug(LOG_TAG, "update alarm to interval " + interval +
                                ", next in " + (nextFetch - now));
                        alarmManager.setInexactRepeating(
                                AlarmManager.RTC_WAKEUP, nextFetch, interval, pendingintent);
                    }

                    @Override
                    public void onRescheduleInitialAlarm(long interval, long nextFetch) {
                        MyApp.LogDebug(LOG_TAG, "set initial alarm to interval " + interval +
                                ", next in " + (nextFetch - now));
                        alarmManager.setInexactRepeating(
                                AlarmManager.RTC_WAKEUP, nextFetch, interval, pendingintent);
                    }
                });
        return alarmUpdater.calculateInterval(now, initial);
    }

    @NonNull
    public static List<Lecture> loadLecturesForAllDays(@NonNull Context context) {
        return loadLecturesForDayIndex(context, ALL_DAYS);
    }

    /**
     * Load all Lectures from the DB on the day specified
     *
     * @param context The Android Context
     * @param day     The day to load lectures for (0..), or -1 for all days
     * @return ArrayList of Lecture objects
     */
    @NonNull
    public static List<Lecture> loadLecturesForDayIndex(@NonNull Context context, int day) {
        AppRepository appRepository = AppRepository.Companion.getInstance(context);

        List<Lecture> lectures;
        if (day == ALL_DAYS) {
            MyApp.LogDebug(LOG_TAG, "Loading lectures for all days.");
            lectures = appRepository.readLecturesOrderedByDateUtc();
        } else {
            MyApp.LogDebug(LOG_TAG, "Loading lectures for day " + day + ".");
            lectures = appRepository.readLecturesForDayIndexOrderedByDateUtc(day);
        }
        MyApp.LogDebug(LOG_TAG, "Got " + lectures.size() + " rows.");

        List<Highlight> highlights = appRepository.readHighlights();
        for (Highlight highlight : highlights) {
            MyApp.LogDebug(LOG_TAG, "highlight = " + highlight);
            for (Lecture lecture : lectures) {
                if (lecture.lecture_id.equals("" + highlight.getEventId())) {
                    lecture.highlight = highlight.isHighlight();
                }
            }
        }
        return lectures;
    }

    public static int getChangedLectureCount(@NonNull List<Lecture> list, boolean favsOnly) {
        int count = 0;
        if (list.isEmpty()) {
            return 0;
        }
        for (int lectureIndex = 0; lectureIndex < list.size(); lectureIndex++) {
            Lecture l = list.get(lectureIndex);
            if (l.isChanged() && ((!favsOnly) || (l.highlight))) {
                count++;
            }
        }
        MyApp.LogDebug(LOG_TAG, "getChangedLectureCount " + favsOnly + ":" + count);
        return count;
    }

    public static int getNewLectureCount(@NonNull List<Lecture> list, boolean favsOnly) {
        int count = 0;
        if (list.isEmpty()) {
            return 0;
        }
        for (int lectureIndex = 0; lectureIndex < list.size(); lectureIndex++) {
            Lecture l = list.get(lectureIndex);
            if ((l.changedIsNew) && ((!favsOnly) || (l.highlight))) count++;
        }
        MyApp.LogDebug(LOG_TAG, "getNewLectureCount " + favsOnly + ":" + count);
        return count;
    }

    public static int getCancelledLectureCount(@NonNull List<Lecture> list, boolean favsOnly) {
        int count = 0;
        if (list.isEmpty()) {
            return 0;
        }
        for (int lectureIndex = 0; lectureIndex < list.size(); lectureIndex++) {
            Lecture l = list.get(lectureIndex);
            if ((l.changedIsCanceled) && ((!favsOnly) || (l.highlight))) count++;
        }
        MyApp.LogDebug(LOG_TAG, "getCancelledLectureCount " + favsOnly + ":" + count);
        return count;
    }

    @NonNull
    public static List<Lecture> readChanges(Context context) {
        MyApp.LogDebug(LOG_TAG, "readChanges");
        List<Lecture> changesList = loadLecturesForAllDays(context);
        if (changesList.isEmpty()) {
            return changesList;
        }
        int lectureIndex = changesList.size() - 1;
        while (lectureIndex >= 0) {
            Lecture l = changesList.get(lectureIndex);
            if (!l.isChanged() && !l.changedIsCanceled && !l.changedIsNew) {
                changesList.remove(l);
            }
            lectureIndex--;
        }
        MyApp.LogDebug(LOG_TAG, changesList.size() + " lectures changed.");
        return changesList;
    }

    @NonNull
    public static List<Lecture> getStarredLectures(@NonNull Context context) {
        List<Lecture> starredList = loadLecturesForAllDays(context);
        if (starredList.isEmpty()) {
            return starredList;
        }
        int lectureIndex = starredList.size() - 1;
        while (lectureIndex >= 0) {
            Lecture l = starredList.get(lectureIndex);
            if (!l.highlight) {
                starredList.remove(l);
            }
            lectureIndex--;
        }
        MyApp.LogDebug(LOG_TAG, starredList.size() + " lectures starred.");
        return starredList;
    }
}
