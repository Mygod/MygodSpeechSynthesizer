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

    public final void speak(String text) throws IOException {
        speak(text, null);
    }
    public abstract void speak(String text, OnTtsSynthesisCallbackListener listener) throws IOException;

    public final void synthesizeToFile(String text, String filename) {
        synthesizeToFile(text, filename, null);
    }
    public abstract void synthesizeToFile(String text, String filename, OnTtsSynthesisCallbackListener listener);

    public abstract void stop();

    public abstract void onDestroy();
}
