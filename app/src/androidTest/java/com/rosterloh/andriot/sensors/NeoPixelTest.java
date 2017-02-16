package com.rosterloh.andriot.sensors;

import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NeoPixelTest {

    private static final String TAG = NeoPixelTest.class.getSimpleName();

    @Test
    public void neoPixel_DisplayColour() {
        try {

            NeoPixel neo = new NeoPixel("SPI0.0");
            neo.setColour(255, 0 ,0); // red
            SystemClock.sleep(1000);
            neo.setColour(0, 255 ,0); // green
            SystemClock.sleep(1000);
            neo.setColour(0, 0 ,255); // blue
            SystemClock.sleep(1000);
            neo.close();
        } catch (IOException e) {
            fail("Failed to open device under test");
        }

    }
}