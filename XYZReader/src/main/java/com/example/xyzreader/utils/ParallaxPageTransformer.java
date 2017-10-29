package com.example.xyzreader.utils;

import android.support.v4.view.ViewPager;
import android.view.View;

import com.example.xyzreader.R;

public class ParallaxPageTransformer implements ViewPager.PageTransformer {
    private static final int FAB_ROTATION_DEGREES = -90;

    public void transformPage(View view, float position) {
        if (position >= -1 && position <= 1) {
            view.findViewById(R.id.photo).setTranslationX(-position * view.getWidth() / 2);
            view.findViewById(R.id.share_fab).setRotation(position * (180 - FAB_ROTATION_DEGREES));
        } else {
            view.setAlpha(1);
            view.setRotation(0);
        }
    }
}