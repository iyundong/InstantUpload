package cn.rainx.ptp.usbcamera.sony;

import android.util.Log;

import java.util.Arrays;

import cn.rainx.ptp.usbcamera.Data;
import cn.rainx.ptp.usbcamera.NameFactory;

/**
 * Created by rainx on 2017/6/11.
 */

public class SonyExtDeviceInfo extends Data {
    private static final String TAG = "SonyExtDeviceInfo";

    int		operationsSupported [];		// 10.2
    int		eventsSupported [];		// 12.5
    int		propertiesSupported [];		// 13.3.5
    int     allSupported[];

    public SonyExtDeviceInfo(NameFactory f) {
        super (true, null, 0, f);
    }

    /** Returns true iff the device supports this operation */
    public boolean supportsOperation (int opCode)
    {
        return supports (operationsSupported, opCode);
    }

    /** Returns true iff the device supports this event */
    public boolean supportsEvent (int eventCode)
    {
        return supports (eventsSupported, eventCode);
    }

    /** Returns true iff the device supports this property */
    public boolean supportsProperty (int propCode)
    {
        return supports (propertiesSupported, propCode);
    }

    private boolean supports (int supported [], int code)
    {
        for (int i = 0; i < supported.length; i++) {
            if (code == supported [i])
                return true;
        }
        return false;
    }

    protected void parse ()
    {
        super.parse ();
        // https://github.com/gphoto/libgphoto2/blob/979e75f15b4bed396a6cac7a505c8d65b92608f2/camlibs/ptp2/ptp.c
        // ptp_sony_get_vendorpropcodes
        // skip first
        nextU16();
        allSupported = nextU16Array ();

        // 一个U16Array还没有读完的情况
        if (allSupported.length * 2 + 2 + 4 < getLength()) {
            int[] p2 = nextU16Array();
            int oldLen = allSupported.length;
            allSupported = Arrays.copyOf(allSupported, oldLen + p2.length);

            for (int i = 0; i < p2.length; i++) {
                allSupported[oldLen + i] = p2[i];
            }
        }
        // https://github.com/gphoto/libgphoto2/blob/master/camlibs/ptp2/library.c
        // search PTP_OC_SONY_GetSDIOGetExtDeviceInfo

        int opcodes = 0, propcodes = 0, events = 0, j = 0,k = 0,l = 0;

        for (int op : allSupported) {
            switch (op & 0x7000) {
                case 0x1000: opcodes++; break;
                case 0x4000: events++; break;
                case 0x5000: propcodes++; break;
                default:
                    Log.d (TAG, "ptp_sony_get_vendorpropcodes() unknown opcode " +  op);
                    break;
            }
        }

        operationsSupported = new int[opcodes];
        eventsSupported = new int[events];
        propertiesSupported = new int[propcodes];

        for (int op : allSupported) {
            switch (op & 0x7000) {
                case 0x1000:
                    operationsSupported[k++] = op;
                    break;
                case 0x4000:
                    eventsSupported[l++] = op;
                    break;
                case 0x5000:
                    propertiesSupported[j++] = op;
                    break;
                default:
                    break;
            }
        }

    }


    public String toString() {

        String result = "DeviceInfo:\n";
        // per chapter 10
        result += ("\n\nOperations Supported:");
        for (int i = 0; i < operationsSupported.length; i++) {
            result += "\n\t" +factory.getOpcodeString (operationsSupported [i]);
        }

        // per chapter 11
        result += ("\n\nEvents Supported:");
        for (int i = 0; i < eventsSupported.length; i++) {
            result += "\n\t" +factory.getEventString (eventsSupported [i]);
        }

        // per chapter 13
        result += ("\n\nDevice Properties Supported:\n");
        for (int i = 0; i < propertiesSupported.length; i++) {
            result += "\t" +factory.getPropertyName (propertiesSupported [i]);
        }
        return result;
    }
}
