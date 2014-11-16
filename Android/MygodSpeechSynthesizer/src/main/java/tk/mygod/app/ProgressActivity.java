package tk.mygod.app;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import tk.mygod.speech.synthesizer.R;
import tk.mygod.widget.ButteryProgressBar;

/**
 * @author   Mygod
 * Based on: http://stackoverflow.com/a/15073680/2245107
 */
public class ProgressActivity extends ActionBarActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private FrameLayout decorView;
    private ProgressBar progressBar;
    private ButteryProgressBar butteryProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                (int) (getResources().getDisplayMetrics().density * 8));
        (progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)).setLayoutParams(params);
        (butteryProgressBar = new ButteryProgressBar(this)).setLayoutParams(params);
        (decorView = (FrameLayout) getWindow().getDecorView()).addView(progressBar);
        decorView.addView(butteryProgressBar);
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        setActionBarProgress(null);
    }

    @Override
    public void onGlobalLayout() {
        Rect rect = new Rect();
        decorView.getWindowVisibleDisplayFrame(rect);
        float y = rect.top + decorView.findViewById(R.id.content).getY() -
                getResources().getDisplayMetrics().density * 4;
        progressBar.setY(y);
        butteryProgressBar.setY(y);
    }

    public ProgressBar getProgressBar() { return progressBar; }
    public ButteryProgressBar getButteryProgressBar(){
        return butteryProgressBar;
    }

    public int getActionBarProgressMax() {
        return progressBar.getMax();
    }
    public void setActionBarProgressMax(int value) {
        progressBar.setMax(value);
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
