package maigosoft.mcpdict;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiRic.ui.widget.Boast;

@SuppressLint("SimpleDateFormat")
public class FavoriteDialogs {

    private static MainActivity activity;

    private static int importMode;

    public static void initialize(MainActivity activity) {
       FavoriteDialogs.activity = activity;
    }

    public static void add(final char unicode) {
        final EditText editText = new EditText(activity);
        editText.setHint(R.string.favorite_add_hint);
        editText.setSingleLine(false);
        new AlertDialog.Builder(activity)
            .setIcon(R.drawable.ic_star_yellow)
            .setTitle(String.format(activity.getString(R.string.favorite_add), unicode))
            .setView(editText)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String comment = editText.getText().toString();
                    UserDatabase.insertFavorite(unicode, comment);
                    String message = String.format(activity.getString(R.string.favorite_add_done), unicode);
                    Boast.showText(activity, message, Toast.LENGTH_SHORT);
                    FavoriteFragment fragment = activity.getFavoriteFragment();
                    if (fragment != null) {
                        fragment.notifyAddItem();
                    }
                    activity.getCurrentFragment().refresh();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    public static void view(final char unicode, final View view) {
        new AlertDialog.Builder(activity)
            .setIcon(R.drawable.ic_star_yellow)
            .setTitle(String.format(activity.getString(R.string.favorite_view), unicode))
            .setMessage(((TextView) view.findViewById(R.id.text_comment)).getText())
            .setPositiveButton(String.format(activity.getString(R.string.favorite_edit_2lines), unicode),
                               new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FavoriteDialogs.edit(unicode, view);
                }
            })
            .setNegativeButton(String.format(activity.getString(R.string.favorite_delete_2lines), unicode),
                               new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FavoriteDialogs.delete(unicode, false);
                }
            })
            .setNeutralButton(R.string.back, null)
            .show();
    }

    public static void edit(final char unicode, View view) {
        final EditText editText = new EditText(activity);
        editText.setText(((TextView) view.findViewById(R.id.text_comment)).getText());
        editText.setSingleLine(false);
        new AlertDialog.Builder(activity)
            .setIcon(R.drawable.ic_star_yellow)
            .setTitle(String.format(activity.getString(R.string.favorite_edit), unicode))
            .setView(editText)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String comment = editText.getText().toString();
                    UserDatabase.updateFavorite(unicode, comment);
                    String message = String.format(activity.getString(R.string.favorite_edit_done), unicode);
                    Boast.showText(activity, message, Toast.LENGTH_SHORT);
                    activity.getCurrentFragment().refresh();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    public static void delete(final char unicode, boolean force) {
        if (force) {
            UserDatabase.deleteFavorite(unicode);
            String message = String.format(activity.getString(R.string.favorite_delete_done), unicode);
            Boast.showText(activity, message, Toast.LENGTH_SHORT);
            FavoriteFragment fragment = activity.getFavoriteFragment();
            if (fragment != null) {
                FavoriteCursorAdapter adapter = (FavoriteCursorAdapter) fragment.getListAdapter();
                adapter.collapseItem(unicode);
            }
            activity.getCurrentFragment().refresh();
            return;
        }

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        final String prefKey = activity.getString(R.string.pref_key_favorite_delete_no_confirm_expiry);
        long expiry = sp.getLong(prefKey, 0);
        long now = System.currentTimeMillis();
        boolean expired = (expiry == 0 || now > expiry);
        if (!expired) {
            delete(unicode, true);
            return;
        }

        final CheckBox checkBox = new CheckBox(activity);
        checkBox.setText(R.string.favorite_delete_no_confirm);
        new AlertDialog.Builder(activity)
            .setIcon(R.drawable.ic_alert)
            .setTitle(String.format(activity.getString(R.string.favorite_delete), unicode))
            .setMessage(String.format(activity.getString(R.string.favorite_delete_confirm), unicode))
            .setView(checkBox)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    delete(unicode, true);
                    if (checkBox.isChecked()) {
                        sp.edit().putLong(prefKey, System.currentTimeMillis() + 3600000).commit();
                            // No confirmation for 1 hour
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    public static void deleteAll() {
        new AlertDialog.Builder(activity)
            .setIcon(R.drawable.ic_alert)
            .setTitle(activity.getString(R.string.favorite_clear))
            .setMessage(activity.getString(R.string.favorite_clear_confirm))
            .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    UserDatabase.deleteAllFavorites();
                    String message = activity.getString(R.string.favorite_clear_done);
                    Boast.showText(activity, message, Toast.LENGTH_SHORT);
                    FavoriteFragment fragment = activity.getFavoriteFragment();
                    if (fragment != null) {
                        FavoriteCursorAdapter adapter = (FavoriteCursorAdapter) fragment.getListAdapter();
                        adapter.collapseAll();
                    }
                    activity.getCurrentFragment().refresh();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    public static void export(boolean force) {
        File backupFile = new File(UserDatabase.getBackupPath());
        if (force || !backupFile.exists()) {
            try {
                UserDatabase.exportFavorites();
                new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_info)
                    .setTitle(activity.getString(R.string.favorite_export))
                    .setMessage(String.format(activity.getString(R.string.favorite_export_done),
                                              UserDatabase.getBackupPath()))
                    .setPositiveButton(R.string.ok, null)
                    .show();
            }
            catch (IOException e) {
                crash(e);
            }
        }
        else {
            long timestamp = backupFile.lastModified();
            new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_alert)
                .setTitle(activity.getString(R.string.favorite_export))
                .setMessage(String.format(activity.getString(R.string.favorite_export_overwrite),
                            UserDatabase.getBackupPath(),
                            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(timestamp))))
                .setPositiveButton(R.string.overwrite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        export(true);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }
    }

    public static void import_(int state) {
        // States:
        //   0: check if the backup file exists, is readable, and contains entries,
        //      and display info about the backup file
        //   1: prompt for import mode
        //   2: do the importing, and (optionally) delete the backup file

        switch (state) {
        case 0:
            File backupFile = new File(UserDatabase.getBackupPath());
            if (!backupFile.exists()) {
                new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_error)
                    .setTitle(activity.getString(R.string.favorite_import))
                    .setMessage(String.format(activity.getString(R.string.favorite_import_file_not_found),
                                              UserDatabase.getBackupPath()))
                    .setPositiveButton(R.string.ok, null)
                    .show();
                break;
            }

            int count = 0;
            try {
                count = UserDatabase.selectBackupFavoriteCount();
            }
            catch (SQLiteException e) {
                new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_error)
                    .setTitle(activity.getString(R.string.favorite_import))
                    .setMessage(String.format(activity.getString(R.string.favorite_import_read_fail),
                                              UserDatabase.getBackupPath()))
                    .setPositiveButton(R.string.ok, null)
                    .show();
                break;
            }

            if (count == 0) {
                new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_error)
                    .setTitle(activity.getString(R.string.favorite_import))
                    .setMessage(String.format(activity.getString(R.string.favorite_import_empty_file),
                                              UserDatabase.getBackupPath()))
                    .setPositiveButton(R.string.ok, null)
                    .show();
                break;
            }

            if (UserDatabase.selectAllFavorites().getCount() == 0) {
                new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_info)
                .setTitle(activity.getString(R.string.favorite_import))
                .setMessage(String.format(activity.getString(R.string.favorite_import_detail),
                                          UserDatabase.getBackupPath(),
                                          count))
                .setPositiveButton(R.string.import_, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importMode = 0;
                        import_(2);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
            }
            else {
                new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_info)
                    .setTitle(activity.getString(R.string.favorite_import))
                    .setMessage(String.format(activity.getString(R.string.favorite_import_detail_select_mode),
                                              UserDatabase.getBackupPath(),
                                              count))
                    .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            import_(1);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            }
            break;

        case 1:
            new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_question)
                .setTitle(activity.getString(R.string.favorite_import_select_mode))
                .setSingleChoiceItems(R.array.favorite_import_modes, -1, null)
                .setPositiveButton(R.string.import_, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importMode = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        import_(2);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
            break;

        case 2:
            try {
                switch (importMode) {
                    case 0: UserDatabase.importFavoritesOverwrite(); break;
                    case 1: UserDatabase.importFavoritesMix(); break;
                    case 2: UserDatabase.importFavoritesAppend(); break;
                }
            }
            catch (IOException | SQLiteException e) {
                crash(e);
                break;
            }

            FavoriteFragment fragment = activity.getFavoriteFragment();
            if (fragment != null) {
                fragment.notifyAddItem();
                FavoriteCursorAdapter adapter = (FavoriteCursorAdapter) fragment.getListAdapter();
                adapter.collapseAll();
            }
            activity.getCurrentFragment().refresh();

            new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_info)
                .setTitle(activity.getString(R.string.favorite_import))
                .setMessage(String.format(activity.getString(R.string.favorite_import_done),
                        UserDatabase.getBackupPath()))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File backupFile = new File(UserDatabase.getBackupPath());
                        backupFile.delete();
                        String message = activity.getString(backupFile.exists() ?
                                                            R.string.favorite_import_delete_backup_fail :
                                                            R.string.favorite_import_delete_backup_done);
                        Boast.showText(activity, message, Toast.LENGTH_SHORT);
                    }
                })
                .setNegativeButton(R.string.keep, null)
                .show();
            break;
        }
    }

    public static void crash(Throwable e) {
        try {
            String logPath = activity.getExternalFilesDir(null) + "/crash.log";
            FileUtils.dumpException(logPath, e);
            new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_error)
                .setTitle(activity.getString(R.string.crash))
                .setMessage(String.format(activity.getString(R.string.crash_saved), logPath))
                .setPositiveButton(R.string.ok, null)
                .show();
        }
        catch (IOException ex) {
            new AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_error)
                .setTitle(activity.getString(R.string.crash))
                .setMessage(activity.getString(R.string.crash_unsaved))
                .setPositiveButton(R.string.ok, null)
                .show();
        }
    }
}
