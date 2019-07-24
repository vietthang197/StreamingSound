package com.example.streamingsound;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText edtIp, edtPort;
    private Button btnStartMicro, btnStopMicro;

    private AudioRecord recorder;

    // config audio record
    private int sampleRate = 16000 ;
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioFormat  = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = true;

    private Thread streamThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        }

        initView();
        initEvents();
    }

    private void initEvents() {
        btnStartMicro.setOnClickListener(this);
        btnStopMicro.setOnClickListener(this);
    }

    private void initView() {
        edtPort = findViewById(R.id.edtPort);
        edtIp = findViewById(R.id.edtIp);
        btnStartMicro = findViewById(R.id.btnStartMicro);
        btnStopMicro = findViewById(R.id.btnStopMicro);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStartMicro:
                doRecordAudio();
                break;
            case R.id.btnStopMicro:
                doStopRecord();
                break;
        }
    }

    private void doStopRecord() {
        if (recorder != null) {
            status = false;
            recorder.release();
            recorder = null;
            Log.d("VS","Recorder released");
            Toast.makeText(getApplicationContext(), "Record stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void doRecordAudio() {
        if (recorder == null ) {
            status = true;

            if (streamThread == null) {
                streamThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DatagramSocket dataSocket = new DatagramSocket();
                            Log.d("SOCKET", "Socket Created");

                            byte[] buffer = new byte[minBufSize];

                            Log.d("BUFFER", String.valueOf(minBufSize));
                            DatagramPacket packet;

                            final InetAddress destination = InetAddress.getByName(edtIp.getText().toString());

                            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize*10);

                            recorder.startRecording();

                            Log.d("Socket", "socket connected");

                            int port = Integer.parseInt(edtPort.getText().toString());

                            while(status == true) {


                                //reading data from MIC into buffer
                                minBufSize = recorder.read(buffer, 0, buffer.length);

                                //putting buffer in the packet
                                packet = new DatagramPacket (buffer,buffer.length, destination,port);

                                dataSocket.send(packet);
                                Log.d("Data stream", "MinBufferSize: " + minBufSize);


                            }

                        } catch (SocketException e) {
                            e.printStackTrace();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                streamThread.start();
            } else {
                status = true;
            }


        }
    }
}
