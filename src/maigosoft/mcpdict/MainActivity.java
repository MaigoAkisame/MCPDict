package maigosoft.mcpdict;

import java.lang.reflect.Field;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
// Remove support.v4 for API level >= 11
import android.view.ViewConfiguration;

public class MainActivity extends ActivityWithOptionsMenu {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize the orthography and database modules on separate threads
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Orthography.initialize(getResources());
                return null;
            }
        }.execute();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                UserDatabase.initialize(MainActivity.this);
                MCPDatabase.initialize(MainActivity.this);
                return null;
            }
        }.execute();

        // Force displaying the overflow menu in the action bar
        // Reference: http://stackoverflow.com/a/11438245
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        }
        catch (Exception e) {
            // Ignore
        }

        // Set up activity layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Set up the tabs
        FragmentTabHost tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);
        @SuppressWarnings("rawtypes")
        Class[] fragmentClasses = {DictionaryFragment.class, FavoriteFragment.class};
        int[] titleIds = {R.string.tab_dictionary, R.string.tab_favorite};
        for (int i = 0; i < fragmentClasses.length; i++) {
            String title = getResources().getString(titleIds[i]);
            tabHost.addTab(
                tabHost.newTabSpec(String.valueOf(i)).setIndicator(title),
                fragmentClasses[i],
                null
            );
        }
    }
}
