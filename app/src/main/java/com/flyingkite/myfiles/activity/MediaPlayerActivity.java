package com.flyingkite.myfiles.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.viewpager.widget.PagerTitleStrip;
import androidx.viewpager2.widget.ViewPager2;

import com.flyingkite.myfiles.R;

public class MediaPlayerActivity extends BaseActivity {

   private ViewPager2 mainPager;
   private PagerTitleStrip mainStrip;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_viewpager);
      initUI();
   }

   private void initUI() {
      mainPager = findViewById(R.id.mainViewPager);
      mainStrip = findViewById(R.id.mainStrip);
   }

   private void parseArg() {
      Intent it = getIntent();
      if (it == null) return;
      //
      it.getStringExtra("");
   }
}
