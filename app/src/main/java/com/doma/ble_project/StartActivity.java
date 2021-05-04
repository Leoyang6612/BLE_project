package com.doma.ble_project;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class StartActivity extends Activity {
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

    protected void onResume() {
        super.onResume();
        Log.d("TAG", "onResume");
    }

    protected void onRestart() {
        super.onRestart();
        Log.d("TAG", "onRestart");
    }

    protected void onPause() {
        super.onPause();
        Log.d("TAG", "onPause");
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
