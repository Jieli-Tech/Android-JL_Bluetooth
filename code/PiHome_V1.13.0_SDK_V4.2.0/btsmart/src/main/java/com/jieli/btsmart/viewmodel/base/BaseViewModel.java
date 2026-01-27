package com.jieli.btsmart.viewmodel.base;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.jieli.btsmart.MainApplication;

/**
 * ViewModel基类
 */
public class BaseViewModel extends ViewModel {
    protected String tag = getClass().getSimpleName();

    public Context getContext(){
        return MainApplication.getApplication();
    }
}
