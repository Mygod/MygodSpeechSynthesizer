package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Project: Mygod Speech Synthesizer
 * Author:  Mygod (mygod.tk)
 */
public abstract class TtsEngine {
    public abstract Set<Locale> getSupportedLanguages();
    public abstract Locale getLanguage();
    public boolean setLanguage(Locale loc) {
        return false;
    }
    public abstract Set<String> getFeatures(Locale locale);

    private Drawable icon;
    public String getID() {
        return getClass().getSimpleName();
    }
    public CharSequence getName(Context context) {
        return getID();
    }
    public final Drawable getIcon(Context context) {
        if (icon == null) icon = getIconInternal(context);
        return icon;
    }
    protected abstract Drawable getIconInternal(Context context);

    protected OnTtsSynthesisCallbackListener listener;
    public final void setSynthesisCallbackListener(OnTtsSynthesisCallbackListener listener) {
        this.listener = listener;
    }
    public abstract String getMimeType();

    public void setPitch(float value) { }
    public void setSpeechRate(float value) { }
    public void setPan(float value) { }

    public abstract void speak(String text) throws IOException;
    public abstract void synthesizeToFile(String text, String filename) throws IOException;
    public abstract void stop();

    public abstract void onDestroy();

    public static interface OnTtsSynthesisCallbackListener {
        public void onTtsSynthesisCallback(int start, int end);
        public void onTtsSynthesisError(int start, int end);
    }
}
