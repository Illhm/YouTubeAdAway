package ma.wanam.youtubeadaway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("ma.wanam.youtubeadaway.ACTION_STATUS".equals(intent.getAction())) {
            String feature = intent.getStringExtra("feature");
            boolean status = intent.getBooleanExtra("status", false);
            String message = intent.getStringExtra("message");

            String key = null;
            if ("Video Ads".equals(feature)) key = "status_video_ads";
            else if ("Background Playback".equals(feature)) key = "status_bg_playback";
            else if ("Ad Cards".equals(feature)) key = "status_ad_cards";

            if (key != null) {
                SharedPreferences prefs = context.getSharedPreferences("status_prefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .putBoolean(key + "_state", status)
                        .putString(key + "_msg", message)
                        .apply();
            }
            appendLog(context, feature + " (" + (status ? "Success" : "Failed") + "): " + message);
        }
    }

    private void appendLog(Context context, String log) {
        try {
            File file = new File(context.getFilesDir(), "hook_logs.txt");
            FileWriter writer = new FileWriter(file, true);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            writer.append(sdf.format(new Date())).append(" - ").append(log).append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
