package maigosoft.mcpdict;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
// Use android.app.ListFragment for API level >= 11
import android.support.v4.widget.CursorAdapter;
import android.text.ClipboardManager;
// Use android.widget.CursorAdapter for API level >= 11
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseSparseArrays")
@SuppressWarnings("deprecation")
public class SearchResultFragment extends ListFragment implements Masks {

    private static final Map<Integer, Integer> COPY_MENU_ITEM_TO_MASK = new HashMap<Integer, Integer>();
    static {
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_all,      MASK_ALL);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_mc,       MASK_MC);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_pu,       MASK_PU);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_ct,       MASK_CT);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_kr,       MASK_KR);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_vn,       MASK_VN);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_jp_all,   MASK_JP_ALL);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_jp_go,    MASK_JP_GO);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_jp_kan,   MASK_JP_KAN);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_jp_tou,   MASK_JP_TOU);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_jp_kwan,  MASK_JP_KWAN);
        COPY_MENU_ITEM_TO_MASK.put(R.id.menu_item_copy_jp_other, MASK_JP_OTHER);
    }

    private static final int[] DICT_LINK_MASKS = {
        MASK_MC, MASK_PU, MASK_CT, MASK_KR, MASK_VN
    };

    private static final String[] DICT_LINK_BASES = {
        "http://ytenx.org/zim?kyonh=1&dzih=",                               // plus UTF-8 encoded string
        "http://www.zdic.net/sousuo/?q=",                                   // plus UTF-8 encoded string
        "http://humanum.arts.cuhk.edu.hk/Lexis/lexi-can/search.php?q=",     // plus Big5 encoded string
        "http://hanja.naver.com/hanja?q=",                                  // plus UTF-8 encoded string
        "http://hanviet.org/hv_timchu.php?unichar=",                        // plus UTF-8 encoded string
    };  // Bases of links to external dictionaries

    private ListView listView;
    private CursorAdapter adapter;

    private View selectedEntry = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.search_result_fragment, container, false);
        listView = (ListView) fragment.findViewById(android.R.id.list);

        // Set up a context menu for each item of the search result
        registerForContextMenu(listView);

        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up the adapter
        adapter = new CustomCursorAdapter(getActivity(), R.layout.search_result_item, null);
        setListAdapter(adapter);
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
        selectedEntry = list.getChildAt(position);
        TextView text = (TextView) selectedEntry.findViewById(R.id.text_hz);
        String hanzi = text.getText().toString();
        int tag = (Integer) selectedEntry.getTag();

        // Inflate the context menu
        getActivity().getMenuInflater().inflate(R.menu.search_result_context_menu, menu);
        SubMenu menuCopy = menu.getItem(0).getSubMenu();
        SubMenu menuDictLinks = menu.getItem(1).getSubMenu();

        // Determine whether to retain each item in the "copy" sub-menu
        for (int i = menuCopy.size() - 1; i >= 0; i--) {
            int id = menuCopy.getItem(i).getItemId();
            int mask = COPY_MENU_ITEM_TO_MASK.get(id);
            if ((tag & mask) == 0) menuCopy.removeItem(id);
        }

        // Determine whether to enable each item in the sub-menu of external dictionaries,
        // and generate links for enabled items
        String utf8 = null;
        String big5 = null;
        try {utf8 = URLEncoder.encode(hanzi, "utf-8");} catch (UnsupportedEncodingException e) {}
        try {big5 = URLEncoder.encode(hanzi, "big5");} catch (UnsupportedEncodingException e) {}
        if (big5.equals("%3F")) big5 = null;    // Unsupported character
        String[] linkArgs = {utf8, utf8, big5, utf8, utf8};
        for (int i = 0; i < menuDictLinks.size(); i++) {
            MenuItem item = menuDictLinks.getItem(i);
            if ((tag & DICT_LINK_MASKS[i]) != 0) {
                item.setEnabled(true);
                item.setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(DICT_LINK_BASES[i] + linkArgs[i])));
            }
            else {
                item.setEnabled(false);
            }
        }

        // Replace the placeholders in the menu items with the character selected
        for (Menu m : new Menu[] {menu, menuCopy, menuDictLinks}) {
            for (int i = 0; i < m.size(); i++) {
                MenuItem item = m.getItem(i);
                item.setTitle(item.getTitle().toString().replace("X", hanzi));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (COPY_MENU_ITEM_TO_MASK.containsKey(item.getItemId())) {
            // Generate the text to copy to the clipboard
            String text = getReading(selectedEntry, COPY_MENU_ITEM_TO_MASK.get(item.getItemId()));
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
            String label = item.getTitle().toString().substring(2);    // this is ugly
            Resources r = getActivity().getResources();
            String message = r.getString(R.string.copy_done).replace("X", label);
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            return true;
        }
        else {
            // Fall back to default behavior
            return false;
        }
    }

    private String getReading(View entry, int mask) {
        int tag = (Integer) entry.getTag();
        if ((tag & mask) == 0) return null;

        TextView[] textViewJPExtras = {
            (TextView) entry.findViewById(R.id.text_jp_extra_1),
            (TextView) entry.findViewById(R.id.text_jp_extra_2),
            (TextView) entry.findViewById(R.id.text_jp_extra_3)
        };
        StringBuilder sb;

        switch (mask) {
        case MASK_MC:
            String[] readings = ((TextView) entry.findViewById(R.id.text_mc)).getText().toString().split("\n");
            String[] details = ((TextView) entry.findViewById(R.id.text_mc_detail)).getText().toString().split("\n");
            String text = "";
            for (int i = 0; i < readings.length; i++) {
                if (i > 0) text += "\n";
                text += readings[i] + details[i];
            }
            return text;
        case MASK_PU:
            return ((TextView) entry.findViewById(R.id.text_pu)).getText().toString();
        case MASK_CT:
            return ((TextView) entry.findViewById(R.id.text_ct)).getText().toString();
        case MASK_KR:
            return ((TextView) entry.findViewById(R.id.text_kr)).getText().toString();
        case MASK_VN:
            return ((TextView) entry.findViewById(R.id.text_vn)).getText().toString();
        case MASK_JP_GO:
            return ((TextView) entry.findViewById(R.id.text_jp_go)).getText().toString();
        case MASK_JP_KAN:
            return ((TextView) entry.findViewById(R.id.text_jp_kan)).getText().toString();
        case MASK_JP_TOU:
            return textViewJPExtras[0].getText().toString();
        case MASK_JP_KWAN:
            return textViewJPExtras[(tag & MASK_JP_TOU) > 0 ? 1 : 0].getText().toString();
        case MASK_JP_OTHER:
            return textViewJPExtras[((tag & MASK_JP_TOU) > 0 ? 1 : 0) +
                                    ((tag & MASK_JP_KWAN) > 0 ? 1 : 0)].getText().toString();
        case MASK_JP_ALL:
            sb = new StringBuilder();
            if ((tag & MASK_JP_GO) > 0)    sb.append(formatReading("吳", getReading(entry, MASK_JP_GO)));
            if ((tag & MASK_JP_KAN) > 0)   sb.append(formatReading("漢", getReading(entry, MASK_JP_KAN)));
            if ((tag & MASK_JP_TOU) > 0)   sb.append(formatReading("唐", getReading(entry, MASK_JP_TOU)));
            if ((tag & MASK_JP_KWAN) > 0)  sb.append(formatReading("慣", getReading(entry, MASK_JP_KWAN)));
            if ((tag & MASK_JP_OTHER) > 0) sb.append(formatReading("他", getReading(entry, MASK_JP_OTHER)));
            return sb.toString();
        case MASK_ALL:
            sb = new StringBuilder();
            String hanzi = ((TextView) entry.findViewById(R.id.text_hz)).getText().toString();
            String unicode = ((TextView) entry.findViewById(R.id.text_unicode)).getText().toString();
            sb.append(hanzi + " " + unicode + "\n");
            if ((tag & MASK_MC) > 0)       sb.append(formatReading("中古",  getReading(entry, MASK_MC)));
            if ((tag & MASK_PU) > 0)       sb.append(formatReading("普",   getReading(entry, MASK_PU)));
            if ((tag & MASK_CT) > 0)       sb.append(formatReading("粵",   getReading(entry, MASK_CT)));
            if ((tag & MASK_KR) > 0)       sb.append(formatReading("朝",   getReading(entry, MASK_KR)));
            if ((tag & MASK_VN) > 0)       sb.append(formatReading("越",   getReading(entry, MASK_VN)));
            if ((tag & MASK_JP_GO) > 0)    sb.append(formatReading("日·吳", getReading(entry, MASK_JP_GO)));
            if ((tag & MASK_JP_KAN) > 0)   sb.append(formatReading("日·漢", getReading(entry, MASK_JP_KAN)));
            if ((tag & MASK_JP_TOU) > 0)   sb.append(formatReading("日·唐", getReading(entry, MASK_JP_TOU)));
            if ((tag & MASK_JP_KWAN) > 0)  sb.append(formatReading("日·慣", getReading(entry, MASK_JP_KWAN)));
            if ((tag & MASK_JP_OTHER) > 0) sb.append(formatReading("日·他", getReading(entry, MASK_JP_OTHER)));
            return sb.toString();
        }
        return null;
    }

    private String formatReading(String prefix, String reading) {
        String separator = reading.contains("\n") ? "\n" : " ";
        return "[" + prefix + "]" + separator + reading + "\n";
    }

    public void updateResults(Cursor data) {
        if (adapter == null) return;
        adapter.changeCursor(data);
        listView.setSelectionAfterHeaderView();     // Scroll to top
        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(getString(R.string.no_matches));
    }
}
