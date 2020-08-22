package com.example.audiolan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    AudioRecord recorder;

    TextView statusText;
    EditText serverIP, serverPort;

    public DatagramPacket packet;

    private int DEFAULT_PORT = 8200;
    private int dest_port;

    String DEFAULT_IP = "192.168.1.165";
    String dest_ip = "192.168.1.165";
    private int DEFAULT_SAMPLE_RATE = 44100;
    private int SAMPLE_RATE = 44100;
    public int sampleRate;
    private int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int BUFFER_SIZE = AudioRecord.getMinBufferSize(44100, CHANNEL_CONFIG, ENCODING);

    private boolean status = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Spinner spinner = findViewById(R.id.sampleRateSpinner);
        final Button buttonStart = findViewById(R.id.buttonStart);
        final Button buttonStop = findViewById(R.id.buttonStop);
        serverIP = findViewById(R.id.textServerInput);
        serverPort = findViewById(R.id.textPortInput);
        statusText = findViewById(R.id.textStatusUpdate);

        buttonStart.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("onClick","buttonStart clicked." );
                status = true;
                statusText.setText(R.string.statusRunning);
                startRecording();
            }
        });

        buttonStop.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("onClick","buttonStop clicked." );
                status = false;
                statusText.setText(R.string.statusNotRunning);
                recorder.release();
            }
        });

        serverIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                dest_ip = serverIP.getText().toString();
            }
        });

        serverPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                dest_port = Integer.parseInt(serverPort.getText().toString());
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String sampleRate = adapterView.getItemAtPosition(i).toString();
                MainActivity.this.sampleRate = Integer.parseInt(sampleRate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                sampleRate = DEFAULT_SAMPLE_RATE;
            }
        });

        String[] items = new String[]{"8000", "11025", "16000", "22050", "44100", "48000", "88200", "96000", "192000"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);

//        getAudioInfo();
//        getValidSampleRates();
    }

    private void checkRecordPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 123);
        }
    }

    public void getAudioInfo() {
        System.out.println("Info");
    }

    public void getValidSampleRates() {
        for (int rate : new int[] {8000, 11025, 16000, 22050, 44100, 48000, 88200, 96000, 192000}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            System.out.printf("\n%d : %d\n", rate, bufferSize);
        }
    }

    public void startRecording() {
        Thread streamThread;
        streamThread = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                try {
                    if (sampleRate == 0) {
                        sampleRate = DEFAULT_SAMPLE_RATE;
                        Log.d("START", "Using default sample rate: " + sampleRate);
                    }
                    else { Log.d("START", "Using sample rate: " + sampleRate); }

                    if (dest_port == 0) {
                        dest_port = DEFAULT_PORT;
                        serverPort.setText(Integer.toString(DEFAULT_PORT));
                        Log.d("START", "Using default port: " + dest_port);
                    }
                    else { Log.d("START", "Using port: " + dest_port); }

                    if (dest_ip.length() == 0) {
                        dest_ip = DEFAULT_IP;
                        serverIP.setText(DEFAULT_IP);
                        Log.d("START", "Using default IP: " + dest_ip);
                    }
                    else { Log.d("START", "Using IP: " + dest_ip); }

                    Log.d("THREAD", "Starting streamThread.");
                    DatagramSocket socket = new DatagramSocket();
                    InetAddress dest_addr = InetAddress.getByName(dest_ip);
                    int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, ENCODING);
                    byte[] buffer = new byte[minBufferSize];

                    AudioRecord.Builder builder = new AudioRecord.Builder();
                    builder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    builder.setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(ENCODING)
                            .setSampleRate(sampleRate)
                            .setChannelMask(CHANNEL_CONFIG)
                            .build());
                    builder.setBufferSizeInBytes(2 * minBufferSize);

                    recorder = builder.build();

                    recorder.startRecording();

                    while (status) {
                        BUFFER_SIZE = recorder.read(buffer, 0, buffer.length);
                        packet = new DatagramPacket(buffer, buffer.length, dest_addr, dest_port);
                        socket.send(packet);
                        Log.d("BUFFER", "Packet size: " + buffer.length);
                    }

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.d("THREAD", "UnknownHostException");

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("THREAD", "IOException");
                }
            }
        });
        streamThread.start();
    }
}