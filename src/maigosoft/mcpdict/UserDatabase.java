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

    // "READ" OPERATIONS

    public static Cursor selectAllFavorites() {
        String query = "SELECT rowid AS _id, unicode, comment, " +
                       "STRFTIME('%Y/%m/%d', timestamp, 'localtime') AS local_timestamp " +
                       "FROM favorite ORDER BY timestamp DESC";
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public static String getFavoriteMessage(char unicode) {
        String query = "SELECT comment FROM favorite WHERE unicode = ?";
        String[] args = {String.format("%04X", (int) unicode)};
        Cursor data = db.rawQuery(query, args);
        return data.getString(data.getColumnIndex("comment"));
    }

    // "WRITE" OPERATIONS

    public static void insertFavorite(char unicode, String comment) {
        ContentValues values = new ContentValues();
        values.put("unicode", String.format("%04X", (int) unicode));
        values.put("comment", comment);
        db.insert("favorite", null, values);
    }

    public static void updateFavorite(char unicode, String comment) {
        ContentValues values = new ContentValues();
        values.put("comment", comment);
        String[] args = {String.format("%04X", (int) unicode)};
        db.update("favorite", values, "unicode = ?", args);
    }

    public static void deleteFavorite(char unicode) {
        String[] args = {String.format("%04X", (int) unicode)};
        db.delete("favorite", "unicode = ?", args);
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
