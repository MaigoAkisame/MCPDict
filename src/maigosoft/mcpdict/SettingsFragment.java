package maigosoft.mcpdict;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;
// This requires adding the following project as a library project:
//   https://github.com/kolavar/android-support-v4-preferencefragment
// Use android.preference.PreferenceFragment for API level >= 11

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
