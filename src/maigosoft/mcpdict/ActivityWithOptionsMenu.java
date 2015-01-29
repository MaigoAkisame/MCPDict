package maigosoft.mcpdict;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ActivityWithOptionsMenu extends FragmentActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.menu_item_settings:
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        case R.id.menu_item_help:
            intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            return true;
        case R.id.menu_item_about:
            Dialog dialog = new AlertDialog.Builder(this)
                                .setTitle(R.string.about)
                                .setMessage(R.string.about_message)
                                .setPositiveButton(R.string.ok, null)
                                .show();
            TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
            messageText.setGravity(Gravity.CENTER);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
