package maigosoft.mcpdict;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.preference.PreferenceManager;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class MCPDatabase extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "mcpdict";
    private static final int DATABASE_VERSION = 9;

    // Must be the same order as defined in the string array "search_as"
    public static final int SEARCH_AS_HZ = 0;
    public static final int SEARCH_AS_MC = 1;
    public static final int SEARCH_AS_PU = 2;
    public static final int SEARCH_AS_CT = 3;
    public static final int SEARCH_AS_SH = 4;
    public static final int SEARCH_AS_MN = 5;
    public static final int SEARCH_AS_KR = 6;
    public static final int SEARCH_AS_VN = 7;
    public static final int SEARCH_AS_JP_GO = 8;
    public static final int SEARCH_AS_JP_KAN = 9;
    public static final int SEARCH_AS_JP_ANY = 10;

    private static final String[] SEARCH_AS_TO_COLUMN_NAME = {
        "unicode", "mc", "pu", "ct", "sh", "mn", "kr", "vn", "jp_go", "jp_kan", null
    };

    private static Context context;
    private static SQLiteDatabase db = null;

    public static void initialize(Context c) {
        if (db != null) return;
        context = c;
        db = new MCPDatabase(context).getWritableDatabase();
        String userDbPath = UserDatabase.getDatabasePath();
        db.execSQL("ATTACH DATABASE '" + userDbPath + "' AS user");
    }

    public MCPDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgradeVersion(DATABASE_VERSION);
        // Uncomment the following statements to force a database upgrade during development
        // SQLiteDatabase db = getWritableDatabase();
        // db.setVersion(-1);
        // db.close();
        // db = getWritableDatabase();
    }

    @SuppressWarnings("deprecation")
    public static Cursor search(String input, int mode) {
        // Search for one or more keywords, considering mode and options

        // Get options and settings from SharedPreferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Resources r = context.getResources();
        boolean kuangxYonhOnly = sp.getBoolean(r.getString(R.string.pref_key_kuangx_yonh_only), false);
        boolean allowVariants = sp.getBoolean(r.getString(R.string.pref_key_allow_variants), true);
        boolean toneInsensitive = sp.getBoolean(r.getString(R.string.pref_key_tone_insensitive), false);
        int cantoneseSystem = sp.getInt(r.getString(R.string.pref_key_cantonese_romanization), 0);

        // Split the input string into keywords and canonicalize them
        List<String> keywords = new ArrayList<String>();
        List<String> variants = new ArrayList<String>();
        if (mode == SEARCH_AS_HZ) {     // Each character is a query
            for (int i = 0; i < input.length(); i++) {
                char inputChar = input.charAt(i);
                if (!Orthography.Hanzi.isHanzi(inputChar)) continue;
                if (input.indexOf(inputChar) < i) continue;     // Ignore a character if it has appeared earlier
                String inputHex = String.format("%04X", (int) inputChar);
                if (!allowVariants) {
                    keywords.add(inputHex);
                }
                else {
                    for (char variant : Orthography.Hanzi.getVariants(inputChar)) {
                        String variantHex = String.format("%04X", (int) variant);
                        int p = keywords.indexOf(variantHex);
                        if (variant == inputChar) {
                            if (p >= 0) {       // The character itself must appear where it is
                                keywords.remove(p);
                                variants.remove(p);
                            }
                            keywords.add(inputHex);
                            variants.add(null); // And no variant information is appended
                        }
                        else {
                            if (p == -1) {      // This variant character may have appeared before
                                keywords.add(variantHex);
                                variants.add(inputHex);
                            }
                            else {
                                if (variants.get(p) != null) {
                                    variants.set(p, variants.get(p) + " " + inputHex);
                                }
                            }
                        }
                    }
                }
            }
        }
        else {                          // Each contiguous run of non-separator and non-comma characters is a query
            if (mode == SEARCH_AS_KR) { // For Korean, put separators around all hanguls
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < input.length(); i++) {
                    char c = input.charAt(i);
                    if (Orthography.Korean.isHangul(c)) {
                        sb.append(" " + c + " ");
                    }
                    else {
                        sb.append(c);
                    }
                }
                input = sb.toString();
            }
            for (String token : input.split("[\\s,]+")) {
                if (token.equals("")) continue;
                token = token.toLowerCase(Locale.US);
                // Canonicalization
                switch (mode) {
                    case SEARCH_AS_MC: token = Orthography.MiddleChinese.canonicalize(token); break;
                    case SEARCH_AS_PU: token = Orthography.Mandarin.canonicalize(token); break;
                    case SEARCH_AS_CT: token = Orthography.Cantonese.canonicalize(token, cantoneseSystem); break;
                    case SEARCH_AS_SH: token = Orthography.Shanghai.canonicalize(token); break;
                    case SEARCH_AS_MN: token = Orthography.Minnan.canonicalize(token); break;
                    case SEARCH_AS_KR: token = Orthography.Korean.canonicalize(token); break;
                    case SEARCH_AS_VN: token = Orthography.Vietnamese.canonicalize(token); break;
                    case SEARCH_AS_JP_GO: case SEARCH_AS_JP_KAN: case SEARCH_AS_JP_ANY:
                                       token = Orthography.Japanese.canonicalize(token); break;
                }
                if (token == null) continue;
                List<String> allTones = null;
                if (toneInsensitive) {
                    switch (mode) {
                        case SEARCH_AS_MC: allTones = Orthography.MiddleChinese.getAllTones(token); break;
                        case SEARCH_AS_PU: allTones = Orthography.Mandarin.getAllTones(token); break;
                        case SEARCH_AS_CT: allTones = Orthography.Cantonese.getAllTones(token); break;
                        case SEARCH_AS_SH: allTones = Orthography.Shanghai.getAllTones(token); break;
                        case SEARCH_AS_MN: allTones = Orthography.Minnan.getAllTones(token); break;
                        case SEARCH_AS_VN: allTones = Orthography.Vietnamese.getAllTones(token); break;
                    }
                }
                if (allTones != null) {
                    keywords.addAll(allTones);
                }
                else {
                    keywords.add(token);
                }
            }
        }
        if (keywords.isEmpty()) return null;

        // Columns to search
        String[] columns = (mode != SEARCH_AS_JP_ANY) ?
                            new String[] {SEARCH_AS_TO_COLUMN_NAME[mode]} :
                            new String[] {"jp_go", "jp_kan", "jp_tou", "jp_kwan", "jp_other"};

        // Build inner query statement (a union query returning the id's of matching Chinese characters)
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("mcpdict");
        List<String> queries = new ArrayList<String>();
        List<String> args = new ArrayList<String>();
        for (int i = 0; i < keywords.size(); i++) {
            String variant = (mode == SEARCH_AS_HZ && allowVariants && variants.get(i) != null) ?
                             ("\"" + variants.get(i) + "\"") : "null";
            String[] projection = {"rowid AS _id", i + " AS rank", variant + " AS variants"};
            for (String column : columns) {
                queries.add(qb.buildQuery(projection, column + " MATCH ?", null, null, null, null, null));
                    // For API level >= 11, omit the third argument (the first null)
                args.add(keywords.get(i));
            }
        }
        String query = qb.buildUnionQuery(queries.toArray(new String[0]), null, null);

        // Build outer query statement (returning all information about the matching Chinese characters)
        qb.setTables("(" + query + ") AS u, mcpdict AS v LEFT JOIN user.favorite AS w ON v.unicode = w.unicode");
        qb.setDistinct(true);
        String[] projection = {"_id",
                   "v.unicode AS unicode", "variants",
                   "mc", "pu", "ct", "sh", "mn", "kr", "vn",
                   "jp_go", "jp_kan", "jp_tou", "jp_kwan", "jp_other",
                   "timestamp IS NOT NULL AS is_favorite", "comment"};
        String selection = "u._id = v.rowid";
        if (kuangxYonhOnly) {
            selection += " AND mc IS NOT NULL";
        }
        query = qb.buildQuery(projection, selection, null, null, null, "rank", null);
            // For API level >= 11, omit the third argument (the first null)

        // Search
        return db.rawQuery(query, args.toArray(new String[0]));
    }

    @SuppressWarnings("deprecation")
    public static Cursor directSearch(char unicode) {
        // Search for a single Chinese character without any conversions
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("mcpdict AS v LEFT JOIN user.favorite AS w ON v.unicode = w.unicode");
        String[] projection = {"v.rowid AS _id",
                   "v.unicode AS unicode", "NULL AS variants",
                   "mc", "pu", "ct", "sh", "mn", "kr", "vn",
                   "jp_go", "jp_kan", "jp_tou", "jp_kwan", "jp_other",
                   "timestamp IS NOT NULL AS is_favorite", "comment"};
        String selection = "v.unicode = ?";
        String query = qb.buildQuery(projection, selection, null, null, null, null, null);
        String[] args = {String.format("%04X", (int) unicode)};
        return db.rawQuery(query, args);
    }

}
