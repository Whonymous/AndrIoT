package com.rosterloh.andriot.di;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.SensorManager;

import com.rosterloh.andriot.api.WeatherService;
import com.rosterloh.andriot.bluetooth.GattServer;
import com.rosterloh.andriot.db.SensorDao;
import com.rosterloh.andriot.db.SettingsDao;
import com.rosterloh.andriot.db.SettingsRepository;
import com.rosterloh.andriot.db.ThingsDb;
import com.rosterloh.andriot.db.WeatherDao;
import com.rosterloh.andriot.nearby.ConnectionsServer;

import java.io.InputStream;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
class AppModule {

    @Provides
    Context provideContext(Application application) {
        return application.getApplicationContext();
    }

    @Provides
    InputStream provideInputStream(Context context) {
        return context.getResources().openRawResource(context.
                getResources().getIdentifier("rsa_private_pkcs8", "raw", context.getPackageName()));
    }

    @Singleton
    @Provides
    WeatherService provideWeatherService() {
        return new Retrofit.Builder()
                .baseUrl("http://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherService.class);
    }

    @Singleton
    @Provides
    ThingsDb provideDb(Application app) {
        return Room.databaseBuilder(app, ThingsDb.class, "things.db")
                .addMigrations(ThingsDb.MIGRATION_2_3, ThingsDb.MIGRATION_3_4)
                .build();
    }

    @Singleton
    @Provides
    WeatherDao provideWeatherDao(ThingsDb db) {
        return db.weatherDao();
    }

    @Singleton
    @Provides
    SettingsDao provideSettingsDao(ThingsDb db) {
        return db.settingsDao();
    }

    @Singleton
    @Provides
    SensorDao provideSensorDao(ThingsDb db) {
        return db.sensorDao();
    }

    @Singleton
    @Provides
    SensorManager provideSensorManager(Application app) {
        return (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
    }

    @Singleton
    @Provides
    ConnectionsServer provideConnectionsServer(Context context, SettingsRepository settingsRepository) {
        return new ConnectionsServer(context, settingsRepository);
    }

    @Singleton
    @Provides
    GattServer provideGattServer(Context context) {
        return new GattServer(context);
    }

    /*
    @Singleton
    @Provides
    CameraController provideCameraController(Application app, SensorHub hub) {
        return new CameraController(app, hub);
    }*/
}
