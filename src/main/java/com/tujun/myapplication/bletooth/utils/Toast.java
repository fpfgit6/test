package com.tujun.myapplication.bletooth.utils;

import android.content.Context;

/**
 * Created by Administrator on 2017/4/12.
 */

public class Toast {
    public static void makeText(Context context,String s){
        android.widget.Toast.makeText(context,s, android.widget.Toast.LENGTH_LONG).show();
    }
}
