package tk.mygod.speech.synthesizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Project: MygodSpeechSynthesizer
 * @author  Mygod
 */
public final class NotificationHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (TtsEngineManager.mainActivity != null &&
                "tk.mygod.speech.synthesizer.action.STOP".equals(intent.getAction()))
            TtsEngineManager.mainActivity.stopSynthesis();
    }
}
