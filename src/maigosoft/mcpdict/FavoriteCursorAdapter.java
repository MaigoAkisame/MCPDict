package maigosoft.mcpdict;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiRic.ui.widget.Boast;

@SuppressLint("UseSparseArrays")
public class FavoriteCursorAdapter extends CursorAdapter {

    private FavoriteFragment fragment;
    private int layout;
    private LayoutInflater inflater;
    private Map<Integer, View> itemViews;

    public FavoriteCursorAdapter(Context context, FavoriteFragment fragment, int layout, Cursor cursor) {
        super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
        this.fragment = fragment;
        this.layout = layout;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemViews = new HashMap<Integer, View>();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, parent, false);
    }

    // By default, ListView recycles its item views.
    // But we want each favorite item to use its own view, because it has to
    //   remember the SearchResultFragment that may be added to it.
    // In API levels >= 16, we can do this by executing the following on each item view:
    //   ViewCompat.setHasTransientState(view, true);
    // But this has no effect for API levels < 16.
    // Therefore we have to override the getView method of the adapter, as follows.
    // On Android 3.x and 4.x, this causes a crash when switching tabs,
    //   and I've found that override the unregisterDataSetObserver method
    //   of the SearchResultCursorAdapter class solves the problem.
    //   (Reference: http://stackoverflow.com/a/9173866)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        if (!itemViews.containsKey(position)) {
            View v = newView(mContext, mCursor, parent);
            bindView(v, mContext, mCursor);
            itemViews.put(position, v);
        }
        return itemViews.get(position);
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        final char unicode;
        String string;
        TextView textView;

        // Chinese character
        string = cursor.getString(cursor.getColumnIndex("unicode"));
        unicode = (char)Integer.parseInt(string, 16);
        string = String.valueOf(unicode);
        textView = (TextView) view.findViewById(R.id.text_hz);
        textView.setText(string);

        // Timestamp
        string = cursor.getString(cursor.getColumnIndex("local_timestamp"));
        textView = (TextView) view.findViewById(R.id.text_timestamp);
        textView.setText(string);

        // Comment
        string = cursor.getString(cursor.getColumnIndex("comment"));
        textView = (TextView) view.findViewById(R.id.text_comment);
        textView.setText(string);

        // "Edit" button


        // "Delete" button
        final Button button = (Button) view.findViewById(R.id.button_delete);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserDatabase.toggleFavorite(unicode);
                int messageId = R.string.favorite_delete_done;
                String message = context.getResources().getString(messageId).replace("X", String.valueOf(unicode));
                Boast.showText(context, message, Toast.LENGTH_SHORT);
                fragment.refresh(false);
            }
        });

        // Give the container a unique ID (the Unicode),
        //   so that a SearchResultFragment may be added to it
        view.findViewWithTag("container").setId((int) unicode);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        itemViews.clear();
        super.changeCursor(cursor);
    }
}
