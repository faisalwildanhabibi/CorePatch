package org.lsposed.corepatch.zygisk;

import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    public static final String TAG = "ZygiskCore";
    // Log strictly into the module's own folder in Magisk/KernelSU/ReSukiSu
    private static final String MODULE_LOG_PATH = "/data/adb/modules/org_lsposed_corepatch_zygisk/zygisk_core.log";

    public static void i(String msg) {
        Log.i(TAG, msg);
        logToFile("[INFO] " + msg);
    }

    public static void d(String msg) {
        Log.d(TAG, msg);
        logToFile("[DEBUG] " + msg);
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
        logToFile("[WARN] " + msg);
    }

    public static void e(String msg, Throwable th) {
        Log.e(TAG, msg, th);
        StringBuilder sb = new StringBuilder();
        sb.append("[ERROR] ").append(msg).append("\n");
        if (th != null) {
            sb.append(Log.getStackTraceString(th)).append("\n");
        }
        logToFile(sb.toString());
    }

    private static synchronized void logToFile(String line) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        String entry = timestamp + " [" + android.os.Process.myPid() + "] " + line + "\n";
        byte[] bytes = entry.getBytes(StandardCharsets.UTF_8);

        try {
            File f = new File(MODULE_LOG_PATH);
            File parent = f.getParentFile();
            if (parent != null && parent.exists()) {
                try (FileOutputStream fos = new FileOutputStream(f, true)) {
                    fos.write(bytes);
                    fos.flush();
                }
                f.setReadable(true, false);
                f.setWritable(true, false);
            }
        } catch (Throwable ignored) {
        }
    }
}
