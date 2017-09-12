package cn.rainx.demo;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.rainx.ptp.usbcamera.BaselineInitiator;
import cn.rainx.tracker.BaseInfo;
import cn.rainx.tracker.IuTracker;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    public static final String TAG = ExampleInstrumentedTest.class.getName();

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("cn.rainx.demo", appContext.getPackageName());
    }


    private  BaseInfo getBaseInfo() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        BaseInfo baseInfo = new BaseInfo(appContext, null, 1000);
        int libver = baseInfo.getLibVersion();
        Log.v(TAG, libver + "");
        Log.v(TAG, baseInfo.getAndroidOSVer() + "");
        //Log.v(TAG, baseInfo.getSerial());
        Log.v(TAG, baseInfo.getAndroidDeviceUniqueId());
        Log.v(TAG, baseInfo.getAndroidUserUniqueId() + "");
        Log.v(TAG, baseInfo.getAndroidDeviceInfo());
        return baseInfo;
    }

    @Test
    public void testReportBaseInfo() {
        BaseInfo baseinfo = getBaseInfo();
        IuTracker tracker = IuTracker.getInstance("http://10.63.255.84:8000");
        tracker.reportDeviceInfo(baseinfo);
    }

    @Test
    public void testReportPerf() {
        BaseInfo baseinfo = getBaseInfo();
        IuTracker tracker = IuTracker.getInstance("http://10.63.255.84:8000");
        tracker.reportPerf(baseinfo, null, 1024 * 1024 , System.currentTimeMillis(),
                System.currentTimeMillis() + 10, 10);
    }

    @Test
    public void testReportBug() {
        BaseInfo baseinfo = getBaseInfo();
        IuTracker tracker = IuTracker.getInstance("http://10.63.255.84:8000");
        tracker.reportBug(baseinfo, null, "PTPException", "....traceback....", "filename : xxxx");
    }

    @Test
    public void testException() {
        Exception e = new Exception("hello");
        Log.v(TAG, e.toString());
        Log.v(TAG, e.getMessage());
        Log.v(TAG, e.getClass().toString());

    }
}
