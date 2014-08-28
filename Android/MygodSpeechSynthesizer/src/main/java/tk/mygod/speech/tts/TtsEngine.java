package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
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

    public abstract void speak(String text);
    public abstract void synthesizeToStream(String text, FileOutputStream output, File cacheDir);
    public abstract void stop();

    public abstract void onDestroy();

    private boolean ignoreSingleLineBreaks;
    public void setIgnoreSingleLineBreaks(boolean value) {
        ignoreSingleLineBreaks = value;
    }

    protected String processText(String text) {
        if (ignoreSingleLineBreaks) text = text.replaceAll("(?<![\\r\\n])(\\r|\\r?\\n)(?![\\r\\n])", " ");
        return text;
    }

    public static interface OnTtsSynthesisCallbackListener {
        public void onTtsSynthesisPrepared(int end);
        public void onTtsSynthesisCallback(int start, int end);
        public void onTtsSynthesisError(int start, int end);
    }

    protected static Pair<Integer, Integer> getRange(String id) {
        String[] parts = id.split(",");
        return new Pair<Integer, Integer>(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private static final HashMap<Character, Integer> splitters = new HashMap<Character, Integer>();
    private static final int SPLITTERS_COUNT = 5, BEST_SPLITTERS_EVER = 0;
    static {
        int priority = BEST_SPLITTERS_EVER;
        for (String group : new String[] { ".?!。？！…", ":;：；—", ",()[]{}，（）【】『』［］｛｝、",
                "'\"‘’“”＇＂<>＜＞《》", " \t\b\n\r\f\b\u000b\u00a0\u2028\u2029/\\|-／＼｜－" }) {
            int length = group.length();
            for (int i = 0; i < length; ++i) splitters.put(group.charAt(i), priority);
            ++priority;
        }
    }
    protected abstract int getMaxLength();
    protected ArrayList<Pair<Integer, Integer>> splitSpeech(String text, boolean aggressiveMode) {
        int last = 0, length = text.length(), maxLength = getMaxLength();
        if (maxLength <= 0) throw new InvalidParameterException("maxLength should be a positive value.");
        ArrayList<Pair<Integer, Integer>> result = new ArrayList<Pair<Integer, Integer>>();
        while (last < length && splitters.get(text.charAt(last)) != null) ++last;
        while (last < length) {
            int i = last + 1, maxEnd = last + maxLength, bestPriority = SPLITTERS_COUNT;
            if (maxEnd > length) maxEnd = length;
            int end = maxEnd;
            while (i < maxEnd) {
                Integer priority = splitters.get(text.charAt(i));
                if (priority != null && priority <= bestPriority) {
                    end = i;
                    bestPriority = priority;
                    if (aggressiveMode && priority == BEST_SPLITTERS_EVER) break;
                }
                ++i;
            }
            while (end < maxEnd && splitters.get(text.charAt(end)) != null) ++end;  // some more punctuations? why not?
            result.add(new Pair<Integer, Integer>(last, end));
            last = end;
            while (last < length && splitters.get(text.charAt(last)) != null) ++last;
        }
        return result;
    }
}
