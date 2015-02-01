package maigosoft.mcpdict;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiRic.ui.widget.Boast;

public class FavoriteDialogs {

    public static final int ACTION_NONE = 0;
    public static final int ACTION_ADD = 1;
    public static final int ACTION_EDIT = 2;
    public static final int ACTION_DELETE = 3;

    private static MainActivity activity;

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
}
