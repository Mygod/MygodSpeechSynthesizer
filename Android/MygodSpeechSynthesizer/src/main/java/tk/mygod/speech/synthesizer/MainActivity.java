package tk.mygod.speech.synthesizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import tk.mygod.speech.tts.OnTtsSynthesisCallbackListener;

/**
 * Project: Mygod Speech Synthesizer
 * Author:  Mygod (mygod.tk)
 */
public class MainActivity extends Activity implements OnTtsSynthesisCallbackListener {
    private EditText inputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputText = (EditText)findViewById(R.id.inputText);
        TtsEngineManager.init(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.synthesize:
                try {
                    TtsEngineManager.engines.selectedEngine.speak(inputText.getText().toString(), this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.stop:
                TtsEngineManager.engines.selectedEngine.stop();
                return true;
            case R.id.synthesize_to_file:
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        TtsEngineManager.engines.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onTtsSynthesisCallback(int offset, int length) {
        // TODO: implement this
    }
}
