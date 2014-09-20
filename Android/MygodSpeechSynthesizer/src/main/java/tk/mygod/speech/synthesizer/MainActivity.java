package tk.mygod.speech.synthesizer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;
import tk.mygod.CurrentApp;
import tk.mygod.app.ProgressActivity;
import tk.mygod.app.SaveFileActivity;
import tk.mygod.speech.tts.TtsEngine;
import tk.mygod.util.FileUtils;
import tk.mygod.util.IOUtils;

import java.io.*;
import java.text.SimpleDateFormat;

/**
 * Project: Mygod Speech Synthesizer
 * @author  Mygod
 */
public class MainActivity extends ProgressActivity implements TtsEngine.OnTtsSynthesisCallbackListener,
        TtsEngineManager.OnSelectedEngineChangingListener {
    private static final int OPEN_TEXT_CODE = 0, SAVE_TEXT_CODE = 1, SAVE_SYNTHESIS_CODE = 2,
                             IDLE = 0, SPEAKING = 1, SYNTHESIZING = 2;
    private EditText inputText;
    private Menu menu;
    private MenuItem synthesizeMenu;
    private int status;
    private boolean inBackground;
    private static final InputFilter[] noFilters = new InputFilter[0],
            readonlyFilters = new InputFilter[] { new InputFilter() {
                public CharSequence filter(CharSequence src, int start, int end, Spanned dest, int dstart, int dend) {
                    return dest.subSequence(dstart, dend);
                }
            } };
    private ParcelFileDescriptor descriptor;    // used to keep alive from GC
    private Notification.Builder builder;

    private String lastText, displayName;
    private void showNotification(CharSequence text) {
        if (status != SPEAKING) lastText = null;
        else if (text != null) lastText = text.toString().replaceAll("\\s+", " ");
        if (!inBackground) return;
        builder.setWhen(System.currentTimeMillis()).setContentText(lastText)
               .setTicker(TtsEngineManager.pref.getBoolean("appearance.ticker", false) ? lastText : null);
        if (Build.VERSION.SDK_INT >= 16) builder.setPriority(TtsEngineManager.pref.getBoolean
                ("appearance.notificationIcon", true) ? Notification.PRIORITY_DEFAULT : Notification.PRIORITY_MIN);
        else builder.setContentText(null).setTicker(null);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, Build.VERSION.SDK_INT >= 16
                ? new Notification.BigTextStyle(builder).bigText(lastText).build() : builder.getNotification());
    }
    private void cancelNotification() {
        inBackground = false;   // which disables further notifications
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        (inputText = (EditText)findViewById(R.id.input_text))
                .setText(String.format(getText(R.string.input_text_default).toString(), CurrentApp.getVersionName(this),
                                       SimpleDateFormat.getInstance().format(CurrentApp.getBuildTime(this))));
        TtsEngineManager.init(this, this);
        Intent intent = new Intent();
        intent.setAction("tk.mygod.speech.synthesizer.action.STOP");
        builder = new Notification.Builder(this).setContentTitle(getString(R.string.notification_title))
                .setAutoCancel(true).setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(this, 0, intent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notification));
        intent = getIntent();
        if (intent.getData() != null) onNewIntent(intent);
    }

    @Override
    protected void onNewIntent(Intent data) {
        if (data == null) return;
        InputStream input = null;
        try {
            Uri uri = data.getData();
            if (uri == null) return;
            if (status != IDLE) {
                Toast.makeText(this, getString(R.string.error_synthesis_in_progress), Toast.LENGTH_SHORT).show();
                return;
            }
            inputText.setText(IOUtils.readAllText(input = getContentResolver().openInputStream(uri)));
            if ("file".equalsIgnoreCase(uri.getScheme())) displayName = uri.getLastPathSegment();
            else {
                String[] projection = { OpenableColumns.DISPLAY_NAME, MediaStore.Images.Media.TITLE };
                Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index >= 0 && (displayName = cursor.getString(index)) == null &&
                                (index = cursor.getColumnIndex(MediaStore.Images.Media.TITLE)) >= 0)
                            displayName = cursor.getString(index);
                    }
                    cursor.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, String.format(getString(R.string.open_error), e.getMessage()), Toast.LENGTH_LONG)
                    .show();
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        if (status != IDLE) {
            inBackground = true;
            showNotification(null);
        }
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        cancelNotification();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(this.menu = menu);
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        synthesizeMenu = menu.findItem(R.id.action_synthesize);
        return true;
    }

    private void reportProgress(int value) {
        setActionBarProgress(value);
        if (value < 0) builder.setProgress(0, 0, true);
        else builder.setProgress(getActionBarProgressMax(), value, false);
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
        synthesizeMenu.setTitle(R.string.action_stop);
        menu.setGroupEnabled(R.id.disabled_when_synthesizing, false);
        inputText.setFilters(readonlyFilters);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(inputText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        setActionBarProgressMax(inputText.getText().length());
        setActionBarSecondaryProgress(0);
        reportProgress(-1); // initializing
    }
    public void stopSynthesis() {
        TtsEngineManager.engines.selectedEngine.stop();
        synthesizeMenu.setIcon(R.drawable.ic_action_mic);
        synthesizeMenu.setTitle(R.string.action_synthesize);
        menu.setGroupEnabled(R.id.disabled_when_synthesizing, true);
        inputText.setFilters(noFilters);
        setActionBarProgress(null);
        if (descriptor != null) descriptor = null;  // pretending I'm reading the value here
        status = IDLE;
        cancelNotification();
    }
    @Override
    public void onSelectedEngineChanging() {
        stopSynthesis();
    }

    private int getStartOffset() {
        String start = TtsEngineManager.pref.getString("text.start", "beginning");
        if ("selection_start".equals(start)) return inputText.getSelectionStart();
        if ("selection_end".equals(start)) return inputText.getSelectionEnd();
        return 0;
    }

    private String getSaveFileName() {
        return displayName == null ? FileUtils.getTempFileName() : displayName;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_synthesize:
                if (status == IDLE) {
                    try {
                        status = SPEAKING;
                        startSynthesis();
                        TtsEngineManager.engines.selectedEngine.setSynthesisCallbackListener(this);
                        TtsEngineManager.engines.selectedEngine.speak(inputText.getText().toString(), getStartOffset());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, String.format(getString(R.string.synthesis_error),
                                e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                        stopSynthesis();
                    }
                } else stopSynthesis();
                return true;
            case R.id.action_synthesize_to_file: {
                String fileName = getSaveFileName() + '.' + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(TtsEngineManager.engines.selectedEngine.getMimeType());
                Intent intent;
                if (Build.VERSION.SDK_INT < 19 ||
                        TtsEngineManager.pref.getBoolean("appearance.oldTimeySaveUI", Build.VERSION.SDK_INT < 19)) {
                    intent = new Intent(this, SaveFileActivity.class);
                    String dir = TtsEngineManager.pref.getString("fileSystem.lastSaveDir", null);
                    if (dir != null) intent.putExtra(SaveFileActivity.EXTRA_CURRENT_DIRECTORY, dir);
                }
                else (intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)).addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_TITLE, fileName);
                intent.setType(TtsEngineManager.engines.selectedEngine.getMimeType());
                startActivityForResult(intent, SAVE_SYNTHESIS_CODE);
                return true;
            }
            case R.id.action_open: {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/plain");
                startActivityForResult(intent, OPEN_TEXT_CODE);
                return true;
            }
            case R.id.action_save: {
                String fileName = getSaveFileName();
                if (!fileName.toLowerCase().endsWith(".txt")) fileName += ".txt";
                Intent intent;
                if (Build.VERSION.SDK_INT < 19 ||
                        TtsEngineManager.pref.getBoolean("appearance.oldTimeySaveUI", Build.VERSION.SDK_INT < 19)) {
                    intent = new Intent(this, SaveFileActivity.class);
                    String dir = TtsEngineManager.pref.getString("fileSystem.lastSaveDir", null);
                    if (dir != null) intent.putExtra(SaveFileActivity.EXTRA_CURRENT_DIRECTORY, dir);
                }
                else (intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)).addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_TITLE, fileName);
                intent.setType("text/plain");
                startActivityForResult(intent, SAVE_TEXT_CODE);
                return true;
            }
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        switch (requestCode) {
            case OPEN_TEXT_CODE:
                onNewIntent(data);
                return;
            case SAVE_TEXT_CODE:
                OutputStream output = null;
                try {
                    (output = getContentResolver().openOutputStream(data.getData()))
                            .write(inputText.getText().toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, String.format(getString(R.string.save_error), e.getMessage()),
                            Toast.LENGTH_LONG).show();
                } finally {
                    if (output != null) try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            case SAVE_SYNTHESIS_CODE:
                try {
                    status = SYNTHESIZING;
                    startSynthesis();
                    TtsEngineManager.engines.selectedEngine.setSynthesisCallbackListener(this);
                    TtsEngineManager.engines.selectedEngine.synthesizeToStream(inputText.getText().toString(),
                            getStartOffset(), new FileOutputStream((descriptor = getContentResolver()
                                    .openFileDescriptor(data.getData(), "w")).getFileDescriptor()), getCacheDir());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, String.format(getString(R.string.synthesis_error), e.getMessage()),
                            Toast.LENGTH_LONG).show();
                    stopSynthesis();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                reportProgress(start);
                inputText.setSelection(start, end);
                inputText.moveCursorToVisibleOffset();
                if (start < inputText.getText().length()) showNotification(inputText.getText().subSequence(start, end));
                else stopSynthesis();
            }
        });
    }
    @Override
    public void onTtsSynthesisError(final int start, final int end) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (start < end)    // not failing on an empty string
                    Toast.makeText(MainActivity.this, String.format(getString(R.string.synthesis_error),
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
