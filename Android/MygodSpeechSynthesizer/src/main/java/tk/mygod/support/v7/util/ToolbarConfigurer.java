package tk.mygod.support.v7.util;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.Toolbar;
import android.view.View;
import tk.mygod.speech.synthesizer.R;

/**
 * @author Mygod
 */
public final class ToolbarConfigurer implements View.OnClickListener {
    private Activity activity;

    public ToolbarConfigurer(Activity activity, Toolbar toolbar, boolean displayHomeAsUpEnabled) {
        toolbar.setTitle((this.activity = activity).getTitle());
        if (!displayHomeAsUpEnabled) return;
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = activity.getParentActivityIntent();
        if (intent == null) activity.finish(); else activity.navigateUpTo(intent);
    }
}
