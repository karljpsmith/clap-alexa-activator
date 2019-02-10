package pocketradiotheater.clapalexaactivator;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;


public class MainListener extends AppCompatActivity {
    public MediaPlayer player;
    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("OnStop ");
        stopAllListeners();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        musicalLock();
        Button buttonWAKE = (Button) findViewById(R.id.wake);
        buttonWAKE.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                triggeredReaction();
            }
        });
        Button buttonRECORD = (Button) findViewById(R.id.record);
        buttonRECORD.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                stopRecording();
                recordAudio("new_wake_word");
            }
        });
        Button buttonRESET = (Button) findViewById(R.id.reset);
        buttonRESET.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                stopRecording();
                resetRecordedFile(R.raw.alexa);
                //musicalLock();
            }
        });
        Button buttonBITCOIN = (Button) findViewById(R.id.bitcoin);
        buttonBITCOIN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                stopRecording();
                resetRecordedFile(R.raw.bitcoin_query);
                //musicalLock();
            }
        });
    }

    public void stopAllListeners(){

    }

    public void triggeredReaction() {
        if (player == null){
            playAudio();
        }
    }

    public void playAudio() {
        stopRecording();
        AudioManager mobilemode = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mobilemode.setStreamVolume(AudioManager.STREAM_MUSIC, mobilemode.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        //player = MediaPlayer.create(this, R.raw.alexa);
        File mp3File = new File(this.getFilesDir(), "/" + "new_wake_word.mp3");
        Uri mp3Uri = Uri.fromFile(mp3File);
        player = MediaPlayer.create(this, mp3Uri);
        if (player == null) {
            System.out.println("file wuz corrupted");
            player = MediaPlayer.create(this, R.raw.alexa);
        }
        player.setVolume(1, 1);
        player.start();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                player.release();
                player = null;
                musicalLock();
            }
        });
    }

    private static final int RECORDER_SAMPLERATE = 44100; //number of audio samples taken in 1 second
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isMusicLockRunning = false;
    public int mustContainToPass[] = new int[]{ 264, 330, 396};
    int BufferElements2Rec = 1024*2; // want to play 2048 (2K) since 2 bytes we use only 1024

    //int BytesPerElement = 2;//2; // 2 bytes in 16bit format
    private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
    int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

    public void musicalLock(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 4);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                               RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                               RECORDER_AUDIO_ENCODING, BufferElements2Rec);//BufferElements2Rec * BytesPerElement);
        recorder.startRecording();
        isRecording = true;
        isMusicLockRunning = true;
        recordingThread = new Thread(new Runnable() {public void run() {
            writeAudioDataToFile();
        }
    }, "AudioRecorder Thread");
        recordingThread.start();
}
public void Trigger (long timeSent, long lastTimeSent){
        long distance = (timeSent - lastTimeSent);
        if (distance > 200 && distance < 500) {
            System.out.println("DOUBLE CLAP: Distance = " + distance);
            triggeredReaction();
        }
}

    public int truncated = 200;
    public short bData[] = new short[BufferElements2Rec];
    public double realNumbers[] = new double[BufferElements2Rec];
    public double complexNumbers[] = new double[BufferElements2Rec];
    public boolean didPassMusic = false;

    public double magnitude[] = new double[truncated];
    public int magnitudeIndex[] = new int[truncated];
    int prevIndexOfMaxMagnitude;
    int checkCounter;
    public String setText;
    Handler mHandler = new Handler();
    long lastTriggeredMoment = 0;
    long triggeredMoment = 0;
    //boolean flip

    private void writeAudioDataToFile() {
        int i = 0;
        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(bData, 0, BufferElements2Rec);
            i = 0;

            //while (i < 100) {
            while (i < BufferElements2Rec) {
                realNumbers[i] = bData[i];
                complexNumbers[i] = 0;
                //magnitudeIndex[i] = i;
                if (bData[i] > 10000) { //10000 is the loudness cutoff
                    triggeredMoment = System.currentTimeMillis();
                    Trigger(triggeredMoment, lastTriggeredMoment);
                    lastTriggeredMoment = triggeredMoment;
                }
                i++;
            }

            if (!isRecording) {
                stopRecording();
            }
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
        //if (didPassMusic = true){
        //   STEP++;
    }

    public void recordAudio(String fileName) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);

        final MediaRecorder recorder2 = new MediaRecorder();
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, fileName);
        recorder2.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder2.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder2.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder2.setOutputFile(this.getFilesDir() + "/" + fileName + ".mp3");
        try {
            recorder2.prepare();
        } catch (Exception e){
            e.printStackTrace();
        }

        final ProgressDialog mProgressDialog = new ProgressDialog(MainListener.this);
        mProgressDialog.setTitle("Recording...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setButton("Stop recording", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mProgressDialog.dismiss();
                recorder2.stop();
                recorder2.release();

                musicalLock();
            }
        });

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
            public void onCancel(DialogInterface p1) {
                recorder2.stop();
                recorder2.release();
                musicalLock();
            }
        });
        recorder2.start();
        mProgressDialog.show();
    }

public void resetRecordedFile(int resource) {
    try{
        CopyRAWtoSDCard(resource, this.getFilesDir() + "/" + "new_wake_word.mp3");
    } catch (IOException e) {
        e.printStackTrace();
    }
    musicalLock();
}

    private void CopyRAWtoSDCard(int id, String path) throws IOException {
        InputStream in = getResources().openRawResource(id);
        FileOutputStream out = new FileOutputStream(path);
        byte[] buff = new byte[1024];
        int read = 0;
        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } finally {
            System.out.println("closing");
            in.close();
            out.close();
        }
    }

}
