package tk.mygod.speech.synthesizer;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;
import tk.mygod.app.FileSaveFragment;
import tk.mygod.app.ProgressActivity;
import tk.mygod.speech.tts.TtsEngine;
import tk.mygod.util.FileUtils;
import tk.mygod.util.IOUtils;

import java.io.*;

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
    private Uri synthesisTarget;
    private File synthesisFile, tempFile;
    private static final InputFilter[] noFilters = new InputFilter[0],
            readonlyFilters = new InputFilter[] { new InputFilter() {
                public CharSequence filter(CharSequence src, int start, int end, Spanned dest, int dstart, int dend) {
                    return dest.subSequence(dstart, dend);
                }
            } };

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
        synthesizeMenu.setIcon(R.drawable.ic_action_mic_muted);
        synthesizeMenu.setTitle(R.string.stop);
        synthesizeToFileMenu.setEnabled(false);
        inputText.setFilters(readonlyFilters);
        progressBar.setMax(inputText.getText().length());
        setActionBarProgress(-1);   // initializing
        working = true;
    }
    private void stopSynthesis(boolean completed) {
        TtsEngineManager.engines.selectedEngine.stop();
        synthesizeMenu.setIcon(R.drawable.ic_action_mic);
        synthesizeMenu.setTitle(R.string.synthesize);
        synthesizeToFileMenu.setEnabled(true);
        inputText.setFilters(noFilters);
        if (completed && (synthesisTarget != null || synthesisFile != null)) {
            setActionBarProgress(progressBar.getMax());
            FileInputStream input = null;
            FileOutputStream output = null;
            try {
                input = new FileInputStream(tempFile);
                if (synthesisTarget != null) {
                    ParcelFileDescriptor pfd = null;
                    try {
                        pfd = getContentResolver().openFileDescriptor(synthesisTarget, "w");
                        output = new FileOutputStream(pfd.getFileDescriptor());
                        IOUtils.copy(input, output);
                    } finally {
                        if (pfd != null) try {
                            pfd.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    output = new FileOutputStream(synthesisFile);
                    IOUtils.copy(input, output);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, String.format(getText(R.string.synthesis_error).toString(),
                                                   e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
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
        }
        setActionBarProgress(null);
        synthesisTarget = null;
        synthesisFile = null;
        if (tempFile != null && tempFile.exists()) {
            tempFile.deleteOnExit();
            tempFile = null;
        }
        working = false;
    }
    @Override
    public void onSelectedEngineChanged() {
        stopSynthesis(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.synthesize:
                if (working) stopSynthesis(false); else {
                    try {
                        startSynthesis();
                        TtsEngineManager.engines.selectedEngine.setSynthesisCallbackListener(this);
                        TtsEngineManager.engines.selectedEngine.speak(inputText.getText().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, String.format(getText(R.string.synthesis_error).toString(),
                                e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                        stopSynthesis(false);
                    }
                }
                return true;
            case R.id.synthesize_to_file:
                String fileName = FileUtils.getTempFileName() + '.' + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(TtsEngineManager.engines.selectedEngine.getMimeType());
                if (Build.VERSION.SDK_INT >= 19) {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(TtsEngineManager.engines.selectedEngine.getMimeType());
                    intent.putExtra(Intent.EXTRA_TITLE, fileName);
                    startActivityForResult(intent, SAVE_REQUEST_CODE);
                }
                else {
                    FileSaveFragment fsf = FileSaveFragment.newInstance(this);
                    fsf.setDefaultFileName(fileName);
                    String dir = TtsEngineManager.pref.getString("fileSystem.lastSaveDir", "");
                    if (dir != null && !dir.isEmpty()) fsf.setCurrentDirectory(new File(dir));
                    fsf.show(getFragmentManager(), "");
                }
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void synthesizeToFile() {
        try {
            startSynthesis();
            TtsEngineManager.engines.selectedEngine.setSynthesisCallbackListener(this);
            TtsEngineManager.engines.selectedEngine.synthesizeToFile(inputText.getText().toString(),
                    (tempFile = new File(getCacheDir(), FileUtils.getTempFileName())).getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, String.format(getText(R.string.synthesis_error).toString(),
                    e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            stopSynthesis(false);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SAVE_REQUEST_CODE:
                if (resultCode != RESULT_OK) return;
                synthesisTarget = data.getData();
                synthesizeToFile();
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
        synthesisFile = new File(absolutePath, fileName);
        synthesizeToFile();
    }

    @Override
    public void onTtsSynthesisCallback(final int start, final int end) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setActionBarProgress(start);
                inputText.setSelection(start, end);
                inputText.moveCursorToVisibleOffset();
                if (start >= inputText.getText().length()) stopSynthesis(true);
            }
        });
    }
    @Override
    public void onTtsSynthesisError(final int start, final int end) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setActionBarProgress(start);
                inputText.setSelection(start, end);
                inputText.moveCursorToVisibleOffset();
                Toast.makeText(MainActivity.this, String.format(getText(R.string.synthesis_error).toString(),
                        inputText.getText().toString().substring(start, end)), Toast.LENGTH_LONG).show();
                stopSynthesis(false);   // force stop currently, alternatives possible
            }
        });
    }

    @Override
    public void onDestroy() {
        TtsEngineManager.engines.onDestroy();
        super.onDestroy();
    }
}
