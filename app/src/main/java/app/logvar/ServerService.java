package app.logvar;

import android.app.*;
import android.content.Intent;
import android.os.IBinder;
import java.io.File;

/** Owns the native runtime so stopping the service also releases its heap. */
public final class ServerService extends Service {
  @Override public void onCreate() {
    super.onCreate();
    NotificationManager manager = getSystemService(NotificationManager.class);
    manager.createNotificationChannel(new NotificationChannel("server", "弹幕服务", NotificationManager.IMPORTANCE_LOW));
    PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    startForeground(1, new Notification.Builder(this, "server")
        .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
        .setContentTitle("Logvar 弹幕服务")
        .setContentText("正在为播放器提供弹幕服务")
        .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).build());
    NodeRuntime.start(new File(getFilesDir(), "nodejs-project").getAbsolutePath(), this::stopSelf);
  }
  @Override public int onStartCommand(Intent intent, int flags, int id) { return START_STICKY; }
  @Override public IBinder onBind(Intent intent) { return null; }
  @Override public void onDestroy() {
    stopForeground(STOP_FOREGROUND_REMOVE);
    super.onDestroy();
    // This is the private :server process, never the activity process.
    android.os.Process.killProcess(android.os.Process.myPid());
  }
}
