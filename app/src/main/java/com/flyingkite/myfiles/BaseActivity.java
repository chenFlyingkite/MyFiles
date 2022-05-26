package com.flyingkite.myfiles;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import flyingkite.library.android.log.Loggable;

public class BaseActivity extends AppCompatActivity implements Loggable {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate(%s)", savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy()");
    }
}
