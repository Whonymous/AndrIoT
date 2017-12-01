package com.rosterloh.andriot.sensors;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.rosterloh.andriot.db.SensorData;
import com.rosterloh.things.driver.bmx280.Bmx280;
import com.rosterloh.things.driver.bmx280.Bmx280SensorDriver;
import com.rosterloh.things.driver.ccs811.Ccs811;
import com.rosterloh.things.driver.htu21d.Htu21d;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class SensorHub {

    private static final String TAG = SensorHub.class.getSimpleName();
    private static final String I2C_BUS = "I2C1";

    private static final String LED_GPIO = "BCM27";
    private static final String BUTTON_GPIO = "BCM17";
    private static final String PIR_GPIO = "BCM21";

    private Gpio mLed;
    private Gpio mButton;
    private Gpio mPir;
    private Htu21d mHtu21d;
    private Bmx280 mBmx280;
    private Bmx280SensorDriver mBmxSensorDriver;
    private Ccs811 mCcs811;

    private MutableLiveData<Boolean> mButtonData = new MutableLiveData<>();

    private GpioCallback mButtonInterrupt = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                mButtonData.setValue(gpio.getValue());

                /*
                if (ready.get()) {
                    setLedValue(true);
                    ready.set(false);
                    camera.takePicture();
                }*/
            } catch (IOException e) {
                Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error reading button state");
                Crashlytics.getInstance().core.log(e.getMessage());
            }

            return true;
        }
    };

    private GpioCallback mPirInterrupt = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                LiveDataBus.publish(LiveDataBus.SUBJECT_MOTION_DATA, gpio.getValue());
            } catch (IOException e) {
                Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error reading PIR state");
                Crashlytics.getInstance().core.log(e.getMessage());
            }

            return true;
        }
    };

    @Inject
    public SensorHub() {

        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            mLed = pioService.openGpio(LED_GPIO);
            mLed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            mButton = pioService.openGpio(BUTTON_GPIO);
            mButton.setDirection(Gpio.DIRECTION_IN);
            mButton.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mButton.setActiveType(Gpio.ACTIVE_LOW);
            mButton.registerGpioCallback(mButtonInterrupt);

            mPir = pioService.openGpio(PIR_GPIO);
            mPir.setDirection(Gpio.DIRECTION_IN);
            mPir.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mPir.setActiveType(Gpio.ACTIVE_HIGH);
            mPir.registerGpioCallback(mPirInterrupt);
        } catch (IOException e) {
            Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error configuring sensors");
            Crashlytics.getInstance().core.log(e.getMessage());
        }

        try {
            mHtu21d = new Htu21d(I2C_BUS);
        } catch (IOException e) {
            mHtu21d = null;
            Timber.w("HTU21D failed to start or is not present for this device");
        }

        try {
            mBmx280 = new Bmx280(I2C_BUS, 0x76);
            if (mBmx280.getChipId() != Bmx280.CHIP_ID_BME280) {
                Timber.w("Unexpected device found 0x" + Integer.toHexString(mBmx280.getChipId()));
            } else {
                mBmx280.setHumidityOversampling(Bmx280.OVERSAMPLING_1X);
            }
            mBmx280.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
            mBmx280.setPressureOversampling(Bmx280.OVERSAMPLING_1X);
            mBmx280.setMode(Bmx280.MODE_NORMAL);/*
            mBmxSensorDriver = new Bmx280SensorDriver(I2C_BUS, 0x76);
            mBmxSensorDriver.registerTemperatureSensor();
            mBmxSensorDriver.registerPressureSensor();
            mBmxSensorDriver.registerHumiditySensor();*/
        } catch (IOException e) {
            mBmx280 = null;
            mBmxSensorDriver = null;
            Timber.w("BME280 failed to start or is not present for this device");
        }

        try {
            mCcs811 = new Ccs811(I2C_BUS, 0x5A);
            mCcs811.setMode(Ccs811.MODE_1S);
            Timber.d("CCS811 Boot FW: " + mCcs811.readBootVersion());
            Timber.d("CCS811 App FW: " + mCcs811.readAppVersion());
        } catch (IOException e) {
            Timber.w("CCS811 failed to start or is not present for this device");
        }
    }

    private void setLedValue(boolean value) {
        try {
            mLed.setValue(value);
        } catch (IOException e) {
            Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error updating GPIO value");
            Crashlytics.getInstance().core.log(e.getMessage());
        }
    }

    private boolean getLedValue() {
        try {
            return mLed.getValue();
        } catch (IOException e) {
            Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error getting GPIO value");
            Crashlytics.getInstance().core.log(e.getMessage());
            return false;
        }
    }

    public LiveData<Boolean> getButtonData() {
        return mButtonData;
    }

    public SensorData getSensorData() {
        try {
            float[] val1 = {0f, 0f};
            int[] val2 = {0 ,0, 0, 0};
            if (mHtu21d != null) {
                val1 = mHtu21d.readTemperatureAndHumidity();
            } else if(mBmx280 != null) {
                val1 = mBmx280.readTemperatureAndHumidity();
            }
            if (mCcs811 != null) {
                val2 = mCcs811.readAlgorithmResults();
                Timber.d("Status: " + val2[2] + " Error: " + val2[3]);

            }
            return new SensorData(val1, val2);
        } catch (IOException e) {
            Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error reading sensor data");
            Crashlytics.getInstance().core.log(e.getMessage());
        }
        return null;
    }

    @Override
    protected void finalize() throws Throwable {

        if (mButton != null) {
            mButton.unregisterGpioCallback(mButtonInterrupt);
            try {
                mButton.close();
            } catch (IOException e) {
                Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error closing BUTTON GPIO");
                Crashlytics.getInstance().core.log(e.getMessage());
            } finally {
                mButton = null;
            }
        }

        if (mPir != null) {
            mPir.unregisterGpioCallback(mPirInterrupt);
            try {
                mPir.close();
            } catch (IOException e) {
                Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error closing PIR GPIO");
                Crashlytics.getInstance().core.log(e.getMessage());
            } finally {
                mPir = null;
            }
        }

        if (mLed != null) {
            try {
                mLed.close();
            } catch (IOException e) {
                Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error closing LED GPIO");
                Crashlytics.getInstance().core.log(e.getMessage());
            } finally {
                mLed = null;
            }
        }

        if (mHtu21d != null) {
            try {
                mHtu21d.close();
            } catch (IOException e) {
                Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error closing HTU21D");
                Crashlytics.getInstance().core.log(e.getMessage());
            } finally {
                mHtu21d = null;
            }
        }

        if (mCcs811 != null) {
            try {
                mCcs811.close();
            } catch (IOException e) {
                Crashlytics.getInstance().core.log(Log.ERROR, TAG, "Error closing CCS811");
                Crashlytics.getInstance().core.log(e.getMessage());
            } finally {
                mCcs811 = null;
            }
        }

        super.finalize();
    }
}
