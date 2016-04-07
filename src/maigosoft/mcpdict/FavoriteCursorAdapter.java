package maigosoft.mcpdict;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

@SuppressLint("UseSparseArrays")
public class FavoriteCursorAdapter extends CursorAdapter {

    private int layout;
    private LayoutInflater inflater;
    private FavoriteFragment fragment;
    private AtomicInteger nextId = new AtomicInteger(42);
        // Answer to life, the universe and everything
    private Set<Character> expandedItems;

    public FavoriteCursorAdapter(Context context, int layout, Cursor cursor, FavoriteFragment fragment) {
        super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
        this.layout = layout;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.fragment = fragment;
        this.expandedItems = new HashSet<Character>();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = inflater.inflate(layout, parent, false);

        // Give the container a unique ID,
        //   so that a SearchResultFragment may be added to it
        int id = nextId.getAndIncrement();
        view.findViewWithTag("container").setId(id);

        // Add a SearchResultFragment to the container
        SearchResultFragment fragment = new SearchResultFragment(false);
        this.fragment.getFragmentManager().beginTransaction().add(id, fragment).commit();
        view.setTag(fragment);
            // Set the fragment as a tag of the view, so it can be retrieved in expandItem

        return view;
    }

    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        final char unicode;
        String string;
        TextView textView;

        // Get the Chinese character from the cursor,
        //   and make sure we're binding it to the view recorded in itemStatus
        string = cursor.getString(cursor.getColumnIndex("unicode"));
        unicode = (char) Integer.parseInt(string, 16);

        // Chinese character
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
        final Button buttonEdit = (Button) view.findViewById(R.id.button_edit);
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FavoriteDialogs.view(unicode, view);
            }
        });

        // "Delete" button
        final Button buttonDelete = (Button) view.findViewById(R.id.button_delete);
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FavoriteDialogs.delete(unicode, false);
            }
        });

        // Restore expanded status
        if (expandedItems.contains(unicode)) {
            expandItem(unicode, view);
        }
        else {
            collapseItem(unicode, view);
        }
    }

    public boolean isItemExpanded(char unicode) {
        return expandedItems.contains(unicode);
    }

    public void expandItem(char unicode, View view) {
        expandItem(unicode, view, null);
    }

    // Mark a Chinese character as expanded
    // If a view is provided, expand that view, too
    // If a list is provided, scroll the list so that the view is entirely visible
    public void expandItem(final char unicode, final View view, final ListView list) {
        expandedItems.add(unicode);
        if (view == null) return;
        final View container = view.findViewWithTag("container");
        final SearchResultFragment fragment = (SearchResultFragment) view.getTag();
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... params) {
                return MCPDatabase.directSearch(unicode);
            }
            @Override
            protected void onPostExecute(Cursor data) {
                fragment.setData(data);
                container.setVisibility(View.VISIBLE);
                if (list == null) return;
                scrollListToShowItem(list, view);
            }
        }.execute();
    }

    public void collapseItem(char unicode) {
        collapseItem(unicode, null, null);
    }

    public void collapseItem(char unicode, View view) {
        collapseItem(unicode, view, null);
    }

    // Mark a Chinese character as collapsed
    // If a view is provided, collapsed that view, too
    // If a list is provided, scroll the list so that the view is entirely visible
    public void collapseItem(char unicode, View view, ListView list) {
        expandedItems.remove(unicode);
        if (view == null) return;
        View container = view.findViewWithTag("container");
        container.setVisibility(View.GONE);
        if (list == null) return;
        scrollListToShowItem(list, view);
    }

    // Mark all Chinese characters as collapsed
    // Only called when clearing all favorite characters
    public void collapseAll() {
        expandedItems.clear();
    }

    // Scroll a list so that a view inside it becomes entirely visible
    // If the view is taller than the list, make sure the view's bottom is visible
    // This method had better reside in a utility class
    public static void scrollListToShowItem(final ListView list, final View view) {
        list.post(new Runnable() {
            @Override
            public void run() {
                int top = view.getTop();
                int bottom = view.getBottom();
                int height = bottom - top;
                int listTop = list.getPaddingTop();
                int listBottom = list.getHeight() - list.getPaddingBottom();
                int listHeight = listBottom - listTop;
                int y = (height > listHeight || bottom > listBottom) ? (listBottom - height) :
                        (top < listTop) ? listTop : top;
                int position = list.getPositionForView(view);
                list.setSelectionFromTop(position, y);
            }
        });
    }
}
