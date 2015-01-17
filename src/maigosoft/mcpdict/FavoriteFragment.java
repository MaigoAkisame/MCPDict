package maigosoft.mcpdict;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FavoriteFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set up the view of the entire dictionary fragment
        View fragment = inflater.inflate(R.layout.favorite_fragment, container, false);

        return fragment;
    }

}
