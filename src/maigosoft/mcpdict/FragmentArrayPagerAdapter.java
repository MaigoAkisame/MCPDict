package maigosoft.mcpdict;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
// Use android.app.* for API level >= 11

public class FragmentArrayPagerAdapter extends FragmentPagerAdapter {

    private Fragment[] fragments;
    private String[] titles;

    public FragmentArrayPagerAdapter(FragmentManager fm, Fragment[] fragments, String[] titles) {
        super(fm);
        this.fragments = fragments;
        this.titles = titles;
    }

    @Override
    public int getCount() {return fragments.length;}

    @Override
    public Fragment getItem(int i) {return fragments[i];}

    @Override
    public String getPageTitle(int i) {return titles[i];}
}
