package tk.mygod.speech.tts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import tk.mygod.speech.synthesizer.R;
import tk.mygod.util.LocaleUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Project:  Mygod Speech Synthesizer
 * @author   Mygod
 * Based on: https://github.com/hungtruong/Google-Translate-TTS
 */
public class GoogleTranslateTtsEngine extends TtsEngine {
    private static Set<Locale> supportedLanguages;
    private final MediaPlayer player;
    private String language = "en";

    static {
        supportedLanguages = new TreeSet<Locale>(new LocaleUtil.DisplayNameComparator());
        for (String code : new String[] {
                "af", "sq", "ar", "hy", "bs", "ca", "zh-CN", "zh-TW", "hr", "cs", "da", "nl", "en", "eo", "fi", "fr",
                "de", "el", "ht", "hi", "hu", "is", "id", "it", "ja", "la", "lv", "mk", "no", "pl", "pt", "ro", "ru",
                "sr", "sk", "es", "sw", "sv", "ta", "th", "tr", "vi", "cy" })
            supportedLanguages.add(LocaleUtil.parseLocale(code));
    }
    public GoogleTranslateTtsEngine() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public Set<Locale> getSupportedLanguages() {
        return supportedLanguages;
    }
    @Override
    public Locale getLanguage() {
        return LocaleUtil.parseLocale(language);
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
    public CharSequence getName(Context context) {
        return context.getResources().getText(R.string.google_translate_tts_engine_name);
    }
    @Override
    protected Drawable getIconInternal(Context context) {
        try {
            return context.getPackageManager().getApplicationIcon("com.google.android.apps.translate");
        } catch (Exception e) {
            return context.getResources().getDrawable(R.drawable.ic_google_translate);
        }
    }

    @Override
    public void speak(String text, OnTtsSynthesisCallbackListener listener) throws IOException {
        player.reset();
        // TODO: long text splitting, SPML support
        player.setDataSource("https://translate.google.com/translate_tts?ie=UTF-8&tl=" + language +
                "&q=" + URLEncoder.encode(text, "UTF-8"));
        player.prepare();
        player.start();
    }

    @Override
    public void synthesizeToFile(String text, String filename, OnTtsSynthesisCallbackListener listener) {
        // TODO: implement this
    }

    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public void onDestroy() {
        player.stop();
        player.release();
    }
}
