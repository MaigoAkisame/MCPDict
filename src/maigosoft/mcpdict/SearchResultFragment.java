package maigosoft.mcpdict;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
//Use android.app.ListFragment for API level >= 11
import android.support.v4.widget.CursorAdapter;
// Use android.widget.CursorAdapter for API level >= 11
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

public class SearchResultFragment extends ListFragment {

    private static final int[] DICT_LINK_VIEW_IDS = {
        R.id.text_mc, R.id.text_pu, R.id.text_ct, R.id.text_kr, R.id.text_vn
    };
    private static final String[] DICT_LINK_BASES = {
        "http://ytenx.org/zim?kyonh=1&dzih=",                               // plus UTF-8 encoded string
        "http://www.zdic.net/sousuo/?q=",                                   // plus UTF-8 encoded string
        "http://humanum.arts.cuhk.edu.hk/Lexis/lexi-can/search.php?q=",     // plus Big5 encoded string
        "http://hanja.naver.com/hanja?q=",                                  // plus UTF-8 encoded string
        "http://hanviet.org/hv_timchu.php?unichar=",                        // plus UTF-8 encoded string
    };  // Bases of links to external dictionaries

    ListView listView;
    CursorAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.search_result_fragment, container, false);
        listView = (ListView) fragment.findViewById(android.R.id.list);

        // Set up a context menu for each item of the search result
        registerForContextMenu(listView);

        return fragment;
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        // Show context menu on short clicks, too
        list.showContextMenuForChild(view);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        // Find the Chinese character in the view being clicked
        ListView list = (ListView) view;
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        int position = info.position - list.getFirstVisiblePosition();
            // info.position is the position of the item in the entire list
            // but list.getChildAt() on the next line requires the position of the item in currently visible items
        View entry = list.getChildAt(position);
        TextView text = (TextView) entry.findViewById(R.id.text_hz);
        String s = text.getText().toString();
        // Generate links to external dictionaries
        String utf8 = null;
        String big5 = null;
        try {utf8 = URLEncoder.encode(s, "utf-8");} catch (UnsupportedEncodingException e) {}
        try {big5 = URLEncoder.encode(s, "big5");} catch (UnsupportedEncodingException e) {}
        if (big5.equals("%3F")) big5 = null;    // Unsupported character
        String[] linkArgs = {utf8, utf8, big5, utf8, utf8};
        // Invalidate links where no pronunciation is available
        for (int i = 0; i < DICT_LINK_VIEW_IDS.length; i++) {
            text = (TextView) entry.findViewById(DICT_LINK_VIEW_IDS[i]);
            if (text.getText().toString().equals(CustomCursorAdapter.Displayer.NULL_STRING)) {
                linkArgs[i] = null;
            }
        }

        // Inflate the menu
        getActivity().getMenuInflater().inflate(R.menu.dict_links, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            // Replace the placeholders in the menu items with the character to look up
            item.setTitle(item.getTitle().toString().replace("X", s));
            // Set the intent of each menu item so we don't have to implement onContextItemSelected
            if (linkArgs[i] != null) {
                item.setEnabled(true);
                item.setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(DICT_LINK_BASES[i] + linkArgs[i])));
            }
            else {
                item.setEnabled(false);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up the adapter
        adapter = new CustomCursorAdapter(getActivity(), R.layout.search_result_item, null);
        setListAdapter(adapter);
    }

    public void updateResults(Cursor data) {
        if (adapter == null) return;
        adapter.changeCursor(data);
        listView.setSelectionAfterHeaderView();     // Scroll to top
        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(getString(R.string.no_matches));
    }
}
