package maigosoft.mcpdict;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

public class MainActivity extends ActivityWithOptionsMenu {

    private MCPDatabase db;
    private CustomSearchView searchView;
    private Spinner spinnerSearchAs;
    private CheckBox checkBoxKuangxYonhOnly;
    private CheckBox checkBoxAllowVariants;
    private CheckBox checkBoxToneInsensitive;
    private SearchResultFragment fragmentResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize the orthography module on a separate thread
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Orthography.initialize(getResources());
                return null;
            }
        }.execute();

        // Set up activity layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Build the database
        db = new MCPDatabase(this);

        // Set up the search view
        searchView = (CustomSearchView) findViewById(R.id.search_view);
        searchView.setHint(getResources().getString(R.string.search_hint));
        searchView.setSearchButtonOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                search();
            }
        });

        // Set up the spinner
        spinnerSearchAs = (Spinner) findViewById(R.id.spinner_search_as);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.search_as, android.R.layout.simple_spinner_item);
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
        checkBoxKuangxYonhOnly = (CheckBox) findViewById(R.id.check_box_kuangx_yonh_only);
        checkBoxAllowVariants = (CheckBox) findViewById(R.id.check_box_allow_variants);
        checkBoxToneInsensitive = (CheckBox) findViewById(R.id.check_box_tone_insensitive);
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

        // Get a reference to the search result fragment
        // Use getFragmentManager() for API level >= 11
        fragmentResult = (SearchResultFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_search_result);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        search();
    }

    private void loadCheckBoxes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Resources r = getResources();
        checkBoxKuangxYonhOnly.setChecked(sp.getBoolean(r.getString(R.string.pref_key_kuangx_yonh_only), false));
        checkBoxAllowVariants.setChecked(sp.getBoolean(r.getString(R.string.pref_key_allow_variants), true));
        checkBoxToneInsensitive.setChecked(sp.getBoolean(r.getString(R.string.pref_key_tone_insensitive), false));
    }

    private void saveCheckBoxes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Resources r = getResources();
        sp.edit().putBoolean(r.getString(R.string.pref_key_kuangx_yonh_only), checkBoxKuangxYonhOnly.isChecked())
                 .putBoolean(r.getString(R.string.pref_key_allow_variants), checkBoxAllowVariants.isChecked())
                 .putBoolean(r.getString(R.string.pref_key_tone_insensitive), checkBoxToneInsensitive.isChecked())
                 .commit();     // Use apply() for API level >= 9
    }

    private void updateCheckBoxesEnabled() {
        int mode = spinnerSearchAs.getSelectedItemPosition();
        checkBoxKuangxYonhOnly.setEnabled(mode != MCPDatabase.SEARCH_AS_MC);
        checkBoxAllowVariants.setEnabled(mode == MCPDatabase.SEARCH_AS_HZ);
        checkBoxToneInsensitive.setEnabled(mode == MCPDatabase.SEARCH_AS_MC ||
                                           mode == MCPDatabase.SEARCH_AS_PU ||
                                           mode == MCPDatabase.SEARCH_AS_CT ||
                                           mode == MCPDatabase.SEARCH_AS_VN);
    }

    private void search() {
        final String query = searchView.getQuery();
        if (query.equals("")) return;
        final int pos = spinnerSearchAs.getSelectedItemPosition();
        if (pos == Spinner.INVALID_POSITION) return;
        // Search on a separate thread
        // Because AsyncTasks are put in a queue,
        //   this will not run until the initialization of the orthography module finishes
        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected Cursor doInBackground(Void... params) {
                return db.search(query, pos);
            }
            @Override
            protected void onPostExecute(Cursor data) {
                fragmentResult.updateResults(data);
            }
        }.execute();
    }
}
