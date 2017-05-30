package cn.rainx.ptp.detect;

import android.hardware.usb.UsbDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by rainx on 2017/5/23.
 */

public class CameraDetector {

    // Select appropriate deviceInitiator, VIDs see http://www.linux-usb.org/usb.ids

    public static final int VENDOR_ID_CANON = 0x04a9;
    public static final int VENDOR_ID_NIKON = 0x04b0;
    public static final int VENDOR_ID_SONY  = 0x054c;

    public static final int VENDOR_ID_OTHER = 0xffff;

    static List<Integer> vendorIds;
    static  {
        vendorIds = Arrays.asList(
                VENDOR_ID_CANON,
                VENDOR_ID_NIKON,
                VENDOR_ID_SONY
        );
    }


    private UsbDevice device;
    public CameraDetector(UsbDevice device) {
        this.device = device;
    }


    public int getSupportedVendorId() {
        if (vendorIds.contains(device.getVendorId())) {
            return device.getVendorId();
        } else {
            return VENDOR_ID_OTHER;
        }
    }

    public String getDeviceUniqName() {
        StringBuffer sb = new StringBuffer();
        sb.append(device.getManufacturerName())
                .append("_")
                .append(device.getProductName())
                .append("_")
                .append(device.getSerialNumber());
        return sb.toString();
    }
}
