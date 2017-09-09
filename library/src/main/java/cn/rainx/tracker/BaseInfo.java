package cn.rainx.tracker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.util.Log;

import com.loopj.android.http.RequestParams;

import java.util.List;
import java.util.UUID;

import cn.rainx.exif.ExifUtils;
import cn.rainx.ptp.db.Uuid;
import cn.rainx.ptp.usbcamera.BaselineInitiator;

/**
 * Created by rainx on 2017/8/26.
 */

public class BaseInfo {

    private int vendorId;
    private int deviceId;
    private String serial;
    private String androidDeviceUniqueId;
    private Integer androidUserUniqueId;
    private String androidDeviceInfo;
    private int androidOSVer;
    private int libVersion;
    private String deviceInfo = "empty";
    private  Context context;

    public static final String TAG = "BaseInfo";


    public BaseInfo(Context context, BaselineInitiator initiator, Integer userUniqueId)
    {
        this.context = context;

        libVersion = initLibVersion();
        androidDeviceUniqueId = initAndroidDevicerUniqueId();
        androidOSVer = initAndroidOSVer();
        androidUserUniqueId = userUniqueId;

        androidDeviceInfo = Build.DEVICE.toString();


        Log.d(TAG, "get android os ver " + androidOSVer);
        Log.d(TAG, "android uuid is " + androidDeviceUniqueId);
        Log.d(TAG, "libversion is " + libVersion);
        if (initiator != null) {
            UsbDevice device = initiator.getDevice();

            if (device != null) {
                vendorId = device.getVendorId();
                deviceId = device.getDeviceId();
                serial = device.getSerialNumber();
            } else {
                vendorId = -1;
                deviceId = -1;
                serial = "empty";
            }
            try {
                deviceInfo = initiator.getDeviceInfo().toString();
            } catch (Exception e) {
                e.printStackTrace();
                // ignore it
                deviceInfo = "Exception when get device info";
            }

        } else {
            vendorId = -1;
            deviceId = -1;
            serial = "empty";

        }

    }

    public Context getContext() {
        return context;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getAndroidDeviceUniqueId() {
        return androidDeviceUniqueId;
    }

    public void setAndroidDeviceUniqueId(String androidDeviceUniqueId) {
        this.androidDeviceUniqueId = androidDeviceUniqueId;
    }

    public Integer getAndroidUserUniqueId() {
        return androidUserUniqueId;
    }

    public void setAndroidUserUniqueId(Integer androidUserUniqueId) {
        this.androidUserUniqueId = androidUserUniqueId;
    }

    public String getAndroidDeviceInfo() {
        return androidDeviceInfo;
    }

    public void setAndroidDeviceInfo(String androidDeviceInfo) {
        this.androidDeviceInfo = androidDeviceInfo;
    }

    public int getAndroidOSVer() {
        return androidOSVer;
    }

    public void setAndroidOSVer(int androidOSVer) {
        this.androidOSVer = androidOSVer;
    }

    public void setLibVersion(int libVersion) {
        this.libVersion = libVersion;
    }

    public int getLibVersion() {
        return libVersion;
    }

    public int initLibVersion()
    {
        try {
            return context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
                    .metaData.getInt("iu_libversion");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String initAndroidDevicerUniqueId() {
        List<Uuid> uuids = Uuid.find(Uuid.class, "key=?", "uuid");
        if (uuids != null && uuids.size() > 0) {
            return uuids.get(0).getValue();
        } else {
            String uuid = UUID.randomUUID().toString();
            Uuid u = new Uuid();
            u.setKey("uuid");
            u.setValue(uuid);
            u.save();
            return uuid;
        }
    }

    public int initAndroidOSVer() {
        return Build.VERSION.SDK_INT;
    }


    public RequestParams getParams() {
        RequestParams params = new RequestParams();
        params.put("vender_id", vendorId);
        params.put("device_id", deviceId);
        params.put("serial", serial);
        params.put("android_device_unique_id", androidDeviceUniqueId);
        params.put("android_user_unique_id", androidUserUniqueId);
        params.put("android_device_info", androidDeviceInfo);
        params.put("android_os_ver", androidOSVer);
        params.put("lib_version", libVersion);
        params.put("device_info", deviceInfo);

        return params;
    }

}
