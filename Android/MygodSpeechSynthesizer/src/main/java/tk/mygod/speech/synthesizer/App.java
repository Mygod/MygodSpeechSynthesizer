package tk.mygod.speech.synthesizer;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;

/**
 * Project: MygodSpeechSynthesizer
 *
 * @author Mygod
 */
public class App extends Application {
    static Notification.Builder builder;

    @Override
    public void onCreate() {
        TtsEngineManager.init(this);
        Intent intent = new Intent();
        intent.setAction("tk.mygod.speech.synthesizer.action.STOP");
        builder = new Notification.Builder(this).setContentTitle(getString(R.string.notification_title))
                .setAutoCancel(true).setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(this, 0, intent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notification));
    }

    @Override
    public void onTerminate() {
        TtsEngineManager.terminate();
        TtsEngineManager.engines.onDestroy();
    }
}
