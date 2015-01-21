package maigosoft.mcpdict;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiRic.ui.widget.Boast;

public class FavoriteFragment extends ListFragmentWithMemory implements RefreshableFragment {

    private View selfView;
    private ListView listView;
    private TextView textTotal;
    private CursorAdapter adapter;

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
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        // TODO: display info about the clicked item
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
