package cn.rainx.ptp.usbcamera.sony;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.rainx.ptp.usbcamera.BaselineInitiator;
import cn.rainx.ptp.usbcamera.Command;
import cn.rainx.ptp.usbcamera.Data;
import cn.rainx.ptp.usbcamera.DeviceInfo;
import cn.rainx.ptp.usbcamera.DevicePropDesc;
import cn.rainx.ptp.usbcamera.Event;
import cn.rainx.ptp.usbcamera.ObjectInfo;
import cn.rainx.ptp.usbcamera.PTPException;
import cn.rainx.ptp.usbcamera.Response;
import cn.rainx.ptp.usbcamera.Session;

/**
 * Created by rainx on 2017/5/20.
 */

public class SonyInitiator extends BaselineInitiator {


    protected int OBJECT_ADDED_EVENT_CODE = 0xc201;
    protected int PTP_OC_SONY_GetAllDevicePropData = 0x9209;
    protected int PTP_DPC_SONY_ObjectInMemory = 0xD215;
    protected int PTP_OC_SONY_GetDevicePropdesc = 0x9203;
    protected int PTP_OC_SONY_GetSDIOGetExtDeviceInfo = 0x9202;

    SonyExtDeviceInfo sonyExtDeviceInfo = null;

    /**
     * Constructs a class driver object, if the device supports
     * operations according to Annex D of the PTP specification.
     *
     * @param dev        the first PTP interface will be used
     * @param connection
     * @throws IllegalArgumentException if the device has no
     *                                  Digital Still Imaging Class or PTP interfaces
     */
    public SonyInitiator(UsbDevice dev, UsbDeviceConnection connection) throws PTPException {

        super();

        this.mConnection = connection;
        if (dev == null) {
            throw new PTPException ("dev = null");//IllegalArgumentException();
        }
        session = new Session();
        this.device = dev;
        intf = findUsbInterface (dev);

        if (intf == null) {
            //if (usbInterface == null) {
            throw new PTPException("No PTP interfaces associated to the device");
        }

        for (int i = 0; i < intf.getEndpointCount(); i++) {
            UsbEndpoint ep = intf.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epOut = ep;
                } else {
                    epIn = ep;
                }
            }
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT){
                epEv = ep;
            }
        }
        endpointSanityCheck();
        inMaxPS = epOut.getMaxPacketSize();
        intrMaxPS = epIn.getMaxPacketSize();

        // clear epOut any previous state
        reset();
    }


    protected int getObjectAddedEventCode() {
        return OBJECT_ADDED_EVENT_CODE;
    }

    // Sony Event Mode
    // See: https://github.com/gphoto/libgphoto2/blob/8b14ec11cadac05b93f344cccfcefe1fb82996e6/camlibs/ptp2/library.c#L3822
    // Sony 的Controll 模式拍照后，返回的id always 是 0xffffc001, 好像获取的时候，游客呢个有可能会出错
    // 所以采用下列方式
    /* Check if there are pending images for download. If yes, synthesize a FILE ADDED event */
    /*
    C_PTP (ptp_sony_getalldevicepropdesc (params)); //avoid caching
    C_PTP (ptp_generic_getdevicepropdesc (params, PTP_DPC_SONY_ObjectInMemory, &dpd));
    GP_LOG_D ("DEBUG== 0xd215 after capture = %d", dpd.CurrentValue.u16);
    */

    /***
     * Sony Event Poll for device
     */
    protected void runEventPoll_NOTUSE() throws PTPException {
        Log.v("PTP_EVENT", "开始event轮询");
        long loopTimes = 0;
        pollEventSetUp();
        byte[] buffer = new byte[intrMaxPS];
        int length;
        while (isSessionActive()) {
            loopTimes++;
            if (!autoPollEvent || mConnection == null) {
                try {
                    Thread.sleep(DEFAULT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
                continue;
            }

            if (loopTimes % 100 == 0) {
                try {
                    Thread.sleep(DEFAULT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }

            getAllDevicePropDesc();
            List<DevicePropDesc> props = getAllDevicePropDesc();
            if (props == null) {
                return;
            }
            for (DevicePropDesc prop : props) {
                // 	{PTP_DPC_SONY_ObjectInMemory, N_("Objects in memory")},	/* 0xD215 */
                // Log.d(TAG, prop.toString());
                if (prop.getPropertyCode() == PTP_DPC_SONY_ObjectInMemory) {
                    if ((Integer) prop.getValue() > 0x8000 ) {
                        Log.d (TAG, "SONY ObjectInMemory count change seen, retrieving file");
                        // 对于索尼的相机，必须先执行getObjectInfo指令，无论是否需要读取文件信息
                        ObjectInfo info = getObjectInfo(0xffffc001);
                        processFileAddEvent(0xffffc001 , info);
                    } else {
                        Log.d(TAG, "current prop.value of PTP_DPC_SONY_ObjectInMemory is " + Integer.toHexString((Integer) prop.getValue()));
                    }
                }
            }
        }

        Log.v("PTP_EVENT", "结束轮询");
    }

    protected Object waitVendorSpecifiedFileReadySignal() {
        long start = System.currentTimeMillis();
        // 5 秒的超时时间
        while (System.currentTimeMillis() - start < 15000) {
            // to avoid cache
            getAllDevicePropDesc();
            List<DevicePropDesc> props = getAllDevicePropDesc();
            if (props == null) {
                return null;
            }
            for (DevicePropDesc prop : props) {
                // 	{PTP_DPC_SONY_ObjectInMemory, N_("Objects in memory")},	/* 0xD215 */
                // Log.d(TAG, prop.toString());
                if (prop.getPropertyCode() == PTP_DPC_SONY_ObjectInMemory) {
                    if ((Integer) prop.getValue() > 0x8000 ) {
                        try {
                            ObjectInfo info = getObjectInfo(0xffffc001);
                            return info;
                        } catch (PTPException e) {
                            e.printStackTrace();
                        }
                        Log.d (TAG, "SONY ObjectInMemory count change seen, retrieving file");
                    } else {
                        Log.d(TAG, "current PTP_DPC_SONY_ObjectInMemory is " + Integer.toHexString((Integer) prop.getValue()) );
                    }
                }
            }
        }
        Log.d(TAG, "Sony waitVendorSpecifiedFileReadySignal timeout!" );
        return null;
    }

    protected void waitVendorSpecifiedFileReadySignal1() {
        try {
            Thread.sleep(2000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }




    // 获取所有的属性列表
    public List<DevicePropDesc> getAllDevicePropDesc() {
        Response response;
        Data data = new Data(this);

        /*
         Key: PropCode,
         Value: PropValue
         */
        List<DevicePropDesc> props = new ArrayList<>();

        synchronized (session) {
            try {
                response = transact0(PTP_OC_SONY_GetAllDevicePropData, data);
                if (data == null) {
                    Log.d(TAG, "data is null");
                    return null;
                }
                if (data.getLength() < 8) {
                    Log.d(TAG, "data length is short than 8");
                    return null;
                }

                Log.d(TAG, "PTP_OC_SONY_GetAllDevicePropData recv data is : " + byteArrayToHex(data.data));

                data.offset = 12 + 8;
                while (data.getLength() - data.offset>0) {
                    SonyDevicePropDesc desc = new SonyDevicePropDesc(this, data);
                    try {
                        desc.parse();

                    }catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    props.add(desc);
                }
                return props;
            } catch (PTPException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public DevicePropDesc getDevicePropDesc(int propcode) {
        Response response;
        Data data = new Data(this);
        try {
            response = transact1(PTP_OC_SONY_GetDevicePropdesc, data, propcode);
            data.toString();
            return null;
        } catch (PTPException e) {
            e.printStackTrace();
            return null;
        }
    }


    public Response setSDIOConnect(int mode) {
        Log.d(TAG, "set setSDIOConnect :" + mode);
        Response response;
        Data data = new Data(this);
        synchronized (session) {
            try {
                response = transact1(Command.SONY_SDIOCOMMAND, data, mode);
                return response;
            } catch (PTPException e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    @Override
    protected void pollEventSetUp() {
        super.pollEventSetUp();
        // get device info first
        try {
            getDeviceInfo();
        }catch (PTPException e) {
            e.printStackTrace();
        }
        setSDIOConnect(0x01);
        setSDIOConnect(0x02);
        sendSonyGetExtDeviceInfoCommand();
        setSDIOConnect(0x03);
    }

    private void sendSonyGetExtDeviceInfoCommand() {
        SonyExtDeviceInfo data = new SonyExtDeviceInfo(this);
        try {
            transact1(PTP_OC_SONY_GetSDIOGetExtDeviceInfo, data, 0xc8);
            try {
                data.parse();
                sonyExtDeviceInfo = data;
                Log.d(TAG, sonyExtDeviceInfo.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (PTPException e) {
            e.printStackTrace();
        }
    }


    // 可以被子类覆盖，进行轮询之前的准备工作
    protected void pollListSetUp() {

        super.pollListSetUp();
        // get device info first
        try {
            getDeviceInfo();
        }catch (PTPException e) {
            e.printStackTrace();
        }
        setSDIOConnect(0x01);
        setSDIOConnect(0x02);
        sendSonyGetExtDeviceInfoCommand();
    }

    protected void pollListAfterGetStorages(int ids[]) {
        Log.v(TAG, "pollListAfterGetStorages : get storages : " + Arrays.toString(ids));
        setSDIOConnect(0x03);
    }


    public void openSession() throws PTPException {
        Log.d(TAG,"claimInterface");
        mConnection.claimInterface(intf, false);
        super.openSession();
    }

    public void close() throws PTPException {
        super.close();
    }
}
