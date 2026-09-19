import app.logvar.SourceDrag;

public class SourceDragCheck {
    public static void main(String[] args) {
        for (float density : new float[]{1f, 2f, 2.75f, 3.5f}) {
            float height = 48 * density;
            // A slow continuous gesture crosses several slots, then reverses.
            for (int delta = -400; delta <= 400; delta++) {
                float distance = delta * density;
                float top = SourceDrag.top(2 * height, distance, height, 7);
                float expected = Math.max(0, Math.min(2 * height + distance, 6 * height));
                if (Math.abs(top - expected) > .001f) throw new AssertionError("Not finger aligned");
                int slot = SourceDrag.slot(top, height, 7);
                if (slot < 0 || slot > 6 || Math.abs(slot * height - top) > height / 2 + .001f)
                    throw new AssertionError("Wrong swap slot");
            }
        }
        System.out.println("PASS: continuous dragging, reversal, bounds and four screen densities");
    }
}
