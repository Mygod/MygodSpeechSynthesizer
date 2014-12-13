package tk.mygod.speech.synthesizer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.melnykov.fab.FloatingActionButton;
import org.xml.sax.SAXException;
import tk.mygod.CurrentApp;
import tk.mygod.app.SaveFileActivity;
import tk.mygod.speech.tts.TtsEngine;
import tk.mygod.text.SsmlDroid;
import tk.mygod.util.FileUtils;
import tk.mygod.util.IOUtils;
import tk.mygod.widget.ObservableScrollView;
import tk.mygod.widget.ScrollViewListener;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author  Mygod
 */
public class MainActivity extends Activity implements TtsEngine.OnTtsSynthesisCallbackListener,
        TtsEngineManager.OnSelectedEngineChangingListener, Toolbar.OnMenuItemClickListener {
    private static final int OPEN_TEXT_CODE = 0, SAVE_TEXT_CODE = 1, SAVE_SYNTHESIS_CODE = 2,
                             IDLE = 0, SPEAKING = 1, SYNTHESIZING = 2;
    private ProgressBar progressBar;
    private EditText inputText;
    private Menu menu;
    private MenuItem styleItem;
    private FloatingActionButton fab;
    private int status, selectionStart, selectionEnd;
    private boolean inBackground;
    private static final InputFilter[] noFilters = new InputFilter[0],
            readonlyFilters = new InputFilter[] { new InputFilter() {
                public CharSequence filter(CharSequence src, int start, int end, Spanned dest, int dstart, int dend) {
                    return dest.subSequence(dstart, dend);
                }
            } };
    private SsmlDroid.Parser parser;
    private ParcelFileDescriptor descriptor;    // used to keep alive from GC
    private Notification.Builder builder;

    private String lastText, displayName;
    @SuppressWarnings("deprecation")
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

    private String formatDefaultText(String pattern, Date buildTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(buildTime);
        return String.format(pattern, CurrentApp.getVersionName(this),
                DateFormat.getDateInstance(DateFormat.FULL).format(buildTime),
                DateFormat.getTimeInstance(DateFormat.FULL).format(buildTime), calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.DAY_OF_WEEK),
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.main_activity_actions);
        styleItem = (menu = toolbar.getMenu()).findItem(R.id.action_style);
        toolbar.setOnMenuItemClickListener(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        ((ObservableScrollView) findViewById(R.id.scroller)).setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                if (y > oldy) fab.hide();
                else if (y < oldy) fab.show();
            }
        });
        TtsEngineManager.init(this, this);
        Date buildTime = CurrentApp.getBuildTime(this);
        inputText = (EditText) findViewById(R.id.input_text);
        boolean failed = true;
        if (TtsEngineManager.getEnableSsmlDroid()) try {
            inputText.setText(formatDefaultText(IOUtils.readAllText(getResources()
                    .openRawResource(R.raw.input_text_default)), buildTime));
            failed = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (failed) inputText.setText(formatDefaultText(getText(R.string.input_text_default).toString(), buildTime));
        Intent intent = new Intent();
        intent.setAction("tk.mygod.speech.synthesizer.action.STOP");
        builder = new Notification.Builder(this).setContentTitle(getString(R.string.notification_title))
                .setAutoCancel(true).setSmallIcon(R.drawable.ic_communication_message)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(this, 0, intent, 0));
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v != inputText) return;
        getMenuInflater().inflate(R.menu.input_text_styles, menu);
        menu.setHeaderTitle("Style...");    // todo: localize!
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        CharSequence source = inputText.getText();
        StyleIdParser parser = new StyleIdParser(item, source.subSequence(selectionStart, selectionEnd));
        if (parser.Tag == null) return super.onContextItemSelected(item);
        inputText.setTextKeepState(source.subSequence(0, selectionStart) + parser.Tag +
                source.subSequence(selectionEnd, source.length()));
        inputText.setSelection(selectionStart + parser.Selection);
        if (parser.Toast != null) Toast.makeText(this, parser.Toast, Toast.LENGTH_LONG).show();
        return true;
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
        styleItem.setVisible(TtsEngineManager.getEnableSsmlDroid());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_style:
                selectionStart = inputText.getSelectionStart();
                selectionEnd = inputText.getSelectionEnd();
                registerForContextMenu(inputText);
                openContextMenu(inputText);
                unregisterForContextMenu(inputText);
                return true;
            case R.id.action_synthesize_to_file: {
                String fileName = getSaveFileName() + '.' + MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(TtsEngineManager.engines.selectedEngine.getMimeType());
                Intent intent;
                if (TtsEngineManager.getOldTimeySaveUI()) {
                    intent = new Intent(this, SaveFileActivity.class);
                    String dir = TtsEngineManager.getLastSaveDir();
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
                if (TtsEngineManager.getOldTimeySaveUI()) {
                    intent = new Intent(this, SaveFileActivity.class);
                    String dir = TtsEngineManager.getLastSaveDir();
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
        return false;
    }

    private void startSynthesis() {
        TtsEngineManager.engines.selectedEngine
                .setPitch(Float.parseFloat(TtsEngineManager.pref.getString("tweaks.pitch", "1")));
        TtsEngineManager.engines.selectedEngine
                .setSpeechRate(Float.parseFloat(TtsEngineManager.pref.getString("tweaks.speechRate", "1")));
        TtsEngineManager.engines.selectedEngine
                .setPan(Float.parseFloat(TtsEngineManager.pref.getString("tweaks.pan", "0")));
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_mic));
        menu.setGroupEnabled(R.id.disabled_when_synthesizing, false);
        inputText.setFilters(readonlyFilters);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(inputText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        builder.setProgress(0, 0, true);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(inputText.getText().length());
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
    }
    public void stopSynthesis() {
        TtsEngineManager.engines.selectedEngine.stop();
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_mic_none));
        menu.setGroupEnabled(R.id.disabled_when_synthesizing, true);
        inputText.setFilters(noFilters);
        progressBar.setVisibility(View.INVISIBLE);
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

    private CharSequence getText() throws IOException, SAXException {
        String text = inputText.getText().toString(), temp = text.replaceAll("\r", ""); // I hate \r!!!1!
        if (!text.equals(temp)) inputText.setText(temp);                                // & u'd better not ask y
        return TtsEngineManager.getEnableSsmlDroid()
                ? (parser = SsmlDroid.fromSsml(text,
                    TtsEngineManager.getIgnoreSingleLineBreak(), null)).Result
                : inputText.getText().toString().replaceAll("(?<!\\n)(\\n)(?!\\n)", " ");
    }

    public void synthesize(View view) {
        if (status == IDLE) {
            try {
                status = SPEAKING;
                startSynthesis();
                TtsEngineManager.engines.selectedEngine.setSynthesisCallbackListener(this);
                TtsEngineManager.engines.selectedEngine.speak(getText(), getStartOffset());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, String.format(getString(R.string.synthesis_error),
                        e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                stopSynthesis();
            }
        } else stopSynthesis();
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
                    TtsEngineManager.engines.selectedEngine.synthesizeToStream(getText(), getStartOffset(),
                            new FileOutputStream((descriptor = getContentResolver()
                                    .openFileDescriptor(data.getData(), "w")).getFileDescriptor()), getCacheDir());
                } catch (Exception e) {
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
    public void onTtsSynthesisPrepared(int e) {
        if (parser != null) e = parser.getSsmlOffset(e, true);
        final int end = e;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setSecondaryProgress(end);
            }
        });
    }
    @Override
    public void onTtsSynthesisCallback(int s, int e) {
        if (parser != null) {
            s = parser.getSsmlOffset(s, false);
            e = parser.getSsmlOffset(e, true);
        }
        if (e < s) e = s;
        final int start = s, end = e;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status != IDLE) {
                    progressBar.setProgress(start);
                    progressBar.setIndeterminate(false);
                }
                builder.setProgress(progressBar.getMax(), start, false);
                inputText.setSelection(start, end);
                inputText.moveCursorToVisibleOffset();
                if (start < inputText.getText().length()) showNotification(inputText.getText().subSequence(start, end));
                else stopSynthesis();
            }
        });
    }
    @Override
    public void onTtsSynthesisError(int s, int e) {
        if (parser != null) {
            s = parser.getSsmlOffset(s, false);
            e = parser.getSsmlOffset(e, true);
        }
        if (e < s) e = s;
        final int start = s, end = e;
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
        stopSynthesis();
        TtsEngineManager.engines.onDestroy();
        super.onDestroy();
    }
}
