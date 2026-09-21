import app.logvar.LayoutPolicy;

public class LayoutPolicyCheck {
    public static void main(String[] args) {
        for (float[] size : new float[][]{{393, 740}, {411, 770}, {360, 640}}) {
            if (!LayoutPolicy.compact(size[0], size[1], 1)) throw new AssertionError("8.0 compact window");
            if (LayoutPolicy.compact(size[0], size[1], 1.5f)) throw new AssertionError("Respect enlarged fonts");
        }
        if (LayoutPolicy.compact(600, 900, 1)) throw new AssertionError("Keep spacious large windows");
        System.out.println("PASS: adaptive layout and large-font fallback");
    }
}
