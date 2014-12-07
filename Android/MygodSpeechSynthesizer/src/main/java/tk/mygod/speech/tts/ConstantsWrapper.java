package tk.mygod.speech.tts;

import android.os.Build;
import android.speech.tts.TextToSpeech;

/**
 * @author Mygod
 */
public final class ConstantsWrapper {
    private ConstantsWrapper() {
        throw new AssertionError();
    }

    public static final String
            KEY_FEATURE_NETWORK_RETRIES_COUNT = Build.VERSION.SDK_INT >= 21
                    ? TextToSpeech.Engine.KEY_FEATURE_NETWORK_RETRIES_COUNT : "networkRetriesCount",
            KEY_FEATURE_NETWORK_TIMEOUT_MS = Build.VERSION.SDK_INT >= 21
                    ? TextToSpeech.Engine.KEY_FEATURE_NETWORK_TIMEOUT_MS : "networkTimeoutMs",
            KEY_FEATURE_NOT_INSTALLED = Build.VERSION.SDK_INT >= 21
                    ? TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED : "notInstalled";
}
