package maigosoft.mcpdict;

import java.lang.reflect.Field;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
        int apiLevel = android.os.Build.VERSION.SDK_INT;
        for (int i = 0; i < nTabs; i++) {
            View tab = tabHost.getTabWidget().getChildAt(i);
            TextView textView = (TextView) tab.findViewById(android.R.id.title);
            // Set tab text size, for all Android versions
            textView.setTextSize(17);
            if (apiLevel < 11) {                            // 1.x and 2.x
                // The only problem with the default style is that the tabs are too tall;
                //   we set it to 1.5 times the text height
                textView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
                tab.getLayoutParams().height = (int) (textView.getMeasuredHeight() * 1.5);
            }
            else if (apiLevel >= 11 && apiLevel < 14) {     // 3.x
                // Lots of problems with the default style:
                //   * Text is white (selected tab) or gray (unselected tab)
                //   * Text is not centered in tabs
                //   * Tabs do not fill the screen width
                //   * Tabs are a little too tall
                // We need to fix them one by one
                textView.setTextColor(Color.BLACK);
                ((RelativeLayout) tab).setHorizontalGravity(Gravity.CENTER);
                textView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
                int height = (int) (textView.getMeasuredHeight() * 1.8);
                tab.setLayoutParams(new LinearLayout.LayoutParams(0, height, 1.0f));
            }
        }
    }

    public RefreshableFragment getCurrentFragment() {
        return (RefreshableFragment) fm.findFragmentByTag(tabHost.getCurrentTabTag());
    }

    public FavoriteFragment getFavoriteFragment() {
        return (FavoriteFragment) fm.findFragmentByTag(getString(R.string.tab_favorite));
    }
}
