package nerd.tuxmobil.fahrplan.congress.contract;

public interface BundleKeys {

    // Add + delete alarm
    String ALARM_SESSION_ID =
            "nerd.tuxmobil.fahrplan.congress.ALARM_SESSION_ID";
    String ALARM_DAY =
            "nerd.tuxmobil.fahrplan.congress.ALARM_DAY";
    String ALARM_TITLE =
            "nerd.tuxmobil.fahrplan.congress.ALARM_TITLE";
    String ALARM_START_TIME =
            "nerd.tuxmobil.fahrplan.congress.ALARM_START_TIME";

    // Session alarm notification
    String BUNDLE_KEY_SESSION_ALARM_SESSION_ID =
            "nerd.tuxmobil.fahrplan.congress.SESSION_ALARM_SESSION_ID";
    String BUNDLE_KEY_SESSION_ALARM_DAY_INDEX =
            "nerd.tuxmobil.fahrplan.congress.SESSION_ALARM_DAY_INDEX";
    String BUNDLE_KEY_SESSION_ALARM_NOTIFICATION_ID =
            "nerd.tuxmobil.fahrplan.congress.SESSION_ALARM_NOTIFICATION_ID";

    // Session details
    String SESSION_ID =
            "nerd.tuxmobil.fahrplan.congress.SESSION_ID";

    // Side pane
    String SIDEPANE =
            "nerd.tuxmobil.fahrplan.congress.SIDEPANE";

    // Changes dialog
    String CHANGES_DLG_NUM_CHANGED =
            "nerd.tuxmobil.fahrplan.congress.ChangesDialog.NUM_CHANGES";
    String CHANGES_DLG_NUM_NEW =
            "nerd.tuxmobil.fahrplan.congress.ChangesDialog.NUM_NEW";
    String CHANGES_DLG_NUM_CANCELLED =
            "nerd.tuxmobil.fahrplan.congress.ChangesDialog.NUM_CANCELLED";
    String CHANGES_DLG_NUM_MARKED =
            "nerd.tuxmobil.fahrplan.congress.ChangesDialog.NUM_MARKED";
    String CHANGES_DLG_VERSION =
            "nerd.tuxmobil.fahrplan.congress.ChangesDialog.VERSION";

    // Settings
    String BUNDLE_KEY_SCHEDULE_URL_UPDATED =
            "nerd.tuxmobil.fahrplan.congress.Prefs.SCHEDULE_URL_UPDATED";
    String BUNDLE_KEY_ENGELSYSTEM_SHIFTS_URL_UPDATED =
            "nerd.tuxmobil.fahrplan.congress.Prefs.ENGELSYSTEM_SHIFTS_URL_UPDATED";
    String BUNDLE_KEY_ALTERNATIVE_HIGHLIGHTING_UPDATED =
            "nerd.tuxmobil.fahrplan.congress.Prefs.ALTERNATIVE_HIGHLIGHT";
    String BUNDLE_KEY_USE_DEVICE_TIME_ZONE_UPDATED =
            "nerd.tuxmobil.fahrplan.congress.Prefs.USE_DEVICE_TIME_ZONE_UPDATED";

}
