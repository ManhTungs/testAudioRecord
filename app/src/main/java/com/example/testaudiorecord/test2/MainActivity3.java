package com.example.testaudiorecord.test2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.testaudiorecord.R;

public class MainActivity3 extends AppCompatActivity {
    Button btnStart,btnStop,btnPause,btnResume,btnReplay,btnMerge;


    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result != null) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
//                            initAndStart(result.getData(), result.getResultCode());
                            startScreenRecord(result.getResultCode(),result.getData());
                        }else {
                            finish();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        btnStart=findViewById(R.id.btn_start);
        btnStop=findViewById(R.id.btn_stop);

        btnPause=findViewById(R.id.btn_pause);
        btnResume=findViewById(R.id.btn_resume);

        btnReplay=findViewById(R.id.btn_replay);
        btnMerge=findViewById(R.id.btn_merge);
        btnStart.setOnClickListener(v -> {
            requestScreenRecord();
        });

        btnStop.setOnClickListener(v -> {
            Intent intent=new Intent(this, Service3.class);
            intent.setAction("PAUSE");
            startService(intent);
        });

        btnPause.setOnClickListener(v -> {
            Intent intent=new Intent(this, Service3.class);
            intent.setAction("RESUME");
            startService(intent);
        });

        btnResume.setOnClickListener(v -> {
            Intent intent=new Intent(this, Service3.class);
            intent.setAction("STOP");
            startService(intent);
        });

        btnReplay.setOnClickListener(v -> {
            Intent intent=new Intent(this, Service3.class);
            intent.setAction("PLAY_AUDIO");
            startService(intent);
        });

        btnMerge.setOnClickListener(v -> {
            Intent intent=new Intent(this, Service3.class);
            intent.setAction("MERGE");
            startService(intent);
        });


    }


    private void requestScreenRecord() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        activityResultLauncher.launch(permissionIntent);
    }

    private void startScreenRecord(int requestCode,Intent data) {
        Intent intent=new Intent(this, Service3.class);
        intent.putExtra("request_code",requestCode);
        intent.putExtra("data",data);
        intent.setAction("START");
        startService(intent);
    }
}
