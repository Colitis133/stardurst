package com.stardust;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            Preference logoutPref = findPreference("logout");
            logoutPref.setOnPreferenceClickListener(preference -> {
                // Send broadcast to trigger logout
                Intent intent = new Intent("stardust-control-event");
                intent.putExtra("command", "logout");
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                return true;
            });

            Preference logsPref = findPreference("view_logs");
            logsPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), LogViewerActivity.class));
                return true;
            });

            SwitchPreference bubblePref = (SwitchPreference) findPreference("bubble_toggle");
            bubblePref.setOnPreferenceChangeListener((preference, newValue) -> {
                Intent intent = new Intent(getActivity(), BubbleService.class);
                if ((boolean) newValue) {
                    getActivity().startService(intent);
                } else {
                    getActivity().stopService(intent);
                }
                return true;
            });
        }
    }
}
