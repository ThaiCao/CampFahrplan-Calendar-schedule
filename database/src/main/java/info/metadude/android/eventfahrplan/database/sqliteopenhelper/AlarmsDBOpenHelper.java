package info.metadude.android.eventfahrplan.database.sqliteopenhelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import info.metadude.android.eventfahrplan.database.contract.FahrplanContract.AlarmsTable;

public class AlarmsDBOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 6;

    private static final String DATABASE_NAME = "alarms";

    private static final String ALARMS_TABLE_CREATE =
            "CREATE TABLE " + AlarmsTable.NAME + " (" +
                    AlarmsTable.Columns.ID + " INTEGER PRIMARY KEY, " +
                    AlarmsTable.Columns.EVENT_TITLE + " TEXT, " +
                    AlarmsTable.Columns.ALARM_TIME_IN_MIN + " INTEGER DEFAULT " +
                    AlarmsTable.Defaults.ALARM_TIME_IN_MIN_DEFAULT + ", " +
                    AlarmsTable.Columns.TIME + " INTEGER, " +
                    AlarmsTable.Columns.TIME_TEXT + " STRING," +
                    AlarmsTable.Columns.EVENT_ID + " INTEGER," +
                    AlarmsTable.Columns.DISPLAY_TIME + " INTEGER," +
                    AlarmsTable.Columns.DAY + " INTEGER);";

    public AlarmsDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ALARMS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if ((oldVersion < 2) && (newVersion >= 2)) {
            db.execSQL("ALTER TABLE " + AlarmsTable.NAME + " ADD " +
                    AlarmsTable.Columns.ALARM_TIME_IN_MIN + " INTEGER DEFAULT" +
                    AlarmsTable.Defaults.ALARM_TIME_IN_MIN_DEFAULT);
        }
        if (oldVersion < 6) {
            // Clear database from FrOSCon 2017
            db.execSQL("DROP TABLE IF EXISTS " + AlarmsTable.NAME);
            onCreate(db);
        }
    }
}
