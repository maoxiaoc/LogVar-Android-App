package app.logvar;

/** Use available content bounds, never device-model or physical-resolution guesses. */
public final class LayoutPolicy {
    public static boolean compact(float widthDp, float heightDp, float fontScale) {
        return fontScale <= 1.15f && (heightDp < 820f || widthDp < 400f);
    }
}
