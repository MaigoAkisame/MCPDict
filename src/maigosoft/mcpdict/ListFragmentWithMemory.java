// A list fragment that remembers its scroll position
// Reference: http://stackoverflow.com/a/3035521

package maigosoft.mcpdict;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

public class ListFragmentWithMemory extends ListFragment implements AbsListView.OnScrollListener {

    protected int index;
    protected int y;

    public ListFragmentWithMemory() {
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Load stored scroll position, and set up the OnScrollListener
        // This must be done after super.onActivityCreated has been executed
        ListView list = getListView();
        list.setSelectionFromTop(index, y);
        list.setOnScrollListener(this);
    }

    @Override
    public void onScroll(AbsListView list, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        index = firstVisibleItem;
        View v = list.getChildAt(0);
        y = (v == null) ? 0 : (v.getTop() - list.getPaddingTop());
    }

    @Override
    public void onScrollStateChanged(AbsListView list, int scrollState) {}
}
