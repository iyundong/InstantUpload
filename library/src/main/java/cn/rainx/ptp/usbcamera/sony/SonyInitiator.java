package cn.rainx.ptp.usbcamera.sony;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import java.util.Arrays;

import cn.rainx.ptp.usbcamera.BaselineInitiator;
import cn.rainx.ptp.usbcamera.Command;
import cn.rainx.ptp.usbcamera.Data;
import cn.rainx.ptp.usbcamera.DeviceInfo;
import cn.rainx.ptp.usbcamera.PTPException;
import cn.rainx.ptp.usbcamera.Response;
import cn.rainx.ptp.usbcamera.Session;

/**
 * Created by rainx on 2017/5/20.
 */

public class SonyInitiator extends BaselineInitiator {


    protected int OBJECT_ADDED_EVENT_CODE = 0xc201;
    protected int PTP_OC_SONY_GetAllDevicePropData = 0x9209;

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

    protected DeviceInfo getDeviceInfoUncached() throws PTPException {
        setSDIOConnect(0x01);
        setSDIOConnect(0x02);
        return super.getDeviceInfoUncached();
    }


    // 可以被子类覆盖，进行轮询之前的准备工作
    protected void pollListSetUp() {

        super.pollListSetUp();
        setSDIOConnect(0x01);
        setSDIOConnect(0x02);
    }

    protected void pollListAfterGetStorages(int ids[]) {
        Log.v(TAG, "pollListAfterGetStorages : get storages : " + Arrays.toString(ids));
        setSDIOConnect(0x03);
    }

    @Override
    protected void pollEventSetUp() {
        super.pollEventSetUp();
        setSDIOConnect(0x01);
        setSDIOConnect(0x02);
        setSDIOConnect(0x03);
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




    // just for avoid caching , so ..
    public void getAllDevicePropDesc() {
        Response response;
        Data data = new Data(this);

        synchronized (session) {
            try {
                response = transact0(PTP_OC_SONY_GetAllDevicePropData, data);
                // 我们这里就不做解析了
            } catch (PTPException e) {
                e.printStackTrace();
            }
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

}
