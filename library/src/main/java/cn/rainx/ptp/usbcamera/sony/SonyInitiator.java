package cn.rainx.ptp.usbcamera.sony;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

import cn.rainx.ptp.usbcamera.BaselineInitiator;
import cn.rainx.ptp.usbcamera.Command;
import cn.rainx.ptp.usbcamera.Data;
import cn.rainx.ptp.usbcamera.PTPException;
import cn.rainx.ptp.usbcamera.Response;

/**
 * Created by rainx on 2017/5/20.
 */

public class SonyInitiator extends BaselineInitiator {

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
        super(dev, connection);
    }

    public Response setSDIOConnect(int mode) {
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
