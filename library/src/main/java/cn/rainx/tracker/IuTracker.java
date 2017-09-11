package cn.rainx.tracker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.Preference;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONObject;

import cn.rainx.ptp.usbcamera.BaselineInitiator;
import cn.rainx.ptp.usbcamera.PTPException;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.annotation.NotThreadSafe;

import static android.util.Log.*;

/**
 * Created by rainx on 2017/8/20.
 */

public class IuTracker {

    public static final String TAG = IuTracker.class.getSimpleName();

    private boolean bugTrackerEnabled = true;
    private boolean perfTrackerEnabled = true;
    private boolean deviceInfoTrackerEnabled = true;
    private String restBaseUri;
    private static volatile IuTracker _instance= null;

    /**
     * 获取单例
     * @param restBaseUri rest服务的基础uri 如 http://127.0.0.1:8000
     * @return
     */
    public static synchronized IuTracker getInstance(String restBaseUri) {
        if (_instance == null) {
            _instance = new IuTracker(restBaseUri);
        }
        return _instance;
    }

    public static AsyncHttpClient syncHttpClient= new SyncHttpClient();
    public static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();


    private IuTracker(String restBaseUri) {
        this.restBaseUri = restBaseUri;

    }

    public void setBugTrackerEnabled(boolean bugTrackerEnabled) {
        this.bugTrackerEnabled = bugTrackerEnabled;
    }

    public void setPerfTrackerEnabled(boolean perfTrackerEnabled) {
        this.perfTrackerEnabled = perfTrackerEnabled;
    }

    public void setDeviceInfoTrackerEnabled(boolean deviceInfoTrackerEnabled) {
        this.deviceInfoTrackerEnabled = deviceInfoTrackerEnabled;
    }

    public void setRestBaseUri(String restBaseUri) {
        int len = restBaseUri.length();
        if (restBaseUri.substring(len - 1, len).equals("/")) {
            restBaseUri = restBaseUri.substring(0, len - 1);
        }
        this.restBaseUri = restBaseUri;
    }


    /**
     * 向服务器报告设备信息，一个客户端只会报告一次
     * @param baseInfo
     */
    public void reportDeviceInfo(BaseInfo baseInfo) {
        // need implement
        // 1 持久化确保改接口对于一个手机只被运行一次
        // 检测是否已经report 过

        if (!deviceInfoTrackerEnabled) {
            return;
        }

        final String key = "Already_Reported_DeviceInfo";

        final SharedPreferences sp = baseInfo.getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        if (!sp.getBoolean(key, false)) {

            String url = restBaseUri + "/device_info_tracker/";

            RequestParams params =  baseInfo.getParams();

            getClient().post(url, params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.i(TAG, "done!");
                    Log.d(TAG, response.toString());
                    sp.edit().putBoolean(key, true);
                    sp.edit().commit();
                }

                @Override
                public void onFailure(int statusCode , Header[] headers, Throwable e, JSONObject response) {
                    e.printStackTrace();
                }
            });


        }

    }

    /**
     * 向服务器报告文件传输速度
     * @param baseInfo 基础信息
     * @param initiator USB Initiator 设备
     * @param filesize 文件大小
     * @param startTs 开始时间
     * @param endTs 结束时间
     * @param costTs 消耗时间
     */
    public void reportPerf(BaseInfo baseInfo, BaselineInitiator initiator,
                           int filesize, float startTs, float endTs, float costTs) {


        if (!perfTrackerEnabled) {
            return;
        }

        String url = restBaseUri + "/perf_tracker/";

        RequestParams params =  baseInfo.getParams();
        if (initiator != null) {
            params.put("sync_trigger_mode", initiator.getSyncTriggerMode());
            params.put("sync_mode", initiator.getSyncMode());
            params.put("sync_record_mode", initiator.getSyncRecordMode());
        } else {
            params.put("sync_trigger_mode", -1);
            params.put("sync_mode", -1);
            params.put("sync_record_mode", -1);
        }
        params.put("filesize", filesize);
        params.put("start_ts", startTs);
        params.put("end_ts", endTs);
        params.put("cost_ts", costTs);

        getClient().post(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "done!");
                Log.d(TAG, response.toString());
            }

            @Override
            public void onFailure(int statusCode , Header[] headers, Throwable e, JSONObject response) {
                e.printStackTrace();
            }
        });

    }

    /**
     * 向服务器报告bug
     * @param baseInfo 基础信息
     * @param initiator USB Initiator 设备
     * @param exception 异常信息
     * @param traceback 调用堆栈
     * @param fileinfo 正在传输的文件信息
     */
    public void reportBug(BaseInfo baseInfo, BaselineInitiator initiator, String exception,
                          String traceback, String fileinfo) {

        if (!bugTrackerEnabled) {
            return;
        }

        String url = restBaseUri + "/bug_tracker/";
        RequestParams params =  baseInfo.getParams();
        if (initiator != null) {
            params.put("sync_trigger_mode", initiator.getSyncTriggerMode());
            params.put("sync_mode", initiator.getSyncMode());
            params.put("sync_record_mode", initiator.getSyncRecordMode());
        } else {
            params.put("sync_trigger_mode", -1);
            params.put("sync_mode", -1);
            params.put("sync_record_mode", -1);
        }
        params.put("exception", exception);
        params.put("traceback", traceback);
        params.put("fileinfo", fileinfo);

        getClient().post(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "done!");
                Log.d(TAG, response.toString());
            }

            @Override
            public void onFailure(int statusCode , Header[] headers, Throwable e, JSONObject response) {
                e.printStackTrace();
            }
        });
    }

    /**
     * @return an async client when calling from the main thread, otherwise a sync client.
     */
    private static AsyncHttpClient getClient()
    {
        // Return the synchronous HTTP client when the thread is not prepared
        if (Looper.myLooper() == null)
            return syncHttpClient;
        return asyncHttpClient;
    }

}
