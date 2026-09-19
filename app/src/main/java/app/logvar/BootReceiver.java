package app.logvar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.io.File;

public final class BootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
    android.content.SharedPreferences settings = context.getSharedPreferences("logvar", Context.MODE_PRIVATE);
    if (settings.getBoolean("boot", false) && settings.getBoolean("foreground", true)
        && new File(context.getFilesDir(), "nodejs-project/main.cjs").isFile()) {
      context.startForegroundService(new Intent(context, ServerService.class));
    }
  }
}
