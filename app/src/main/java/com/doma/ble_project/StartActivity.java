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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Log.d("TAG", "onCreate");


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



}
