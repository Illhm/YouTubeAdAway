package ma.wanam.youtubeadaway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatusReceiver extends BroadcastReceiver {
    public static final String ACTION_STATUS = "ma.wanam.youtubeadaway.ACTION_STATUS";
    public static final String EXTRA_FEATURE = "FEATURE";
    public static final String EXTRA_STATUS = "STATUS";
    public static final String EXTRA_MESSAGE = "MESSAGE";
    public static final String LOG_FILE = "hook_logs.txt";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_STATUS.equals(intent.getAction())) {
            String feature = intent.getStringExtra(EXTRA_FEATURE);
            boolean status = intent.getBooleanExtra(EXTRA_STATUS, false);
            String message = intent.getStringExtra(EXTRA_MESSAGE);

            // Update SharedPreferences
            try {
                // Using world readable mode for consistency with existing code, though MODE_PRIVATE is safer for this specific file if only app reads it.
                // However, existing code uses MODE_WORLD_READABLE for preferences.
                // The receiver updates the app's internal preferences which the UI reads.
                // Since this runs in the module app's process, standard prefs are fine.
                SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
                if (feature != null) {
                    prefs.edit().putBoolean("status_" + feature, status).apply();
                }
            } catch (Exception e) {
                Log.e("StatusReceiver", "Error updating prefs", e);
            }

            // Write to log file
            if (message != null) {
                writeLog(context, message);
            }
        }
    }

    private void writeLog(Context context, String message) {
        File file = new File(context.getFilesDir(), LOG_FILE);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String logEntry = timestamp + ": " + message + "\n";

        try (FileOutputStream fos = new FileOutputStream(file, true);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(logEntry);
        } catch (IOException e) {
            Log.e("StatusReceiver", "Error writing log", e);
        }
    }
}
