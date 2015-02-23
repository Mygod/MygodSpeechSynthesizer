package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import tk.mygod.text.EarconSpan;

import java.io.File;
import java.io.FileOutputStream;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
public abstract class TtsEngine {
    protected Context context;
    protected TtsEngine(Context context) {
        this.context = context;
    }

    public abstract Set<TtsVoice> getVoices();
    public abstract TtsVoice getVoice();
    public abstract boolean setVoice(TtsVoice voice);
    public abstract boolean setVoice(String voice);

    private Drawable icon;
    public String getID() {
        return getClass().getSimpleName();
    }
    public String getName() {
        return getID();
    }
    public final Drawable getIcon() {
        if (icon == null) icon = getIconInternal();
        return icon;
    }
    protected abstract Drawable getIconInternal();

    protected OnTtsSynthesisCallbackListener listener;
    public final void setSynthesisCallbackListener(OnTtsSynthesisCallbackListener listener) {
        this.listener = listener;
    }
    public abstract String getMimeType();

    public void setPitch(float value) { }
    public void setSpeechRate(float value) { }
    public void setPan(float value) { }

    public abstract void speak(CharSequence text, int startOffset);
    public abstract void synthesizeToStream(CharSequence text, int startOffset, FileOutputStream output, File cacheDir);
    public abstract void stop();

    public abstract void onDestroy();

    public interface OnTtsSynthesisCallbackListener {
        void onTtsSynthesisPrepared(int end);
        void onTtsSynthesisCallback(int start, int end);
        void onTtsSynthesisError(int start, int end);
    }

    private static final HashMap<Character, Integer> splitters = new HashMap<>();
    private static final int SPLITTERS_COUNT = 6, BEST_SPLITTERS_EVER = 0, SPACE_FOR_THE_BEST = 1;
    static {
        int priority = BEST_SPLITTERS_EVER;
        for (String group : new String[] { "!。？！…", ".?", ":;：；—", ",()[]{}，（）【】『』［］｛｝、",
                "'\"‘’“”＇＂<>＜＞《》",
                " \t\b\n\r\f\r\u000b\u001c\u001d\u001e\u001f\u00a0\u2028\u2029/\\|-／＼｜－" }) {
            int length = group.length();
            for (int i = 0; i < length; ++i) splitters.put(group.charAt(i), priority);
            ++priority;
        }
    }
    protected abstract int getMaxLength();

    protected static class SpeechPart {
        private SpeechPart(int start, int end, boolean isEarcon) {
            Start = start;
            End = end;
            IsEarcon = isEarcon;
        }

        public int Start, End;
        public boolean IsEarcon;

        public boolean equals(SpeechPart other) {
            return Start == other.Start && End == other.End && IsEarcon == other.IsEarcon;
        }
        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other instanceof SpeechPart) return equals((SpeechPart) other);
            try {
                return equals(parse(other.toString()));
            } catch (Exception exc) {
                return false;
            }
        }

        public static SpeechPart parse(String value) {
            String[] parts = value.split(",");
            return new SpeechPart(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), false);
        }

        @Override
        public String toString() {
            return Start + "," + End;
        }
    }

    protected ArrayList<SpeechPart> splitSpeech(CharSequence text, int startOffset, boolean aggressiveMode) {
        int last = startOffset, length = text.length(), maxLength = getMaxLength();
        if (maxLength <= 0) throw new InvalidParameterException("maxLength should be a positive value.");
        ArrayList<SpeechPart> result = new ArrayList<>();
        int earconsLength = 0, nextEarcon = length;
        SpeechPart[] earconParts = null;
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            EarconSpan[] earcons = spanned.getSpans(last, length, EarconSpan.class);
            if ((earconsLength = earcons.length) > 0) {
                earconParts = new SpeechPart[earconsLength];
                for (int i = 0; i < earconsLength; ++i) {
                    EarconSpan span = earcons[i];
                    earconParts[i] = new SpeechPart(spanned.getSpanStart(span), spanned.getSpanEnd(span), true);
                }
                nextEarcon = earconParts[0].Start;
            }
        }
        int j = 0;
        while (last < nextEarcon && splitters.get(text.charAt(last)) != null) ++last;
        while (last < length) {
            if (last == nextEarcon) {
                SpeechPart part = earconParts[j];
                result.add(part);
                last = part.End;
                nextEarcon = ++j < earconsLength ? earconParts[j].Start : length;
            } else {
                int i = last + 1, maxEnd = last + maxLength, bestPriority = SPLITTERS_COUNT;
                if (maxEnd > nextEarcon) maxEnd = nextEarcon;
                int end = maxEnd;
                while (i < maxEnd) {
                    int next = i + 1;
                    Integer priority = splitters.get(text.charAt(i));
                    if (priority != null) {
                        if (priority == SPACE_FOR_THE_BEST && (next >= nextEarcon ||
                                Character.isWhitespace(text.charAt(next)))) priority = BEST_SPLITTERS_EVER;
                        if (priority <= bestPriority) {
                            i = next;
                            while (i < maxEnd && splitters.get(text.charAt(i)) != null) ++i;    // get more splitters
                            end = i;
                            bestPriority = priority;
                            if (aggressiveMode && priority == BEST_SPLITTERS_EVER) break;
                            continue;
                        }
                    }
                    i = next;
                }
                result.add(new SpeechPart(last, end, false));
                last = end;
            }
            while (last < nextEarcon && splitters.get(text.charAt(last)) != null) ++last;
        }
        return result;
    }
}
