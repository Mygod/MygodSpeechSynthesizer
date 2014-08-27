package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import tk.mygod.speech.synthesizer.R;
import tk.mygod.util.IOUtils;
import tk.mygod.util.LocaleUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Project:  Mygod Speech Synthesizer
 * @author   Mygod
 * Based on: https://github.com/hungtruong/Google-Translate-TTS
 */
public class GoogleTranslateTtsEngine extends TtsEngine
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
    private static Set<Locale> supportedLanguages;
    private static Set<String> supportedFeatures;
    private final MediaPlayer player;
    private String language = "en", currentText;

    static {
        supportedLanguages = new TreeSet<Locale>(new LocaleUtils.DisplayNameComparator());
        for (String code : new String[] {
                "af", "sq", "ar", "hy", "bs", "ca", "zh-CN", "zh-TW", "hr", "cs", "da", "nl", "en", "eo", "fi", "fr",
                "de", "el", "ht", "hi", "hu", "is", "id", "it", "ja", "la", "lv", "mk", "no", "pl", "pt", "ro", "ru",
                "sr", "sk", "es", "sw", "sv", "ta", "th", "tr", "vi", "cy" })
            supportedLanguages.add(LocaleUtils.parseLocale(code));
        supportedFeatures = new HashSet<String>(1);
        supportedFeatures.add(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
    }
    public GoogleTranslateTtsEngine() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    @Override
    public Set<Locale> getSupportedLanguages() {
        return supportedLanguages;
    }
    @Override
    public Locale getLanguage() {
        return LocaleUtils.parseLocale(language);
    }
    @Override
    public boolean setLanguage(Locale loc) {
        String lang = loc.toString().replace('_', '-');
        if (supportedLanguages.contains(loc)) {
            language = lang;
            return true;
        }
        return false;
    }
    @Override
    public Set<String> getFeatures(Locale locale) {
        return supportedFeatures;
    }

    @Override
    public CharSequence getName(Context context) {
        return context.getResources().getText(R.string.google_translate_tts_engine_name);
    }
    @Override
    protected Drawable getIconInternal(Context context) {
        try {
            return context.getPackageManager().getApplicationIcon("com.google.android.apps.translate");
        } catch (Exception e) { // fallback if you don't have the official app installed :S
            return context.getResources().getDrawable(R.drawable.ic_google_translate);
        }
    }

    @Override
    public String getMimeType() {
        return "audio/mpeg";
    }

    private String getUrl(String text) throws UnsupportedEncodingException {
        return "https://translate.google.com/translate_tts?ie=UTF-8&tl=" + language +
               "&q=" + URLEncoder.encode(text, "UTF-8");
    }
    @Override
    public void speak(String text) throws IOException {
        player.reset();
        // TODO: long text splitting
        player.setDataSource(getUrl(currentText = text));
        player.prepareAsync();
    }
    @Override
    public void synthesizeToFile(String text, String filename) throws IOException {
        // TODO: long text splitting
        currentText = text;
        new SynthesizeToFileTask().execute(filename);
    }
    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (listener != null) listener.onTtsSynthesisCallback(0, currentText.length());
        mp.start();
    }
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (listener != null) listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
    }
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (listener != null) listener.onTtsSynthesisError(0, currentText.length());
        return false;
    }

    @Override
    public void onDestroy() {
        player.stop();
        player.release();
    }

    private class SynthesizeToFileTask extends AsyncTask<String, Integer, Exception> {
        @Override
        protected Exception doInBackground(String... params) {
            try {
                if (params.length != 1) throw new InvalidParameterException("There must be and only be 1 param.");
                InputStream input = null;
                FileOutputStream output = null;
                if (listener != null) listener.onTtsSynthesisCallback(0, currentText.length());
                try {
                    IOUtils.copy(input = new URL(getUrl(currentText)).openStream(),
                                 output = new FileOutputStream(params[0]));
                    if (listener != null) listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
                } finally {
                    if (input != null) try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (output != null) try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }
        }

        protected void onPostExecute(Exception e) {
            if (listener == null) return;   // nobody listens?! well YKW fuck it
            if (e != null) listener.onTtsSynthesisError(0, currentText.length());
            listener.onTtsSynthesisCallback(currentText.length(), currentText.length());
        }
    }
}
