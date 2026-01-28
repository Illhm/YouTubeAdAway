package ma.wanam.youtubeadaway;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static SharedPreferences prefs;

    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        try {
            prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            Log.e(getPackageName(), ignored.toString());
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // copy btc wallet address to the clipboard
            getPreferenceScreen().findPreference("donate_btc")
                    .setOnPreferenceClickListener(preference -> {
                        ClipboardManager clipboardManager = (ClipboardManager)
                                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText(preference.getKey(), preference.getSummary());
                        clipboardManager.setPrimaryClip(clipData);

                        return true;
                    });

            // set module status
            Preference statusPreference = getPreferenceScreen().findPreference("status");
            statusPreference.setIcon(XChecker.isEnabled() ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert);
            statusPreference.setSummary(XChecker.isEnabled() ? R.string.module_active : R.string.module_inactive);

            // View Logs
            findPreference("view_logs").setOnPreferenceClickListener(preference -> {
                File file = new File(getContext().getFilesDir(), "hook_logs.txt");
                StringBuilder content = new StringBuilder();
                if (file.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        content.append("Error reading logs: ").append(e.getMessage());
                    }
                } else {
                    content.append("No logs found.");
                }

                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.logs_dialog_title)
                        .setMessage(content.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return true;
            });
        }

        private SharedPreferences statusPrefs;
        private final SharedPreferences.OnSharedPreferenceChangeListener statusListener = (sharedPreferences, key) -> updateStatuses();

        private void updateStatuses() {
            updateStatus("status_video_ads");
            updateStatus("status_bg_playback");
            updateStatus("status_ad_cards");
        }

        private void updateStatus(String key) {
            Preference pref = findPreference(key);
            if (pref != null) {
                if (!statusPrefs.contains(key + "_state")) {
                    pref.setIcon(android.R.drawable.ic_popup_sync);
                    pref.setSummary("Waiting...");
                } else {
                    boolean state = statusPrefs.getBoolean(key + "_state", false);
                    String msg = statusPrefs.getString(key + "_msg", "Waiting...");
                    pref.setIcon(state ? android.R.drawable.presence_online : android.R.drawable.presence_busy);
                    pref.setSummary(msg);
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (getContext() != null) {
                statusPrefs = getContext().getSharedPreferences("status_prefs", Context.MODE_PRIVATE);
                statusPrefs.registerOnSharedPreferenceChangeListener(statusListener);
                updateStatuses();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if (statusPrefs != null) {
                statusPrefs.unregisterOnSharedPreferenceChangeListener(statusListener);
            }
        }
    }
}