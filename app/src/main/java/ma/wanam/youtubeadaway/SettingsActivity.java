package ma.wanam.youtubeadaway;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;

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

        findViewById(R.id.btn_view_log).setOnClickListener(v -> showLogs());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusDashboard();
    }

    private void updateStatusDashboard() {
        if (prefs == null) return;

        updateFeatureStatus("hide_invideo_ads", "status_video_ads", R.id.icon_video_ads, R.id.text_video_ads);
        updateFeatureStatus("hide_ad_cards", "status_ad_cards", R.id.icon_ad_cards, R.id.text_ad_cards);
        updateFeatureStatus("enable_bg_playback", "status_bg_playback", R.id.icon_bg_playback, R.id.text_bg_playback);
    }

    private void updateFeatureStatus(String prefKey, String statusKey, int iconId, int textId) {
        boolean isEnabled;
        if ("hide_ad_cards".equals(prefKey)) {
            isEnabled = prefs.getBoolean(prefKey, false);
        } else {
            isEnabled = prefs.getBoolean(prefKey, true);
        }

        ImageView icon = findViewById(iconId);
        TextView text = findViewById(textId);

        if (!isEnabled) {
            icon.setImageResource(android.R.drawable.ic_menu_help);
            icon.setColorFilter(android.graphics.Color.GRAY);
            text.setText(R.string.status_disabled);
            text.setTextColor(android.graphics.Color.GRAY);
            return;
        }

        if (prefs.contains(statusKey)) {
            boolean isActive = prefs.getBoolean(statusKey, false);
            if (isActive) {
                icon.setImageResource(android.R.drawable.presence_online); // Green dot indicator usually
                if (icon.getDrawable() == null) {
                     // Fallback if presence_online not found/supported in some contexts, though standard android.
                     icon.setImageResource(android.R.drawable.checkbox_on_background);
                }
                icon.setColorFilter(android.graphics.Color.GREEN);
                text.setText(R.string.status_active);
                text.setTextColor(android.graphics.Color.GREEN);
            } else {
                icon.setImageResource(android.R.drawable.ic_delete);
                icon.setColorFilter(android.graphics.Color.RED);
                text.setText(R.string.status_error);
                text.setTextColor(android.graphics.Color.RED);
            }
        } else {
            icon.setImageResource(android.R.drawable.ic_popup_sync);
            icon.setColorFilter(android.graphics.Color.parseColor("#FFA500"));
            text.setText(R.string.status_unknown);
            text.setTextColor(android.graphics.Color.parseColor("#FFA500"));
        }
    }

    private void showLogs() {
        File file = new File(getFilesDir(), StatusReceiver.LOG_FILE);
        StringBuilder content = new StringBuilder();
        if (file.exists()) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    content.append(line).append("\n");
                }
            } catch (Exception e) {
                content.append("Error reading log: ").append(e.getMessage());
            }
        } else {
            content.append(getString(R.string.log_empty));
        }

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(content.toString());
        textView.setPadding(32, 32, 32, 32);
        scrollView.addView(textView);

        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.log_viewer_title)
                .setView(scrollView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
        }
    }
}