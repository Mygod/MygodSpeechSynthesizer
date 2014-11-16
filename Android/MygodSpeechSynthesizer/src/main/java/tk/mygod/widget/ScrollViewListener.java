package tk.mygod.widget;

/**
 * @author   Mygod
 * Based on: http://stackoverflow.com/a/3952629/2245107
 */
public interface ScrollViewListener {
    void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy);
}
