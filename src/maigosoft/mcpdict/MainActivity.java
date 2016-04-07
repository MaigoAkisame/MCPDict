package maigosoft.mcpdict;

import java.lang.reflect.Field;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TabHost;
import android.widget.TextView;

public class MainActivity extends ActivityWithOptionsMenu {

    private FragmentManager fm;
    private FragmentTabHost tabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize the some "static" classes on separate threads
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

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                FavoriteDialogs.initialize(MainActivity.this);
                return null;
            }
        }.execute();

        // Force displaying the overflow menu in the action bar
        // Reference: http://stackoverflow.com/a/11438245
        // Only works for Android 4.x
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
        tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        fm = getSupportFragmentManager();
        tabHost.setup(this, fm, android.R.id.tabcontent);
        @SuppressWarnings("rawtypes")
        Class[] fragmentClasses = {DictionaryFragment.class, FavoriteFragment.class};
        int[] titleIds = {R.string.tab_dictionary, R.string.tab_favorite};
        int nTabs = fragmentClasses.length;
        for (int i = 0; i < nTabs; i++) {
            String title = getString(titleIds[i]);
            tabHost.addTab(
                tabHost.newTabSpec(title).setIndicator(title),
                fragmentClasses[i],
                null
            );
        }
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                fm.executePendingTransactions();
                getCurrentFragment().refresh();
            }
        });

        // Styling of the tabs has to go here; XML doesn't work
        for (int i = 0; i < nTabs; i++) {
            View tab = tabHost.getTabWidget().getChildAt(i);
            TextView textView = (TextView) tab.findViewById(android.R.id.title);
            textView.setTextSize(17);
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        // Make settings take effect immediately as the user navigates back to the dictionary
        DictionaryFragment fragment = getDictionaryFragment();
        if (fragment != null) {
            fragment.refresh();
        }
    }

    public RefreshableFragment getCurrentFragment() {
        return (RefreshableFragment) fm.findFragmentByTag(tabHost.getCurrentTabTag());
    }

    public DictionaryFragment getDictionaryFragment() {
        return (DictionaryFragment) fm.findFragmentByTag(getString(R.string.tab_dictionary));
    }

    public FavoriteFragment getFavoriteFragment() {
        return (FavoriteFragment) fm.findFragmentByTag(getString(R.string.tab_favorite));
    }
}
