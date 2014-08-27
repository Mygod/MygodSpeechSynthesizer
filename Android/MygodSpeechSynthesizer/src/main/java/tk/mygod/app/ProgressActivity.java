package tk.mygod.app;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import tk.mygod.widget.ButteryProgressBar;

/**
 * @author   Mygod
 * Based on: http://stackoverflow.com/a/15073680/2245107
 */
public class ProgressActivity extends Activity {
    protected ProgressBar progressBar;
    protected ButteryProgressBar butteryProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        (progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal))
                .setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, 24));
        (butteryProgressBar = new ButteryProgressBar(this))
                .setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, 24));
        setActionBarProgress(null);
        final FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(butteryProgressBar);
        ViewTreeObserver observer = butteryProgressBar.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                View contentView = decorView.findViewById(android.R.id.content);
                butteryProgressBar.setY(contentView.getY() - 10);
                ViewTreeObserver observer = butteryProgressBar.getViewTreeObserver();
                if (Build.VERSION.SDK_INT < 16) observer.removeGlobalOnLayoutListener(this);
                else observer.removeOnGlobalLayoutListener(this);
            }
        });
    }

    public ProgressBar getProgressBar() { return progressBar; }
    public ButteryProgressBar getButteryProgressBar(){
        return butteryProgressBar;
    }

    public Integer getActionBarProgress() {
        if (progressBar.getVisibility() == View.VISIBLE) return progressBar.getProgress();
        if (butteryProgressBar.getVisibility() == View.VISIBLE) return -1;
        return null;
    }
    public void setActionBarProgress(Integer progress) {
        if (progress == null) {
            progressBar.setVisibility(View.GONE);
            butteryProgressBar.setVisibility(View.GONE);
        } else if (progress < 0) {
            progressBar.setVisibility(View.GONE);
            butteryProgressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(progress);
            butteryProgressBar.setVisibility(View.GONE);
        }
    }
}
