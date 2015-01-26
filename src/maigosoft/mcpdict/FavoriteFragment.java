package maigosoft.mcpdict;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiRic.ui.widget.Boast;

@SuppressLint("UseSparseArrays")
public class FavoriteFragment extends ListFragmentWithMemory implements RefreshableFragment {

    private View selfView;
    private ListView listView;
    private TextView textTotal;
    private CursorAdapter adapter;
    private FragmentManager fm;
    private Map<Character, Fragment> subFragments = new HashMap<Character, Fragment>();
    private Map<Character, View> containers = new HashMap<Character, View>();

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
        listView = (ListView) selfView.findViewById(android.R.id.list);
        textTotal = (TextView) selfView.findViewById(R.id.text_total);

        // Set up the "clear all" button
        Button button = (Button) selfView.findViewById(R.id.button_clear);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_alert)
                    .setTitle(getString(R.string.favorite_clear))
                    .setMessage(getString(R.string.favorite_clear_confirm))
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            UserDatabase.deleteAllFavorites();
                            refresh(true);
                            String message = getString(R.string.favorite_clear_done);
                            Boast.showText(getActivity(), message, Toast.LENGTH_SHORT);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });

        return selfView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up the adapter
        if (adapter == null) {
            adapter = new FavoriteCursorAdapter(getActivity(), this, R.layout.favorite_item, null);
            setListAdapter(adapter);
        }

        // Get a reference to the FragmentManager
        fm = getFragmentManager();
        removeSubFragments();   // Called after tab switch
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

        View container = view.findViewWithTag("container");
        SearchResultFragment fragment = (SearchResultFragment) subFragments.get(unicode);
        if (fragment == null) {
            // Create the SearchResultFragment
            fragment = new SearchResultFragment();
            subFragments.put(unicode, fragment);
            containers.put(unicode, container);
            fm.beginTransaction().add((int) unicode, fragment).commit();
            fm.executePendingTransactions();
                // [WTF] It took me 2 hours to think of adding this statement!

            // Set up the data of the fragment
            // Note: this must be done on the UI thread, otherwise
            //   the code below that measures the view's height won't work
            fragment.setListAdapter(new SearchResultCursorAdapter(
                getActivity(),
                R.layout.search_result_item,
                MCPDatabase.directSearch(unicode),
                false   // Do not show the favorite button
            ));
        }

        // Toggle the visibility of the container
        int vis = container.getVisibility();
        vis = vis == View.GONE ? View.VISIBLE : View.GONE;
        container.setVisibility(vis);

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

    // Clean up the sub-fragments
    public void removeSubFragments() {
        FragmentTransaction ft = fm.beginTransaction();
        for (Fragment fragment : subFragments.values()) {
            ft.remove(fragment);
        }
        for (View container : containers.values()) {
            container.setVisibility(View.GONE);     // Hide the container as well
        }
        ft.commitAllowingStateLoss();
        subFragments.clear();
        containers.clear();
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
                textTotal.setText(getString(R.string.favorite_total).replace("X", String.valueOf(data.getCount())));
            }
        }.execute();
    }
}
