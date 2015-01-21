package maigosoft.mcpdict;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class UserDatabase extends SQLiteOpenHelper {

    // STATIC VARIABLES AND METHODS

    private static final String DATABASE_NAME = "user";
    private static final int DATABASE_VERSION = 1;

    private static Context context;
    private static SQLiteDatabase db = null;

    public static void initialize(Context c) {
        if (db != null) return;
        context = c;
        db = new UserDatabase(context).getWritableDatabase();
    }

    public static String getDatabasePath() {
        return context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
    }

    public static Cursor selectAllFavorites() {
        String query = "SELECT rowid AS _id, unicode, comment, " +
                       "STRFTIME('%Y/%m/%d', timestamp, 'localtime') AS local_timestamp " +
                       "FROM favorite ORDER BY timestamp DESC";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    // Returns the status after toggling
    public static boolean toggleFavorite(char c) {
        String unicode = String.format("%04X", (int)c);
        String[] args = {unicode};
        Cursor cursor = db.rawQuery("SELECT unicode FROM favorite WHERE unicode = ?", args);
        if (cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put("unicode", unicode);
            values.put("comment", "TODO");
            db.insert("favorite", null, values);
            return true;
        }
        else {
            db.delete("favorite", "unicode = ?", args);
            return false;
        }
    }

    public static void deleteAllFavorites() {
        db.delete("favorite", null, null);
    }

    // NON-STATIC METHODS IMPLEMENTING THOSE OF THE ABSTRACT SUPER-CLASS

    public UserDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE favorite (" +
                   "    unicode TEXT," +
                   "    comment STRING," +
                   "    timestamp REAL DEFAULT (JULIANDAY('now')) NOT NULL" +
                   ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

}
