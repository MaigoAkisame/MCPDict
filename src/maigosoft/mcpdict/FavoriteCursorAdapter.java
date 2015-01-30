package maigosoft.mcpdict;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

@SuppressLint("UseSparseArrays")
public class FavoriteCursorAdapter extends CursorAdapter {

    private Context context;
    private int layout;
    private LayoutInflater inflater;
    private FragmentManager fm;
    private Map<Character, ItemStatus> itemStatus;

    public FavoriteCursorAdapter(Context context, int layout, Cursor cursor) {
        super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER);
        this.context = context;
        this.layout = layout;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemStatus = new HashMap<Character, ItemStatus>();
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
        String string = mCursor.getString(mCursor.getColumnIndex("unicode"));
        char unicode = (char) Integer.parseInt(string, 16);
        if (!itemStatus.containsKey(unicode)) {
            View view = newView(mContext, mCursor, parent);
            itemStatus.put(unicode, new ItemStatus(view));
        }
        View view = itemStatus.get(unicode).view;
        bindView(view, mContext, mCursor);
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
        if (!itemStatus.containsKey(unicode) || itemStatus.get(unicode).view != view) {
            throw new IllegalStateException("FavoriteCursorAdapter: View to bind not recorded in itemStatus");
        }
        Log.d("MCP", "bindView " + unicode);

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
                Log.d("MCP", "edit clicked");
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

        // Give the container a unique ID (the Unicode),
        //   so that a SearchResultFragment may be added to it
        Log.d("MCP", "before: container.id = " + view.findViewWithTag("container").getId());
        view.findViewWithTag("container").setId((int) unicode);
        Log.d("MCP", "after: container.id = " + view.findViewWithTag("container").getId());

        // Restore expanded status
        if (itemStatus.get(unicode).isExpanded) {
            expandItem(unicode);
        }
        else {
            collapseItem(unicode);
        }
    }

    public void setFragmentManager(FragmentManager fm) {
        this.fm = fm;
    }

    public boolean isItemExpanded(char unicode) {
        return itemStatus.containsKey(unicode) && itemStatus.get(unicode).isExpanded;
    }

    public void expandItem(char unicode) {
        Log.d("MCP", "start expanding " + unicode);
        ItemStatus status = itemStatus.get(unicode);
        if (status == null) return;
        status.isExpanded = true;
        if (status.view == null) return;
        View container = status.getContainer();
        if (container == null) return;
        Log.d("MCP", "container.id = " + container.getId());
        if (status.fragment == null) {
            // Create the SearchResultFragment
            status.fragment = new SearchResultFragment();
            Log.d("MCP", "AD.FM = " + fm);
            fm.beginTransaction().add((int) unicode, status.fragment).commit();
            fm.executePendingTransactions();
                // [WTF] It took me 2 hours to think of adding this statement!

            // Set up the data of the fragment
            // Note: this must be done on the UI thread, otherwise
            //   the code in FavoriteFragment.onListItemClick that measures
            //   the view's height won't work
            status.fragment.setListAdapter(new SearchResultCursorAdapter(
                context,
                R.layout.search_result_item,
                MCPDatabase.directSearch(unicode),
                false   // Do not show the favorite button
            ));
        }
        container.setVisibility(View.VISIBLE);
        Log.d("MCP", "finish expanding " + unicode);
    }

    public void collapseItem(char unicode) {
        Log.d("MCP", "start collapsing " + unicode);
        ItemStatus status = itemStatus.get(unicode);
        if (status == null) return;
        status.isExpanded = false;
        if (status.view == null) return;
        View container = status.getContainer();
        if (container == null) return;
        container.setVisibility(View.GONE);
        Log.d("MCP", "finish collapsing " + unicode);
    }

    public void collapseAll() {
        for (char unicode : itemStatus.keySet()) {
            if (!itemStatus.get(unicode).isExpanded) {
                collapseItem(unicode);
            }
        }
    }

    public void clearFragments() {
        FragmentTransaction ft = fm.beginTransaction();
        for (ItemStatus status : itemStatus.values()) {
            if (status.fragment == null) continue;
            ft.remove(status.fragment);
            status.fragment = null;
            Log.d("MCP", "clear fragment");
        }
        ft.commitAllowingStateLoss();
    }

    private class ItemStatus {
        public View view;
        public SearchResultFragment fragment;
        public boolean isExpanded;

        public ItemStatus(View view) {
            this.view = view;
            this.fragment = null;
            this.isExpanded = false;
        }

        public View getContainer() {
            return view.findViewWithTag("container");
        }
    }
}
