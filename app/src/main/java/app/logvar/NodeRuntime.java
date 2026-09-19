package app.logvar;

final class NodeRuntime {
  static { System.loadLibrary("node"); System.loadLibrary("logvar_native"); }
  private static boolean started;
  static synchronized void start(String root, Runnable onExit) {
    if (started) return;
    started = true;
    new Thread(() -> {
      try { start(new String[]{"node", root + "/main.cjs", root}); }
      finally { onExit.run(); }
    }, "Logvar server").start();
  }
  private static native int start(String[] args);
}
