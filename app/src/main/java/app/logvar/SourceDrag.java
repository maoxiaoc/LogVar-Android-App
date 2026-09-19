package app.logvar;

/** All coordinates are pixels in the source list, never mixed with dp. */
public final class SourceDrag {
    public static float top(float startTop, float distance, float rowHeight, int count) {
        return Math.max(0, Math.min(startTop + distance, (count - 1) * rowHeight));
    }

    public static int slot(float top, float rowHeight, int count) {
        return Math.max(0, Math.min(Math.round(top / rowHeight), count - 1));
    }
}
