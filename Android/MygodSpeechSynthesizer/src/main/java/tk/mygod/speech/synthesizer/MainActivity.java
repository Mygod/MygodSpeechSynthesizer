package tk.mygod.speech.synthesizer;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;
import tk.mygod.app.FileSaveFragment;
import tk.mygod.app.ProgressActivity;
import tk.mygod.speech.tts.TtsEngine;
import tk.mygod.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
public class MainActivity extends ProgressActivity implements TtsEngine.OnTtsSynthesisCallbackListener,
        TtsEngineManager.OnSelectedEngineChangedListener, FileSaveFragment.Callbacks {
    /**
     * Answer to The Ultimate Question of Life, the Universe, and Everything.
     */
    private static final int SAVE_REQUEST_CODE = 42;
    private EditText inputText;
    private MenuItem synthesizeMenu, synthesizeToFileMenu;
    private boolean working;
    private static final InputFilter[] noFilters = new InputFilter[0],
            readonlyFilters = new InputFilter[] { new InputFilter() {
                public CharSequence filter(CharSequence src, int start, int end, Spanned dest, int dstart, int dend) {
                    return dest.subSequence(dstart, dend);
                }
            } };
    private ParcelFileDescriptor descriptor;    // used to keep alive from GC

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputText = (EditText)findViewById(R.id.inputText);
        TtsEngineManager.init(getApplicationContext(), this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        synthesizeMenu = menu.findItem(R.id.synthesize);
        synthesizeToFileMenu = menu.findItem(R.id.synthesize_to_file);
        return true;
    }

    private void startSynthesis() {
        TtsEngineManager.engines.selectedEngine
                .setPitch(Float.parseFloat(TtsEngineManager.pref.getString("tweaks.pitch", "1")));
        TtsEngineManager.engines.selectedEngine
                .setSpeechRate(Float.parseFloat(TtsEngineManager.pref.getString("tweaks.speechRate", "1")));
        TtsEngineManager.engines.selectedEngine
                .setPan(Float.parseFloat(TtsEngineManager.pref.getString("tweaks.pan", "0")));
        TtsEngineManager.engines.selectedEngine
                .setIgnoreSingleLineBreaks(TtsEngineManager.pref.getBoolean("text.ignoreSingleLineBreak", false));
        synthesizeMenu.setIcon(R.drawable.ic_action_mic_muted);
        synthesizeMenu.setTitle(R.string.stop);
        synthesizeToFileMenu.setEnabled(false);
        inputText.setFilters(readonlyFilters);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(inputText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        setActionBarProgressMax(inputText.getText().length());
        setActionBarSecondaryProgress(0);
        setActionBarProgress(-1);   // initializing
        working = true;
    }
    private void stopSynthesis() {
        TtsEngineManager.engines.selectedEngine.stop();
        synthesizeMenu.setIcon(R.drawable.ic_action_mic);
        synthesizeMenu.setTitle(R.string.synthesize);
        synthesizeToFileMenu.setEnabled(true);
        inputText.setFilters(noFilters);
        setActionBarProgress(null);
        if (descriptor != null) descriptor = null;  // pretending I'm reading the value here
        working = false;
    }
    @Override
    public void onSelectedEngineChanged() {
        stopSynthesis();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.synthesize:
                if (working) stopSynthesis(); else {
                    try {
                        startSynthesis();
                        TtsEngineManager.engines.selectedEngine.setSynthesisCallbackListener(this);
                        TtsEngineManager.engines.selectedEngine.speak(inputText.getText().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, String.format(getText(R.string.synthesis_error).toString(),
                                e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                        stopSynthesis();
                    }
                }
                return true;
            case R.id.synthesize_to_file:
                String fileName = FileUtils.getTempFileName() + '.' + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(TtsEngineManager.engines.selectedEngine.getMimeType());
                if (Build.VERSION.SDK_INT < 19 ||
                        TtsEngineManager.pref.getBoolean("appearance.oldTimeySaveDialog", Build.VERSION.SDK_INT < 19)) {
                    FileSaveFragment fsf = FileSaveFragment.newInstance(this);
                    fsf.setDefaultFileName(fileName);
                    String dir = TtsEngineManager.pref.getString("fileSystem.lastSaveDir", "");
                    if (dir != null && !dir.isEmpty()) fsf.setCurrentDirectory(new File(dir));
                    fsf.show(getFragmentManager(), "");
                }
                else {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(TtsEngineManager.engines.selectedEngine.getMimeType());
                    intent.putExtra(Intent.EXTRA_TITLE, fileName);
                    startActivityForResult(intent, SAVE_REQUEST_CODE);
                }
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void synthesizeToStream(FileOutputStream output) {
        try {
            startSynthesis();
            TtsEngineManager.engines.selectedEngine.setSynthesisCallbackListener(this);
            TtsEngineManager.engines.selectedEngine
                    .synthesizeToStream(inputText.getText().toString(), output, getCacheDir());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, String.format(getText(R.string.synthesis_error).toString(),
                    e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            stopSynthesis();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SAVE_REQUEST_CODE:
                if (resultCode != RESULT_OK) return;
                try {
                    synthesizeToStream(new FileOutputStream((descriptor = getContentResolver()
                            .openFileDescriptor(data.getData(), "w")).getFileDescriptor()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, String.format(getText(R.string.synthesis_error).toString(), e.getMessage()),
                                   Toast.LENGTH_LONG).show();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public boolean onCanSave(String absolutePath, String fileName) {
        return true;
    }
    @Override
    public void onConfirmSave(String absolutePath, String fileName) {
        if (fileName == null || fileName.isEmpty()) return;
        TtsEngineManager.editor.putString("fileSystem.lastSaveDir", absolutePath);
        TtsEngineManager.editor.apply();
        try {
            synthesizeToStream(new FileOutputStream(new File(absolutePath, fileName)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, String.format(getText(R.string.synthesis_error).toString(), e.getMessage()),
                           Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onTtsSynthesisPrepared(final int end) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setActionBarSecondaryProgress(end);
            }
        });
    }
    @Override
    public void onTtsSynthesisCallback(final int start, final int end) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setActionBarProgress(start);
                inputText.setSelection(start, end);
                inputText.moveCursorToVisibleOffset();
                if (start >= inputText.getText().length()) stopSynthesis();
            }
        });
    }
    @Override
    public void onTtsSynthesisError(final int start, final int end) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (start < end)
                    Toast.makeText(MainActivity.this, String.format(getText(R.string.synthesis_error).toString(),
                                   inputText.getText().toString().substring(start, end)), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        TtsEngineManager.engines.onDestroy();
        super.onDestroy();
    }
}
