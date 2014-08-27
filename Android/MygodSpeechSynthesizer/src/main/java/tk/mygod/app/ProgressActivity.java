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
public class ProgressActivity extends Activity implements ViewTreeObserver.OnGlobalLayoutListener {
    private FrameLayout decorView;
    protected ProgressBar progressBar;
    protected ButteryProgressBar butteryProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        (progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal))
                .setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                        (int) (getResources().getDisplayMetrics().density * 8)));
        (butteryProgressBar = new ButteryProgressBar(this))
                .setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                        (int) (getResources().getDisplayMetrics().density * 8)));
        (decorView = (FrameLayout) getWindow().getDecorView()).addView(progressBar);
        decorView.addView(butteryProgressBar);
        progressBar.getViewTreeObserver().addOnGlobalLayoutListener(this);
        butteryProgressBar.getViewTreeObserver().addOnGlobalLayoutListener(this);
        setActionBarProgress(null);
    }

    @Override
    public void onGlobalLayout() {
        float y = decorView.findViewById(android.R.id.content).getY() - getResources().getDisplayMetrics().density * 4;
        progressBar.setY(y);
        butteryProgressBar.setY(y);
        if (Build.VERSION.SDK_INT < 16) {
            progressBar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            butteryProgressBar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
        } else {
            progressBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            butteryProgressBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
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
    public int getActionBarSecondaryProgress() {
        return progressBar.getSecondaryProgress();
    }
    public void setActionBarSecondaryProgress(int progress) {
        progressBar.setSecondaryProgress(progress);
    }
}
