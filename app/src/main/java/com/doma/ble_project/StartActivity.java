package com.doma.ble_project;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class StartActivity extends Activity{
    String data = "0";
    String IDnum = "0";

    //ryan tfLite --------------------------------------------------

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 2000; //time
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    private static final long AVERAGE_WINDOW_DURATION_MS = 2000; //time
    private static final float DETECTION_THRESHOLD = 0.50f;
    private static final int SUPPRESSION_MS = 1500;
    private static final int MINIMUM_COUNT = 3;
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 300;
    private static final String LABEL_FILENAME = "file:///android_asset/label.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/model_5class0517.tflite";

    // UI elements.
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final String LOG_TAG = StartActivity.class.getSimpleName();

    // Working variables.
    short[] recordingBuffer = new short[RECORDING_LENGTH];
    int recordingOffset = 0;
    boolean shouldContinue = true;
    private Thread recordingThread;
    boolean shouldContinueRecognition = true;
    private Thread recognitionThread;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private final ReentrantLock tfLiteLock = new ReentrantLock();

    private List<String> labels = new ArrayList<String>();
    private List<String> displayedLabels = new ArrayList<>();
    private RecognizeCommands recognizeCommands = null;

    private final Interpreter.Options tfLiteOptions = new Interpreter.Options();
    private MappedByteBuffer tfLiteModel;
    private Interpreter tfLite;

    private Button
            startButton,
            stopButton;

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    //ryan tfLite --------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Log.d("TAG", "onCreate");

        //ryan tfLite --------------------------------------------------

        // Load the labels for the model, but only display those that don't start
        // with an underscore.
        String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
        Log.i(LOG_TAG, "Reading labels from: " + actualLabelFilename);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(actualLabelFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                Log.d("label", line);

                labels.add(line);
                if (line.charAt(0) != '_') {

                    displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
                }
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!", e);
        }

        // Set up an object to smooth recognition results to increase accuracy.
        recognizeCommands =
                new RecognizeCommands(
                        labels,
                        AVERAGE_WINDOW_DURATION_MS,
                        DETECTION_THRESHOLD,
                        SUPPRESSION_MS,
                        MINIMUM_COUNT,
                        MINIMUM_TIME_BETWEEN_SAMPLES_MS);

        String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
        try {
            tfLiteModel = loadModelFile(getAssets(), actualModelFilename);
            Log.d("LOG_TAG", "load model");
            recreateInterpreter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Start the recording and recognition threads.
        requestMicrophonePermission();

        startButton = findViewById(R.id.button_start_record);
        stopButton = findViewById(R.id.button_stop_record);

        stopButton.setEnabled(false);
        startButton.setEnabled(true);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopButton.setEnabled(true);
                startButton.setEnabled(false);
                startRecording();
                startRecognition();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                stopRecording();
                stopRecognition();
            }
        });

        //ryan tfLite --------------------------------------------------

    }

    protected void onStart() {
        super.onStart();
        Log.d("TAG", "onStart");
    }

    protected void onRestart() {
        super.onRestart();
        Log.d("TAG", "onRestart");
    }

    protected void onPause() {
        super.onPause();
        Log.d("TAG", "onPause");
    }

    protected void onResume() {
        super.onResume();
        Log.d("TAG", "onResume");
    }

    protected void onStop() {
        super.onStop();
        Log.d("TAG", "onStop");
    }

    protected void onDestory() {
        super.onResume();
        Log.d("TAG", "onDestory");
    }

    public void buttonOnClick(View view) {
        //Toast toast = Toast.makeText(this, "starting", Toast.LENGTH_SHORT);
        //toast.show();;
        Log.d("TAG", "Click");
        Intent intent = new Intent();
        intent.setClass(StartActivity.this, ScanActivity.class);

        //使用Intent類別所提供的putExtra方法，在轉換Activity之前，將資料（本例為bmi）放進去Intent物件中
        intent.putExtra(ScanActivity.ID_NAME, data);

        //在一個Activity中可以使用startActivity方法，將一個intent物件發送至Android系統中，由Android系統判別，判別後由系統將我們的ResultActivity(此處=ScanActivity)顯示在畫面上，
        startActivity(intent);

    }

    public void IDLoginOnClick(View view) {
        final View item = LayoutInflater.from(StartActivity.this).inflate(R.layout.alertdialog_layout, null);
        new AlertDialog.Builder(StartActivity.this)
                .setTitle("請輸入ID(1-9)")
                .setView(item)
                .setNeutralButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Cancel", Toast.LENGTH_SHORT).show();
                    }

                })
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText editText = (EditText) item.findViewById(R.id.edit_text);
                        if (editText.getText().toString().length() != 0) {

                            IDnum = editText.getText().toString();

                            if (IDnum.equals("1") || IDnum.equals("2") || IDnum.equals("3") || IDnum.equals("4") || IDnum.equals("5")
                                    || IDnum.equals("6") || IDnum.equals("7") || IDnum.equals("8") || IDnum.equals("9")) {
                                data = editText.getText().toString();
                                Toast.makeText(getApplicationContext(), "Set ID successfully ! Click Start", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "ID is between 1-9 :", Toast.LENGTH_SHORT).show();
                            }


                        }
                        if (TextUtils.isEmpty(data)) {
                            Toast.makeText(getApplicationContext(), "NO ID input  \n ID :" + data, Toast.LENGTH_SHORT).show();
                        } else {

                            // Toast.makeText(getApplicationContext(), "ID :" + data, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    //ryan tfLite --------------------------------------------------

    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[] {android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Log.d("Permissions","REQUEST_RECORD_AUDIO");
            //startRecording();
            //startRecognition();
        }
    }

    public synchronized void startRecording() {
        if (recordingThread != null) {
            return;
        }
        shouldContinue = true;
        recordingThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                record();
                            }
                        });
        recordingThread.start();
    }

    public synchronized void stopRecording() {
        if (recordingThread == null) {
            return;
        }
        shouldContinue = false;
        recordingThread = null;
    }

    private void record() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // Estimate the buffer size we'll need for this device.
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }

        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {

            //Log.d("record","recording");

            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            int maxLength = recordingBuffer.length;
            int newRecordingOffset = recordingOffset + numberRead;
            int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
            int firstCopyLength = numberRead - secondCopyLength;
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock();
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                recordingOffset = newRecordingOffset % maxLength;
            } finally {
                recordingBufferLock.unlock();
            }
        }

        record.stop();
        record.release();
    }

    public synchronized void startRecognition() {
        if (recognitionThread != null) {
            return;
        }
        shouldContinueRecognition = true;
        recognitionThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                recognize();
                            }
                        });
        recognitionThread.start();
    }

    public synchronized void stopRecognition() {
        if (recognitionThread == null) {
            return;
        }
        shouldContinueRecognition = false;
        recognitionThread = null;
    }

    private void recognize() {

        Log.v(LOG_TAG, "Start recognition");

        short[] inputBuffer = new short[RECORDING_LENGTH];
        float[][] floatInputBuffer = new float[1][RECORDING_LENGTH];
        final float[][] outputScores = new float[1][labels.size()];



        // Loop, grabbing recorded data and running the recognition model on it.
        while (shouldContinueRecognition) {
            long startTime = new Date().getTime();
            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock();
            try {
                int maxLength = recordingBuffer.length;
                int firstCopyLength = maxLength - recordingOffset;
                int secondCopyLength = recordingOffset;
                System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
                System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
            } finally {
                recordingBufferLock.unlock();
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (int i = 0; i < RECORDING_LENGTH; ++i) {
                floatInputBuffer[0][i] = inputBuffer[i] / 32767.0f;
            }

            Object[] inputArray = {floatInputBuffer};


            Map<Integer, Object> outputMap = new HashMap<>();
            outputMap.put(0, outputScores);

            // Run the model.
            tfLiteLock.lock();
            try {

                tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

            } finally {
                tfLiteLock.unlock();
            }

            // Use the smoother to figure out if we've had a real recognition event.
            long currentTime = System.currentTimeMillis();
            final RecognizeCommands.RecognitionResult result =
                    recognizeCommands.processLatestResults(outputScores[0], currentTime);
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {

                            String showtext = null;

                            // If we do have a new command, highlight the right list entry.
                            if (!result.foundCommand.startsWith("_") && result.isNewCommand) {
                                int labelIndex = -1;
                                for (int i = 0; i < labels.size(); ++i) {
                                    if (labels.get(i).equals(result.foundCommand)) {
                                        labelIndex = i;
                                    }
                                }

                                for(int i = 0;i < labels.size();i++)
                                {
                                    Log.d(String.valueOf(i), "scores: " + String.valueOf(outputScores[0][i]));
                                }
                                Log.d("labelIndex",String.valueOf(labelIndex));

                                //new label
                                switch (labelIndex - 2) {
                                    case 0:
                                        showtext = "applause";
                                        break;
                                    case 1:
                                        showtext = "cat";
                                        break;
                                    case 2:
                                        showtext = "dog_barking";
                                        break;
                                    case 3:
                                        showtext = "hammer";
                                        break;
                                    case 4:
                                        showtext = "laughter";
                                        break;
                                }


                                if(showtext != null){
                                    Toast.makeText(StartActivity.this, showtext, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
            try {
                // We don't need to run too frequently, so snooze for a bit.
                Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        Log.v(LOG_TAG, "End recognition");
    }

    private void recreateInterpreter() {
        tfLiteLock.lock();
        try {
            if (tfLite != null) {
                tfLite.close();
                tfLite = null;
            }
            //tfLite = new Interpreter(tfLiteModel, tfLiteOptions);
            tfLite = new Interpreter(tfLiteModel, tfLiteOptions);

            tfLite.resizeInput(0, new int[] {RECORDING_LENGTH, 1});
            //tfLite.resizeInput(1, new int[] {1});
        } finally {
            tfLiteLock.unlock();
        }
    }


    //ryan tfLite --------------------------------------------------

}
