package cn.rainx.ptp.usbcamera.sony;

import cn.rainx.ptp.usbcamera.Buffer;
import cn.rainx.ptp.usbcamera.DevicePropDesc;
import cn.rainx.ptp.usbcamera.DevicePropValue;
import cn.rainx.ptp.usbcamera.NameFactory;

/**
 * Created by rainx on 2017/6/4.
 */

public class SonyDevicePropDesc extends DevicePropDesc {

    protected int unknown;


    /* Device Property pack/unpack */
    /*
        #define PTP_dpd_Sony_DevicePropertyCode	0
        #define PTP_dpd_Sony_DataType		2
        #define PTP_dpd_Sony_GetSet		4
        #define PTP_dpd_Sony_Unknown		5
        #define PTP_dpd_Sony_FactoryDefaultValue	6
    */

    protected Buffer buf;

    public SonyDevicePropDesc(NameFactory f, Buffer buf) {
        super(f);
        this.data = buf.data;
        this.offset = buf.offset;
        this.buf = buf;
    }

    public void parse ()
    {
        // per 13.3.3, tables 23, 24, 25
        propertyCode = nextU16 ();
        dataType = nextU16 ();
        writable = nextU8 () != 0;

        unknown = nextS8();

        // FIXME use factories, as vendor hooks
        factoryDefault = DevicePropValue.get (dataType, this);
        currentValue = DevicePropValue.get (dataType, this);

        formType = nextU8 ();
        switch (formType) {
            case 0:	// no more
                break;
            case 1:	// range: min, max, step
                constraints = new Range (dataType, this);
                break;
            case 2:	// enumeration: n, value1, ... valueN
                constraints = parseEnumeration ();
                break;
            default:
                System.err.println ("ILLEGAL prop desc form, " + formType);
                formType = 0;
                break;
        }

        // sync offset
        this.buf.offset = this.offset;
    }

}
