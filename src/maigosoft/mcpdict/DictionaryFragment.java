package maigosoft.mcpdict;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

public class DictionaryFragment extends Fragment implements RefreshableFragment {

    private View selfView;
    private CustomSearchView searchView;
    private Spinner spinnerSearchAs;
    private CheckBox checkBoxKuangxYonhOnly;
    private CheckBox checkBoxAllowVariants;
    private CheckBox checkBoxToneInsensitive;
    private SearchResultFragment fragmentResult;

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
        selfView = inflater.inflate(R.layout.dictionary_fragment, container, false);

        // Set up the search view
        searchView = (CustomSearchView) selfView.findViewById(R.id.search_view);
        searchView.setHint(getResources().getString(R.string.search_hint));
        searchView.setSearchButtonOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                refresh();
                fragmentResult.scrollToTop();
            }
        });

        // Set up the spinner
        spinnerSearchAs = (Spinner) selfView.findViewById(R.id.spinner_search_as);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            getActivity(), R.array.search_as, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerSearchAs.setAdapter(adapter);
        spinnerSearchAs.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCheckBoxesEnabled();
                searchView.clickSearchButton();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set up the checkboxes
        checkBoxKuangxYonhOnly = (CheckBox) selfView.findViewById(R.id.check_box_kuangx_yonh_only);
        checkBoxAllowVariants = (CheckBox) selfView.findViewById(R.id.check_box_allow_variants);
        checkBoxToneInsensitive = (CheckBox) selfView.findViewById(R.id.check_box_tone_insensitive);
        loadCheckBoxes();
        updateCheckBoxesEnabled();
        CompoundButton.OnCheckedChangeListener checkBoxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                saveCheckBoxes();
                searchView.clickSearchButton();
            }
        };
        checkBoxKuangxYonhOnly.setOnCheckedChangeListener(checkBoxListener);
        checkBoxAllowVariants.setOnCheckedChangeListener(checkBoxListener);
        checkBoxToneInsensitive.setOnCheckedChangeListener(checkBoxListener);

        // Get a reference to the SearchResultFragment
        fragmentResult = (SearchResultFragment) getFragmentManager().findFragmentById(R.id.fragment_search_result);

        return selfView;
    }

    private void loadCheckBoxes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Resources r = getResources();
        checkBoxKuangxYonhOnly.setChecked(sp.getBoolean(r.getString(R.string.pref_key_kuangx_yonh_only), false));
        checkBoxAllowVariants.setChecked(sp.getBoolean(r.getString(R.string.pref_key_allow_variants), true));
        checkBoxToneInsensitive.setChecked(sp.getBoolean(r.getString(R.string.pref_key_tone_insensitive), false));
    }

    private void saveCheckBoxes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Resources r = getResources();
        sp.edit().putBoolean(r.getString(R.string.pref_key_kuangx_yonh_only), checkBoxKuangxYonhOnly.isChecked())
                 .putBoolean(r.getString(R.string.pref_key_allow_variants), checkBoxAllowVariants.isChecked())
                 .putBoolean(r.getString(R.string.pref_key_tone_insensitive), checkBoxToneInsensitive.isChecked())
                 .apply();
    }

    private void updateCheckBoxesEnabled() {
        int mode = spinnerSearchAs.getSelectedItemPosition();
        checkBoxKuangxYonhOnly.setEnabled(mode != MCPDatabase.SEARCH_AS_MC);
        checkBoxAllowVariants.setEnabled(mode == MCPDatabase.SEARCH_AS_HZ);
        checkBoxToneInsensitive.setEnabled(mode == MCPDatabase.SEARCH_AS_MC ||
                                           mode == MCPDatabase.SEARCH_AS_PU ||
                                           mode == MCPDatabase.SEARCH_AS_CT ||
                                           mode == MCPDatabase.SEARCH_AS_SH ||
                                           mode == MCPDatabase.SEARCH_AS_MN ||
                                           mode == MCPDatabase.SEARCH_AS_VN);
    }

    @Override
    public void refresh() {
        // Search on a separate thread
        // Because AsyncTasks are put in a queue,
        //   this will not run until the initialization of the orthography module finishes
        final String query = searchView.getQuery();
        final int mode = spinnerSearchAs.getSelectedItemPosition();
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... params) {
                return MCPDatabase.search(query, mode);
            }
            @Override
            protected void onPostExecute(Cursor data) {
                fragmentResult.setData(data);
                TextView textEmpty = (TextView) fragmentResult.getView().findViewById(android.R.id.empty);
                if (query.trim().equals("")) {
                    textEmpty.setText("");
                }
                else {
                    textEmpty.setText(R.string.no_matches);
                }
            }
        }.execute();
    }
}
