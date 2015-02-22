package tk.mygod.speech.tts;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import tk.mygod.util.FileUtils;
import tk.mygod.util.IOUtils;
import tk.mygod.util.LocaleUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

/**
 * @author Mygod
 */
@SuppressLint("NewApi")
public final class SvoxPicoTtsEngine extends TtsEngine implements TextToSpeech.OnInitListener {
    private static Field earcons, startLock;
    private final Comparator<TtsVoice> voiceComparator = new Comparator<TtsVoice>() {
        @Override
        public int compare(TtsVoice lhs, TtsVoice rhs) {
            String l = lhs.getDisplayName(context), r = rhs.getDisplayName(context);
            int result = l.compareToIgnoreCase(r);
            return result == 0 ? l.compareTo(r) : result;
        }
    };
    private final Semaphore initLock = new Semaphore(1);
    protected TextToSpeech tts;
    private SpeechPart lastPart;
    public TextToSpeech.EngineInfo engineInfo;
    private CharSequence currentText;
    private Locale lastLanguage;
    private int startOffset;
    private boolean useNativeVoice = Build.VERSION.SDK_INT >= 21;

    static {
        try {
            Class c = TextToSpeech.class;
            (earcons = c.getDeclaredField("mEarcons")).setAccessible(true);
            (startLock = c.getDeclaredField("mStartLock")).setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public SvoxPicoTtsEngine(final Context context) {
        super(context);
        initLock.acquireUninterruptibly();
        tts = new TextToSpeech(this.context = context, this);
        setListener();
    }
    public SvoxPicoTtsEngine(Context context, TextToSpeech.EngineInfo info) {
        super(context);
        initLock.acquireUninterruptibly();
        tts = new TextToSpeech(context, this, (engineInfo = info).name);
        setListener();
    }
    private Set<Locale> supportedLanguages;

    private void handleNativeVoiceException(RuntimeException exc) throws RuntimeException {
        if (exc instanceof NullPointerException && "collection == null".equals(exc.getMessage())) {
            useNativeVoice = false; // disable further attempts to improve performance
            Log.e("SvoxPicoTtsEngine", "Voices not supported: " + engineInfo.name);
        } else throw exc;
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) throw new RuntimeException("SvoxPicoTtsEngine initialization failed.");
        new Thread() {
            @Override
            public void run() {
                getVoices();
                supportedLanguages = new TreeSet<>(new LocaleUtils.DisplayNameComparator());
                for (Locale locale : Locale.getAvailableLocales()) try {
                    int test = tts.isLanguageAvailable(locale);
                    if (test == TextToSpeech.LANG_NOT_SUPPORTED) continue;
                    tts.setLanguage(locale);
                    supportedLanguages.add(getLanguage());
                } catch (Exception e) { // god damn Samsung TTS
                    e.printStackTrace();
                }
                if (lastLanguage == null) setDefaultLanguage(); else setLanguage(lastLanguage);
                initLock.release();
            }
        }.start();  // put init in a separate thread to speed up booting
    }
    @Override
    public Set<Locale> getLanguages() {
        initLock.acquireUninterruptibly();
        initLock.release();
        return supportedLanguages;
    }
    @SuppressWarnings("deprecation")
    private void setDefaultLanguage() {
        if (useNativeVoice) try {
            Voice voice = tts.getDefaultVoice();
            if (voice != null) {
                tts.setVoice(voice);
                setLanguage(voice.getLocale());
            }
            return;
        } catch (RuntimeException exc) {
            handleNativeVoiceException(exc);
        }
        setLanguage(Build.VERSION.SDK_INT >= 18 ? tts.getDefaultLanguage()
                : context.getResources().getConfiguration().locale);
    }
    @SuppressWarnings("deprecation")
    @Override
    public Locale getLanguage() {
        if (useNativeVoice) try {
            return tts.getVoice().getLocale();
        } catch (RuntimeException exc) {
            handleNativeVoiceException(exc);
        }
        return tts.getLanguage();
    }
    @Override
    public boolean setLanguage(Locale loc) {
        try {
            int test = tts.isLanguageAvailable(loc);
            if (test == TextToSpeech.LANG_NOT_SUPPORTED) return false;
            tts.setLanguage(lastLanguage = loc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Set<TtsVoice> getVoices() {
        if (useNativeVoice) try {
            TreeSet<TtsVoice> voices = new TreeSet<>(voiceComparator);
            for (Voice voice : tts.getVoices()) voices.add(new VoiceWrapper(voice));
            return voices;
        } catch (RuntimeException exc) {
            handleNativeVoiceException(exc);
        }
        return super.getVoices();
    }
    @Override
    public TtsVoice getVoice() {
        if (useNativeVoice) try {
            return new VoiceWrapper(tts.getVoice());
        } catch (RuntimeException exc) {
            handleNativeVoiceException(exc);
        }
        return new LocaleVoice(getLanguage());
    }
    @Override
    public boolean setVoice(TtsVoice voice) {
        if (Build.VERSION.SDK_INT >= 21 && voice instanceof VoiceWrapper) try {
            tts.setVoice(((VoiceWrapper) voice).voice);
            return true;
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return super.setVoice(voice);
    }
    @Override
    public boolean setVoice(String voiceName) {
        if (useNativeVoice) try {
            for (Voice voice : tts.getVoices()) if (voice.getName().equals(voiceName)) {
                tts.setVoice(voice);
                return true;
            }
            return false;
        } catch (RuntimeException exc) {
            handleNativeVoiceException(exc);
        }
        return super.setVoice(voiceName);
    }

    @Override
    public String getID() {
        return super.getID() + ':' + engineInfo.name;
    }
    @Override
    public String getName() {
        return engineInfo.label;
    }
    @Override
    protected Drawable getIconInternal() {
        return context.getPackageManager().getDrawable(engineInfo.name, engineInfo.icon, null);
    }

    @Override
    public String getMimeType() {
        return "audio/x-wav";
    }

    private Float pan;
    @Override
    public void setPitch(float value) {
        tts.setPitch(value);
    }
    @Override
    public void setSpeechRate(float value) {
        tts.setSpeechRate(value);
    }
    @Override
    public void setPan(float value) {
        pan = value;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Bundle getParamsL(String id) {
        Bundle params = new Bundle();
        Set<String> features = getVoice().getFeatures();
        if (features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_RETRIES_COUNT))
            params.putInt(TextToSpeech.Engine.KEY_FEATURE_NETWORK_RETRIES_COUNT, 0x7fffffff);
        if (features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_TIMEOUT_MS))
            params.putInt(TextToSpeech.Engine.KEY_FEATURE_NETWORK_TIMEOUT_MS, 0x7fffffff);
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
        if (pan != null) params.putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, pan);
        return params;
    }
    private HashMap<String, String> getParams(String id) {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
        if (pan != null) params.put(TextToSpeech.Engine.KEY_PARAM_PAN, pan.toString());
        return params;
    }
    @Override
    public void speak(CharSequence text, int startOffset) {
        currentText = text;
        this.startOffset = startOffset;
        synthesizeToStreamTask = null;
        (speakTask = new SpeakTask()).execute();
    }
    @Override
    public void synthesizeToStream(CharSequence text, int startOffset, FileOutputStream output, File cacheDir) {
        currentText = text;
        this.startOffset = startOffset;
        speakTask = null;
        (synthesizeToStreamTask = new SynthesizeToStreamTask()).execute(output, cacheDir);
    }
    @Override
    public void stop() {
        if (speakTask != null) speakTask.cancel(false);
        if (synthesizeToStreamTask != null) synthesizeToStreamTask.cancel(false);
        tts.stop();
    }

    private void setListener() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (speakTask == null) return;
                SpeechPart part = SpeechPart.parse(utteranceId);
                if (listener != null) listener.onTtsSynthesisCallback(part.Start, part.End);
            }

            @Override
            public void onDone(String utteranceId) {
                SpeechPart part = SpeechPart.parse(utteranceId);
                if (synthesizeToStreamTask != null) {
                    synthesizeToStreamTask.mergeQueue.add(utteranceId);
                    synthesizeToStreamTask.synthesizeLock.release();
                    if (listener != null) listener.onTtsSynthesisPrepared(part.End);
                }
                else if (speakTask != null && listener != null)
                    if (part.Start == lastPart.Start && part.End == lastPart.End) {
                        listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
                        speakTask = null;
                    }
                    else listener.onTtsSynthesisCallback(part.End, part.End);
            }

            @Override
            public void onError(String utteranceId) {
                SpeechPart part = SpeechPart.parse(utteranceId);
                if (listener != null) listener.onTtsSynthesisError(part.Start, part.End);
                if (synthesizeToStreamTask != null) synthesizeToStreamTask.synthesizeLock.release();
            }
        });
    }

    @Override
    public void onDestroy() {
        stop();
        tts.shutdown();
    }

    @Override
    protected int getMaxLength() {
        return Build.VERSION.SDK_INT >= 18 ? TextToSpeech.getMaxSpeechInputLength() : 4000;  // fallback to default
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static final class VoiceWrapper extends TtsVoice {
        private Voice voice;

        VoiceWrapper(Voice voice) {
            this.voice = voice;
        }

        @Override
        public Set<String> getFeatures() {
            return voice.getFeatures();
        }
        @Override
        public int getLatency() {
            return voice.getLatency();
        }
        @Override
        public Locale getLocale() {
            return voice.getLocale();
        }
        @Override
        public String getName() {
            return voice.getName();
        }
        @Override
        public int getQuality() {
            return voice.getQuality();
        }
        @Override
        public boolean isNetworkConnectionRequired() {
            return voice.isNetworkConnectionRequired();
        }
        @Override
        public String getDisplayName(Context context) {
            return String.format("%s (%s)", voice.getName(), voice.getLocale().getDisplayName());
        }

        @Override
        public String toString() {
            return voice.getName();
        }
    }
    @SuppressWarnings("deprecation")
    final class LocaleVoice extends LocaleWrapper {
        LocaleVoice(Locale loc) {
            super(loc);
        }

        @Override
        public Set<String> getFeatures() {
            Set<String> features = tts.getFeatures(locale);
            if (tts.isLanguageAvailable(getLocale()) == TextToSpeech.LANG_MISSING_DATA)
                features.add(ConstantsWrapper.KEY_FEATURE_NOT_INSTALLED);
            return features;
        }
        @Override
        public boolean isNetworkConnectionRequired() {
            Set<String> features = getFeatures();
            return features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) &&
                    !features.contains(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
        }
    }

    private SpeakTask speakTask;
    private SynthesizeToStreamTask synthesizeToStreamTask;

    private final class SpeakTask extends AsyncTask<Void, Void, Void> {
        @SuppressWarnings("deprecation")
        @Override
        protected Void doInBackground(Void... params) {
            try {
                ArrayList<SpeechPart> parts = splitSpeech(currentText, startOffset, true);
                lastPart = null;
                for (SpeechPart part : parts) try {
                    if (isCancelled()) {
                        tts.stop();
                        return null;
                    }
                    CharSequence cs = currentText.subSequence(part.Start, part.End);
                    String id = part.toString();
                    if (part.IsEarcon) {
                        String uri = cs.toString();
                        synchronized (startLock.get(tts)) {
                            ((Map<String, Uri>) earcons.get(tts)).put(uri, Uri.parse(uri));
                        }
                        if (Build.VERSION.SDK_INT >= 21)
                            tts.playEarcon(uri, TextToSpeech.QUEUE_ADD, getParamsL(id), id);
                        else tts.playEarcon(uri, TextToSpeech.QUEUE_ADD, getParams(id));
                    } else if (Build.VERSION.SDK_INT >= 21) tts.speak(cs, TextToSpeech.QUEUE_ADD, getParamsL(id), id);
                    else tts.speak(cs.toString(), TextToSpeech.QUEUE_ADD, getParams(id));
                    lastPart = part;   // assuming preparer is faster than speaker, which is often the case
                    if (listener != null) listener.onTtsSynthesisPrepared(part.End);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) listener.onTtsSynthesisError(part.Start, part.End);
                }
                if (isCancelled()) tts.stop();
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) listener.onTtsSynthesisError(0, currentText.length());
            }
            if (lastPart == null && listener != null)   // nothing speakable, end now
                listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
            return null;
        }
    }

    private final class SynthesizeToStreamTask extends AsyncTask<Object, Void, Void> {
        private FileOutputStream output;
        private ArrayList<SpeechPart> parts;
        private final LinkedBlockingDeque<String> mergeQueue = new LinkedBlockingDeque<>();
        private final HashMap<String, File> fileMap = new HashMap<>();
        public final Semaphore synthesizeLock = new Semaphore(1);

        private class BackgroundThread extends Thread {
            private byte[] header = null;
            private long length = 0;
            private int partIndex = 0;

            private void processFile(SpeechPart part, String id) {
                File cache = null;
                InputStream input = null;
                try {
                    if (!part.equals(parts.get(partIndex))) throw new IOException("Input is not in order!");
                    ++partIndex;
                    if (isCancelled()) return;
                    if (listener != null) listener.onTtsSynthesisCallback(part.Start, part.End);
                    input = (cache = fileMap.get(id)) == null
                            ? context.getContentResolver().openInputStream(Uri.parse(
                            currentText.subSequence(part.Start, part.End).toString()))
                            : new FileInputStream(cache);
                    if (header == null) {
                        header = new byte[44];
                        if (input.read(header, 0, 44) != 44) throw new IOException("File malformed.");
                        output.write(header, 0, 44);
                    } else if (input.skip(44) != 44) throw new IOException("File malformed.");
                    length += IOUtils.copy(input, output);
                    if (listener != null) listener.onTtsSynthesisCallback(part.End, part.End);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) listener.onTtsSynthesisError(part.Start, part.End);
                } finally {
                    if (input != null) try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fileMap.remove(id);
                    if (cache != null && !cache.delete()) cache.deleteOnExit();
                }
            }

            private void processEarcons() {
                SpeechPart part;
                while (partIndex < parts.size() && (part = parts.get(partIndex)).IsEarcon)
                    processFile(part, part.toString());
            }

            @Override
            public void run() {
                try {
                    processEarcons();
                    String id;
                    while (!(id = mergeQueue.take()).isEmpty()) {
                        processFile(SpeechPart.parse(id), id);
                        processEarcons();
                    }
                    if (header != null) {                   // update the header even if the operation is
                        header[40] = (byte) length;         // cancelled so that saved part can be read
                        header[41] = (byte) (length >> 8);
                        header[42] = (byte) (length >> 16);
                        header[43] = (byte) (length >> 24);
                        length += 36;
                        header[4] = (byte) length;
                        header[5] = (byte) (length >> 8);
                        header[6] = (byte) (length >> 16);
                        header[7] = (byte) (length >> 24);
                        output.getChannel().position(0);
                        output.write(header, 0, 44);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) listener.onTtsSynthesisError(0, currentText.length());
                } finally {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!isCancelled() && listener != null)
                        listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
                    synthesizeToStreamTask = null;
                }
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        protected Void doInBackground(Object... params) {
            Thread merger = null;
            try {
                if (params.length != 2 || !(params[0] instanceof FileOutputStream) || !(params[1] instanceof File))
                    throw new InvalidParameterException("Params incorrect.");
                output = (FileOutputStream) params[0];
                File cacheDir = (File) params[1];
                parts = splitSpeech(currentText, startOffset, false);
                if (isCancelled()) return null;
                (merger = new BackgroundThread()).start();
                for (SpeechPart part : parts) {
                    if (isCancelled()) return null;
                    if (part.IsEarcon) continue;
                    try {
                        File cache = new File(cacheDir, FileUtils.getTempFileName() + part.Start);
                        String id = part.toString();
                        fileMap.put(id, cache);
                        synthesizeLock.acquireUninterruptibly();
                        CharSequence cs = currentText.subSequence(part.Start, part.End);
                        if (Build.VERSION.SDK_INT >= 21) tts.synthesizeToFile(cs, getParamsL(id), cache, id);
                        else tts.synthesizeToFile(cs.toString(), getParams(id), cache.getAbsolutePath());
                        synthesizeLock.acquireUninterruptibly();    // wait for synthesis
                        synthesizeLock.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (listener != null) listener.onTtsSynthesisError(part.Start, part.End);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) listener.onTtsSynthesisError(0, currentText.length());
            } finally {
                if (merger != null) mergeQueue.add(""); // put end sign
            }
            return null;
        }
    }
}
