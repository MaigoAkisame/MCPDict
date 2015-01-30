package maigosoft.mcpdict;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

@SuppressLint("UseSparseArrays")
public class FavoriteFragment extends ListFragmentWithMemory implements RefreshableFragment {

    private View selfView;
    private View header;
    private TextView textTotal;
    private Button buttonClear;
    private ListView listView;
    private FavoriteCursorAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // A hack to avoid nested fragments from being inflated twice
        // Reference: http://stackoverflow.com/a/14695397
        if (selfView != null) {
            ViewGroup parent = (ViewGroup) selfView.getParent();
            if (parent != null) parent.removeView(selfView);
            return selfView;
        }

        // Inflate the fragment view
        selfView = inflater.inflate(R.layout.favorite_fragment, container, false);

        // Get references to some child views
        header = selfView.findViewById(R.id.favorite_header);
        textTotal = (TextView) selfView.findViewById(R.id.text_total);
        listView = (ListView) selfView.findViewById(android.R.id.list);

        // Set up the "clear all" button
        buttonClear = (Button) selfView.findViewById(R.id.button_clear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FavoriteDialogs.deleteAll();
            }
        });

        return selfView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up the adapter
        if (adapter == null) {
            adapter = new FavoriteCursorAdapter(getActivity(), R.layout.favorite_item, null);
            setListAdapter(adapter);
        }

        Log.d("MCP", "FM = " + getFragmentManager());
        adapter.setFragmentManager(getFragmentManager());
    }

    @Override
    public void onListItemClick(final ListView list, final View view, final int position, long id) {
        // When a favorite item is clicked, display a SearchResultFragment below it
        //   to show the details about the character in the item, or hide the
        //   SearchResultFragment if it is already displayed

        // Find the Chinese character in the view being clicked
        TextView text = (TextView) view.findViewById(R.id.text_hz);
        String hanzi = text.getText().toString();
        final char unicode = hanzi.charAt(0);

        if (adapter.isItemExpanded(unicode)) {
            adapter.collapseItem(unicode);
        }
        else {
            adapter.expandItem(unicode);
        }

        // If the current favorite item (including the SearchResultFragment)
        //   is not entirely visible, try to scroll the ListView to make it visible
        // This must be put in post(), in order to be executed after the
        //   dimensions of the current item are known
        listView.post(new Runnable() {
            @Override
            public void run() {
                int top = view.getTop();
                int bottom = view.getBottom();
                int listBottom = list.getHeight() - list.getPaddingBottom();
                int listTop = list.getPaddingTop();
                final int y = (top < listTop) ? listTop : (bottom < listBottom) ? top : (listBottom - bottom + top);
                listView.setSelectionFromTop(position, y);
            }
        });
    }

    @Override
    public void refresh(final boolean scrollToTop) {
        if (adapter == null) return;
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... params) {
                return UserDatabase.selectAllFavorites();
            }
            @Override
            protected void onPostExecute(Cursor data) {
                adapter.changeCursor(data);
                if (scrollToTop) listView.setSelectionAfterHeaderView();
                if (data.getCount() == 0) {
                    header.setVisibility(View.GONE);
                }
                else {
                    header.setVisibility(View.VISIBLE);
                    textTotal.setText(String.format(getString(R.string.favorite_total), data.getCount()));
                }
            }
        }.execute();
    }
}
