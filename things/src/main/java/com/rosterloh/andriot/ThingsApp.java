package com.rosterloh.andriot;

import android.app.Activity;
import android.app.Application;

import com.google.android.things.AndroidThings;
import com.rosterloh.andriot.di.DaggerAppComponent;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import timber.log.Timber;

public class ThingsApp extends Application implements HasActivityInjector {

    @Inject
    DispatchingAndroidInjector<Activity> mDispatchingAndroidInjector;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Timber.i("Built with Things Version: " + AndroidThings.getVersionString());

        DaggerAppComponent.builder()
                .application(this)
                .build()
                .inject(this);
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return mDispatchingAndroidInjector;
    }
}
