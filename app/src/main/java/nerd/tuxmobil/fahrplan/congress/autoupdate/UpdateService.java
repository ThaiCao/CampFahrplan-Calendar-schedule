package nerd.tuxmobil.fahrplan.congress.autoupdate;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.Time;

import org.ligi.tracedroid.logging.Log;

import java.util.List;

import nerd.tuxmobil.fahrplan.congress.MyApp;
import nerd.tuxmobil.fahrplan.congress.MyApp.TASKS;
import nerd.tuxmobil.fahrplan.congress.R;
import nerd.tuxmobil.fahrplan.congress.extensions.Contexts;
import nerd.tuxmobil.fahrplan.congress.models.Lecture;
import nerd.tuxmobil.fahrplan.congress.net.ConnectivityStateReceiver;
import nerd.tuxmobil.fahrplan.congress.net.CustomHttpClient.HTTP_STATUS;
import nerd.tuxmobil.fahrplan.congress.net.FetchFahrplan;
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository;
import nerd.tuxmobil.fahrplan.congress.schedule.MainActivity;
import nerd.tuxmobil.fahrplan.congress.serialization.FahrplanParser;
import nerd.tuxmobil.fahrplan.congress.utils.FahrplanMisc;

public class UpdateService extends IntentService implements
        FetchFahrplan.OnDownloadCompleteListener,
        FahrplanParser.OnParseCompleteListener {

    public UpdateService() {
        super("UpdateService");
    }

    final String LOG_TAG = "UpdateService";

    private FahrplanParser parser;

    @Override
    public void onParseDone(Boolean result, String version) {
        MyApp.LogDebug(LOG_TAG, "parseDone: " + result + " , numDays=" + MyApp.meta.getNumDays());
        MyApp.task_running = TASKS.NONE;
        MyApp.fahrplan_xml = null;
        List<Lecture> changesList = FahrplanMisc.readChanges(this);
        if (!changesList.isEmpty()) {
            showScheduleUpdateNotification(version, changesList.size());
        }
        MyApp.LogDebug(LOG_TAG, "background update complete");
        stopSelf();
    }

    private void showScheduleUpdateNotification(String version, int changesCount) {
        String changesTxt = getResources().getQuantityString(R.plurals.changes_notification,
                changesCount, changesCount);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent contentIntent = PendingIntent
                .getActivity(this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        int reminderColor = ContextCompat.getColor(this, R.color.colorActionBar);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String reminderTone = prefs.getString("reminder_tone", "");

        String contentText;
        if (TextUtils.isEmpty(version)) {
            contentText = getString(R.string.schedule_updated);
        } else {
            contentText = getString(R.string.schedule_updated_to, version);
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setAutoCancel(true)
                .setContentText(contentText)
                .setContentTitle(getString(R.string.app_name))
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setSound(Uri.parse(reminderTone))
                .setContentIntent(contentIntent)
                .setSubText(changesTxt)
                .setColor(reminderColor)
                .build();
        NotificationManager manager = Contexts.getNotificationManager(this);
        manager.notify(2, notification);
    }

    public void parseFahrplan() {
        MyApp.task_running = TASKS.PARSE;
        if (MyApp.parser == null) {
            AppRepository appRepository = AppRepository.Companion.getInstance(getApplicationContext());
            parser = new FahrplanParser(getApplicationContext(), appRepository);
        } else {
            parser = MyApp.parser;
        }
        parser.setListener(this);
        parser.parse(MyApp.fahrplan_xml, MyApp.meta.getETag());
    }

    public void onGotResponse(HTTP_STATUS status, String response, String eTagStr, String host) {
        MyApp.LogDebug(LOG_TAG, "Response... " + status);
        MyApp.task_running = TASKS.NONE;
        if ((status == HTTP_STATUS.HTTP_OK) || (status == HTTP_STATUS.HTTP_NOT_MODIFIED)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            Time now = new Time();
            now.setToNow();
            long millis = now.toMillis(true);
            Editor edit = prefs.edit();
            edit.putLong("last_fetch", millis);
            edit.commit();
        }
        if (status != HTTP_STATUS.HTTP_OK) {
            MyApp.LogDebug(LOG_TAG, "background update failed with " + status);
            stopSelf();
            return;
        }

        MyApp.fahrplan_xml = response;
        MyApp.meta.setETag(eTagStr);
        parseFahrplan();
    }

    private void fetchFahrplan() {
        if (MyApp.task_running == TASKS.NONE) {
            MyApp.task_running = TASKS.FETCH;
            // Bypass legacy data loading!
            AppRepository.Companion.getInstance(this).loadSessions((message) -> {
                if (message.isEmpty()) {
                    onParseDone(true, "foobar");
                } else {
                    // Fail silenty: don't fetch.
                    Log.e(getClass().getName(), message);
                }
                return null;
            });
        } else {
            MyApp.LogDebug(LOG_TAG, "fetch already in progress");
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        MyApp.LogDebug(LOG_TAG, "onHandleIntent");
        Log.d(getClass().getName(), "intent = " + intent);
        ConnectivityManager connectivityManager = Contexts.getConnectivityManager(this);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            MyApp.LogDebug(LOG_TAG, "not connected");
            ConnectivityStateReceiver.enableReceiver(this);
            stopSelf();
            return;
        }

        AppRepository appRepository = AppRepository.Companion.getInstance(getApplicationContext());
        MyApp.meta = appRepository.readMeta(); // to load eTag

        MyApp.LogDebug(LOG_TAG, "going to fetch schedule");
        FahrplanMisc.setUpdateAlarm(this, false);
        fetchFahrplan();
    }

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, UpdateService.class);
        context.startService(intent);
    }

}
