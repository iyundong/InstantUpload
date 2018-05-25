package cn.rainx.demo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpDeviceInfo;
import android.mtp.MtpObjectInfo;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.rainx.exif.ExifUtils;
public class ControllerActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    // log 显示区域
    private EditText etLogPanel;

    // 是否连接
    boolean isOpenConnected = false;

    // USB 设备句柄
    UsbDevice usbDevice;

    // 对象名称
    EditText etPtpObjectName;
    EditText etPtpObjectInfoName;

    CancellationSignal signal;

    // 接口
    protected UsbInterface 				intf;

    protected UsbDeviceConnection mConnection = null;

    private MtpClient mClient;
    private MtpDevice mDevice;
    /**
     * usb插拔接收器
     */
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                log("usb设备已接入");
                isOpenConnected = false;
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                log("usb设备已拔出");
                isOpenConnected = false;
                detachDevice();
            }
        }
    };


    // 处理权限
    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //user choose YES for your previously popup window asking for grant perssion for this usb device
                        if(null != usbDevice){
                            performConnect(usbDevice);
                        }
                    }
                    else {
                        //user choose NO for your previously popup window asking for grant perssion for this usb device
                        log("Permission denied for device" + usbDevice);
                    }
                }
            }
        }
    };


    /**
     * 注册usb设备插拔广播接收器
     */
    void registerUsbDeviceReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }


    final private static String TAG = "ControllerActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        setTitle("PTP协议测试");

        // 注册按钮点击事件
        ((Button) findViewById(R.id.allUsbDevices)).setOnClickListener(this);
        ((Button) findViewById(R.id.connectToDevices)).setOnClickListener(this);
        ((Button) findViewById(R.id.getDevicePTPInfo)).setOnClickListener(this);
        ((Button) findViewById(R.id.getAllObjects)).setOnClickListener(this);
        ((Button) findViewById(R.id.transferObject)).setOnClickListener(this);
        ((Button) findViewById(R.id.getObjectInfo)).setOnClickListener(this);
        ((Button) findViewById(R.id.updateExif)).setOnClickListener(this);

        etPtpObjectName = (EditText) findViewById(R.id.ptpObject);
        etPtpObjectInfoName = (EditText) findViewById(R.id.ptpObjectInfo);

        etLogPanel = (EditText) findViewById(R.id.logPanel);
        etLogPanel.setGravity(Gravity.BOTTOM);

        log("程序初始化完成");

        registerUsbDeviceReceiver();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        try {
            unregisterReceiver(usbPermissionReceiver);
        } catch (Exception e) {
            // 如果之前没有注册, 则会抛出异常
            e.printStackTrace();
        }
        detachDevice();
    }


    void detachDevice() {
        mClient.close();
    }


    /**
     * 注册操作usb设备需要的权限
     * @param usbDevice
     */
    void registerUsbPermission( UsbDevice usbDevice ){
        log("请求USB设备访问权限");
        UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbPermissionReceiver, filter);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, mPermissionIntent);
    }

    // 处理按钮点击
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.allUsbDevices:
                getAllUsbDevices();
                break;
            case R.id.connectToDevices:
                connectMTPDevice();
                break;
            case R.id.getDevicePTPInfo:
                //getDevicePTPInfo();
                getDevicePTPInfoVersion2();
                break;
            case R.id.getAllObjects:
                getAllObjects();
                break;
            case R.id.getObjectInfo:
                getObjectInfo();
                break;
            case R.id.transferObject:
                transferObject();
                break;
            case R.id.updateExif:
                updateExif();
                break;
        }
    }


    public void getAllUsbDevices() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        int i = 0;
        while (deviceIterator.hasNext()) {
            i++;
            UsbDevice device = deviceIterator.next();
            log("--------");
            log("设备 ： " + i);
            log("device id : " + device.getDeviceId());
            log("name : " + device.getDeviceName());
            log("class : " + device.getDeviceClass());
            log("subclass : " + device.getDeviceSubclass());
            log("vendorId : " + device.getVendorId());
            // log("version : " + device.getVersion() );
            log("serial number : " + device.getSerialNumber() );
            log("interface count : " + device.getInterfaceCount());
            log("device protocol : " + device.getDeviceProtocol());
            log("--------");

        }
    }

    public void getDevicePTPInfoVersion2() {
        if (isOpenConnected) {
            log("准备获取ptp信息v2");
            try {
                MtpDeviceInfo deviceInfo = mDevice.getDeviceInfo();
                String manufacture = deviceInfo.getManufacturer();
                String model = deviceInfo.getModel();
                String version = deviceInfo.getVersion();
                String serialNo = deviceInfo.getSerialNumber();

                log(String.format("device info: %s %s ver: %s sn: %s", manufacture, model, version, serialNo));

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log("mtp/ptp 设备未连接v2");
        }
    }

    public void getAllObjects() {
        if (isOpenConnected && mDevice != null) {
            log("准备获取objects信息");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int[] sids = mDevice.getStorageIds();
                        for (int sid : sids) {
                            log("--------");
                            log("检查storage id: " + sid);
                            log("--------");
                            int[] objectHandles = mDevice.getObjectHandles(sid, 0, 0);
                            log("获取sid (" + sid + ")中的对象句柄");
                            List<String> strHandleList = new ArrayList<String>(objectHandles.length);

                            for (int objectHandle : objectHandles) {
                                strHandleList.add(objectHandle + "");
                            }
                            log(TextUtils.join(",", strHandleList));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();


        } else {
            log("mtp/ptp 设备未连接");
        }
    }

    public void getObjectInfo() {

        try {
            if (isOpenConnected) {
                final String oh = etPtpObjectInfoName.getText().toString();
                if (oh.trim() == "") {
                    log("请输入object handle , 为数字类型");
                    return;
                }

                log("准备获取信息");

                MtpObjectInfo info = mDevice.getObjectInfo(Integer.valueOf(oh));
                if (info != null) {
                    String fileName = info.getName();
                    int height = info.getImagePixHeight();
                    int width = info.getImagePixWidth();
                    int type = info.getAssociationType();
                    String displayString;
                    if (type == MtpConstants.ASSOCIATION_TYPE_GENERIC_FOLDER) {
                        displayString = String.format("目录: %s", fileName);
                    } else {
                        displayString = String.format(Locale.US, "文件: %s, width: %d, height: %d", fileName, width, height);
                    }

                    log(displayString);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void transferObject() {
        if (isOpenConnected) {
            final String oh = etPtpObjectName.getText().toString();
            if (oh.trim() == "") {
                log("请输入object handle , 为数字类型");
                return;
            }
            final int ohandle = Integer.valueOf(oh);

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    File tmp = new File(getExternalCacheDir(), "tmp_" + oh + ".jpg");
                    String outputFilePath = tmp.getPath();
                    log("准备传输数据");
                    try {
                        boolean transfer = mDevice.importFile(ohandle, outputFilePath);

                        if (transfer) {
                            log("传输成功 : " + outputFilePath);
                        } else {
                            log("传输失败");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            })).start();
        }
    }


    // 连接到ptp/mtp设备
    void connectMTPDevice(){
        UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        Log.v("usb manager get", usbManager.toString());
        Map<String, UsbDevice> map = usbManager.getDeviceList();
        Set<String> set = map.keySet();

        if (set.size() == 0) {
            log("无法获取设备信息，请确保相机已经连接或者处于激活状态");
        }

        for (String s : set) {
            UsbDevice device = map.get(s);
            if( !usbManager.hasPermission(device) ){
                registerUsbPermission(device);
                return;
            }else {
                performConnect(device);
            }
        }
    }

    // 更新图片的exif信息
    private void updateExif() {
        log("getExternalFilesDir is " + getExternalFilesDir(null).getAbsolutePath());
        final String filePath = getExternalFilesDir(null).getAbsolutePath() + "/hello.jpg";
        // 密云	116.85	减0小时12分36秒	40.37
        boolean ret = ExifUtils.updateExifLocation(filePath, 40.37d, 116.85, new Date());
        log("update ret value is " + ret);
    }


    void performConnect(UsbDevice device) {
        if (mClient == null) {
            mClient = new MtpClient(getApplicationContext());
        }


        if (!isOpenConnected) {
            try {

                List<MtpDevice> mtpDeviceList = mClient.getDeviceList();

                for (MtpDevice currentDevice : mtpDeviceList) {
                    if (currentDevice.getDeviceName().equals(device.getDeviceName())) {
                        mDevice = currentDevice;
                    }
                }

                log("已连接到 " + mDevice.getDeviceName());

                isOpenConnected = true;

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log(e.toString());
            }
        } else {
            log("设备已经连接，无需重联");
        }
    }



    // searches for an interface on the given USB device, returns only class 6  // From androiddevelopers ADB-Test
    private UsbInterface findUsbInterface(UsbDevice device) {
        //Log.d (TAG, "findAdbInterface " + device.getDeviceName());
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            Log.d (TAG, "Interface " +i + " Class " +intf.getInterfaceClass() +" Prot " +intf.getInterfaceProtocol());
            if (intf.getInterfaceClass() == 6
                //255 && intf.getInterfaceSubclass() == 66 && intf.getInterfaceProtocol() == 1
                    ) {
                return intf;
            }
        }
        return null;
    }

    private void log(final String text) {
        ControllerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, text);
                String originalText = etLogPanel.getText().toString();
                //String newText = originalText + text + "\n";
                etLogPanel.append(text + "\n");
            }
        });
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
