// Copyright 2000 by David Brownell <dbrownell@users.sourceforge.net>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed epIn the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package cn.rainx.ptp.usbcamera;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.rainx.ptp.db.SyncDevice;
import cn.rainx.ptp.db.SyncDeviceManager;
import cn.rainx.ptp.interfaces.FileAddedListener;
import cn.rainx.ptp.interfaces.FileDownloadedListener;
import cn.rainx.ptp.interfaces.FileTransferListener;
import cn.rainx.ptp.params.SyncParams;

/**
 * This initiates interactions with USB devices, supporting only
 * mandatory PTP-over-USB operations; both
 * "push" and "pull" modes are supported.  Note that there are some
 * operations that are mandatory for "push" responders and not "pull"
 * ones, and vice versa.  A subclass adds additional standardized
 * operations, which some PTP devices won't support.  All low
 * level interactions with the device are done by this class,
 * including especially error recovery.
 *
 * <p> The basic sequence of operations for any PTP or ISO 15470
 * initiator (client) is:  acquire the device; wrap it with this
 * driver class (or a subclass); issue operations;
 * close device.  PTP has the notion
 * of a (single) session with the device, and until you have an open
 * session you may only invoke {@link #getDeviceInfo} and
 * {@link #openSession} operations.  Moreover, devices may be used
 * both for reading images (as from a camera) and writing them
 * (as to a digital picture frame), depending on mode support.
 *
 * <p> Note that many of the IOExceptions thrown here are actually
 * going to be <code>usb.core.PTPException</code> values.  That may
 * help your application level recovery processing.  You should
 * assume that when any IOException is thrown, your current session
 * has been terminated.
 *
 *
 * @version $Id: BaselineInitiator.java,v 1.17 2001/05/30 19:33:43 dbrownell Exp $
 * @author David Brownell
 *
 * This class has been reworked by ste epIn order to make it compatible with
 * usbjava2. Also, this is more a derivative work than just an adaptation of the
 * original version. It has to serve the purposes of usbjava2 and cameracontrol.
 */
public class BaselineInitiator extends NameFactory implements Runnable {

    ///////////////////////////////////////////////////////////////////
    // USB Class-specific control requests; from Annex D.5.2
    private static final byte CLASS_CANCEL_REQ        = (byte) 0x64;
    private static final byte CLASS_GET_EVENT_DATA    = (byte) 0x65;
    private static final byte CLASS_DEVICE_RESET      = (byte) 0x66;
    private static final byte CLASS_GET_DEVICE_STATUS = (byte) 0x67;
    protected static final int  DEFAULT_TIMEOUT 		  = 1000; // ms

    final static boolean DEBUG = false;
    final static boolean TRACE = false;
	public static final String TAG = "BaselineInitiator";
    
    public UsbDevice device;
    protected UsbInterface intf;
    protected UsbEndpoint epIn;
    protected int                    	inMaxPS;
    protected UsbEndpoint epOut;
    protected UsbEndpoint epEv;
    protected int                       intrMaxPS;
    protected Session                	session;
    protected DeviceInfo             	info;
    public UsbDeviceConnection mConnection = null; // must be initialized first!
    	// mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

    protected int OBJECT_ADDED_EVENT_CODE = Event.ObjectAdded;

    protected List<FileAddedListener> fileAddedListenerList = new ArrayList<FileAddedListener>();
    protected List<FileDownloadedListener> fileDownloadedListenerList = new ArrayList<FileDownloadedListener>();
    protected List<FileTransferListener> fileTransferListenerList = new ArrayList<FileTransferListener>();

    /// 是否自动下载文件
    protected boolean autoDownloadFile = true;

    /// 是否自动轮询事件
    protected boolean autoPollEvent = true;

    /// 文件下载路径
    protected String fileDownloadPath;



    // running polling pollingThread
    Thread pollingThread = null;

    // 同步触发模式
    protected int syncTriggerMode = SyncParams.SYNC_TRIGGER_MODE_EVENT;
    // 同步模式
    protected int syncMode = SyncParams.SYNC_MODE_SYNC_NEW_ADDED;
    // 同步记录模式
    protected int syncRecordMode = SyncParams.SYNC_RECORD_MODE_REMEMBER;

    // 获取get object handle 时的过滤参数 0 为全部文件
    protected int getObjectHandleFilterParam = 0;

    // 运行时的线程
    protected volatile boolean pollThreadRunning = false;



    // 提供一个默认的构造函数，供子类继承时使用
    protected BaselineInitiator() { };

    /**
     * Constructs a class driver object, if the device supports
     * operations according to Annex D of the PTP specification.
     *
     * @param dev the first PTP interface will be used
     * @exception IllegalArgumentException if the device has no
     *	Digital Still Imaging Class or PTP interfaces
     */
    public BaselineInitiator(UsbDevice dev, UsbDeviceConnection connection) throws PTPException {
        if (connection == null) {
            throw new PTPException ("Connection = null");//IllegalArgumentException();
        }
    	this.mConnection = connection;
//        try {
            if (dev == null) {
                throw new PTPException ("dev = null");//IllegalArgumentException();
            }
            session = new Session();
            this.device = dev;
            intf = findUsbInterface (dev);

//            UsbInterface usbInterface = intf.getUsbInterface();

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

            //UsbDevice usbDevice = dev.getUsbDevice();
//            UsbConfigDescriptor[] descriptors = usbDevice.getConfig();
//
//            if ((descriptors == null) || (descriptors.length < 1)) {
//                throw new PTPException("UsbDevice with no descriptors!");
//            }

            // we want exclusive access to this interface.
            //TODO implement: UsbDeviceConnection.claimInterface (intf, true);
//            dev.open(
//                descriptors[0].getConfigurationValue(),
//                intf.getInterface(),
//                intf.getAlternateSetting()
//            );

            // clear epOut any previous state
            reset();
            if (getClearStatus() != Response.OK
                    && getDeviceStatus(null) != Response.OK) {
                throw new PTPException("can't init");
            }

            Log.d(TAG, "trying getDeviceInfoUncached");
            // get info to sanity check later requests
            info = getDeviceInfoUncached(); 

            // set up to use vendor extensions, if any
            if (info.vendorExtensionId != 0) {
                info.factory = updateFactory(info.vendorExtensionId);
            }
            session.setFactory((NameFactory) this);

    }

    
	// searches for an interface on the given USB device, returns only class 6  // From androiddevelopers ADB-Test
	protected UsbInterface findUsbInterface(UsbDevice device) {
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
    
    
    /**
     * @return the device
     */
    public UsbDevice getDevice() {
        return device;
    }

        /**
     * Returns the last cached copy of the device info, or returns
     * a newly cached copy.
     * @see #getDeviceInfoUncached
     */
    public DeviceInfo getDeviceInfo() throws PTPException {
        if (info == null) {
            return getDeviceInfoUncached();
        }
        return info;
    }

    /**
     * Sends a USB level CLASS_DEVICE_RESET control message.
     * All PTP-over-USB devices support this operation.
     * This is documented to clear stalls and camera-specific suspends,
     * flush buffers, and close the current session.
     *
     */
    public void reset() throws PTPException 
    {
//        try {
/*
 * JAVA: public int controlMsg(int requestType,
                      int request,
                      int value,
                      int index,
                      byte[] data,
                      int size,
                      int timeout,
                      boolean reopenOnTimeout)
               throws USBException
               
Android: UsbDeviceConnection controlTransfer (int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)                   	
 */		
    	if (mConnection == null) throw new PTPException("No Connection");
    	
    	mConnection.controlTransfer(
                (int) ( UsbConstants.USB_DIR_OUT      |
                		UsbConstants.USB_TYPE_CLASS        /* |
                        UsbConstants.RECIPIENT_INTERFACE */),
                CLASS_DEVICE_RESET,
                0,
                0,
                new byte[0],
                0,
                DEFAULT_TIMEOUT //,
                //false
            );

            session.close();
//        } catch (USBException e) {
//            throw new PTPException(
//                "Error initializing the communication with the camera (" +
//                e.getMessage()
//                + ")" , e);
//        }
    }

    /**
     * Issues an OpenSession command to the device; may be used
     * with all responders.  PTP-over-USB doesn't seem to support
     * multisession operations; you must close a session before
     * opening a new one.
     */
    public void openSession() throws PTPException {
        Command command;
        Response response;

        synchronized (session) {
            command = new Command(Command.OpenSession, session,
                    session.getNextSessionID());
            response = transactUnsync(command, null);
            switch (response.getCode()) {
                case Response.OK:
                    session.open();
                    pollingThread = new Thread(this);
                    pollingThread.start();
                    return;
                default:
                    throw new PTPException(response.toString());
            }
        }
    }

    /**
     * Issues a CloseSession command to the device; may be used
     * with all responders.
     */
    public void closeSession() throws PTPException {
        Response response;

        synchronized (session) {
            // checks for session already open
            response = transact0(Command.CloseSession, null);
            switch (response.getCode()) {
                case Response.SessionNotOpen:
                    if (DEBUG) {
                        System.err.println("close unopen session?");
                    }
                // FALLTHROUGH
                case Response.OK:
                    session.close();
                    return;
                default:
                    throw new PTPException(response.toString());
            }
        }
    }

    /**
     * Closes the session (if active) and releases the device.
     *
     * @throws PTPException
     */
    public void close() throws PTPException {
        // stop and close polling thead;
        if (pollingThread != null) {
            pollingThread.interrupt();
            pollingThread = null;
        }

        if (isSessionActive()) {
            try {
                closeSession();
            } catch (PTPException ignore) {
                //
                // Is we cannot close the session, there is nothing we can do
                //
            } catch (IllegalArgumentException ignore) {
                //
            }
        }

        try {
        	if (mConnection != null && intf != null) mConnection.releaseInterface(intf);
            if (mConnection != null) mConnection.close();
        	device = null;
            info = null;
        } catch (Exception ignore) {
            throw new PTPException("Unable to close the USB device");
        }
    }

	public Response showResponse (Response response) {
		Log.d(TAG, "  Type: " + response.getBlockTypeName(response.getBlockType()) +" (Code: " +response.getBlockType() +")\n");   // getU16 (4)
		Log.d(TAG, "  Name: " + response.getCodeName(response.getCode())+ ", code: 0x" + Integer.toHexString(response.getCode()) +"\n"); //getU16 (6)
		//			log ("  CodeString:" + response.getCodeString()+ "\n");
		Log.d(TAG, "  Length: " + response.getLength()+ " bytes\n");  //getS32 (0)
		Log.d(TAG, "  String: " + response.toString());
		return response;
	}

	public void showResponseCode (String comment, int code){
		Log.d(TAG, comment +" Response: " +Response._getResponseString (code) +",  code: 0x" + Integer.toHexString(code));
	}
    
    /**
     * @return true if the current session is active, false otherwise
     */
    public boolean isSessionActive() {
        synchronized (session) {
            return session.isActive();
        }
    }

    // ------------------------------------------------------- Protected methods

    ///////////////////////////////////////////////////////////////////
    /**
     * Performs a PTP transaction, passing zero command parameters.
     * @param code the command code
     * @param data data to be sent or received; or null
     * @return response; check for code Response.OK before using
     *	any positional response parameters
     */
    protected Response transact0(int code, Data data)
    throws PTPException {
        synchronized (session) {
            Command command = new Command(code, session);
            return transactUnsync(command, data);
        }
    }

    /**
     * Performs a PTP transaction, passing one command parameter.
     * @param code the command code
     * @param data data to be sent or received; or null
     * @param p1 the first positional parameter
     * @return response; check for code Response.OK before using
     *	any positional response parameters
     */
    protected Response transact1(int code, Data data, int p1)
    throws PTPException {
        synchronized (session) {
            Command command = new Command(code, session, p1);
            return transactUnsync(command, data);
        }
    }

    /**
     * Performs a PTP transaction, passing two command parameters.
     * @param code the command code
     * @param data data to be sent or received; or null
     * @param p1 the first positional parameter
     * @param p2 the second positional parameter
     * @return response; check for code Response.OK before using
     *	any positional response parameters
     */
    protected Response transact2(int code, Data data, int p1, int p2)
            throws PTPException {
        synchronized (session) {
            Command command = new Command(code, session, p1, p2);
            return transactUnsync(command, data);
        }
    }

    /**
     * Performs a PTP transaction, passing three command parameters.
     * @param code the command code
     * @param data data to be sent or received; or null
     * @param p1 the first positional parameter
     * @param p2 the second positional parameter
     * @param p3 the third positional parameter
     * @return response; check for code Response.OK before using
     *	any positional response parameters
     */
    protected Response transact3(int code, Data data, int p1, int p2, int p3)
            throws PTPException {
        synchronized (session) {
            Command command = new Command(code, session, p1, p2, p3);
            return transactUnsync(command, data);
        }
    }

    // --------------------------------------------------------- Private methods

        // like getDeviceStatus(),
    // but clears stalled endpoints before returning
    // (except when exceptions are thrown)
    // returns -1 if device wouldn't return OK status
    public int getClearStatus() throws PTPException {
        Buffer buf = new Buffer(null, 0);
        int retval = getDeviceStatus(buf);

        // any halted endpoints to clear?  (always both)
        if (buf.length != 4) {
            while ((buf.offset + 4) <= buf.length) {
                int ep = buf.nextS32();
                if (epIn.getAddress() == ep) {
                    if (TRACE) {
                        System.err.println("clearHalt epIn");
                    }
                    clearHalt(epIn);
                } else if (epOut.getAddress() == ep) {
                    if (TRACE) {
                        System.err.println("clearHalt epOut");
                    }
                    clearHalt(epOut);
                } else {
                    if (DEBUG || TRACE) {
                        System.err.println("?? halted EP: " + ep);
                    }
                }
            }

            // device must say it's ready
            int status = Response.Undefined;

            for (int i = 0; i < 10; i++) {
                try {
                    status = getDeviceStatus(null);
                } catch (PTPException x) {
                    if (DEBUG) {
                        x.printStackTrace();
                    }
                }
                if (status == Response.OK) {
                    break;
                }
                if (TRACE) {
                    System.err.println("sleep; status = "
                            + getResponseString(status));
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException x) {
                }
            }
            if (status != Response.OK) {
                retval = -1;
            }
        } else {
            if (TRACE) {
                System.err.println("no endpoints halted");
            }
        }
        return retval;
    }

    // returns Response.OK, Response.DeviceBusy, etc
    // per fig D.6, response may hold stalled endpoint numbers
    protected int getDeviceStatus(Buffer buf)
    throws PTPException {
//        try {
    	if (mConnection == null) throw new PTPException("No Connection");
    	
            byte[] data = new byte[33];
            
        	mConnection.controlTransfer(
//            device.controlMsg(
                (int) (UsbConstants.USB_DIR_IN        |
                		UsbConstants.USB_TYPE_CLASS    /*     |
                		UsbConstants.RECIPIENT_INTERFACE*/),
                CLASS_GET_DEVICE_STATUS,
                0,
                0,
                data,
                data.length, // force short reads
                DEFAULT_TIMEOUT //,
                //false
            );

            if (buf == null) {
                buf = new Buffer(data);
            } else {
                buf.data = data;
            }
            buf.offset = 4;
            buf.length = buf.getU16(0);
            if (buf.length != buf.data.length) {
                //throw new PTPException("DeviceStatus error, Buffer length wrong!");
            }

            return buf.getU16(2);
//        }  catch (USBException e) {
//            throw new PTPException(
//                "Error initializing the communication with the camera (" +
//                e.getMessage()
//                + ")" , e);
//        }
    }

    // add event listener
    // rm event listener
    ///////////////////////////////////////////////////////////////////
    // mandatory for all responders
    /**
     * Issues a GetDeviceInfo command to the device; may be used
     * with all responders.  This is the only generic PTP command
     * that may be issued both inside or outside of a session.
     */
    protected DeviceInfo getDeviceInfoUncached()
    throws PTPException {
        DeviceInfo data = new DeviceInfo(this);
        Response response;

        synchronized (session) {
//            Log.d(TAG, "getDeviceInfoUncached, sessionID " +session.getSessionId());
            Command command;
            command = new Command(Command.GetDeviceInfo, session);
//            Log.d(TAG, "Command: " +(command.getCode()) +" session: " + session.getSessionId());
            response = transactUnsync(command, data);
//            Log.d(TAG, "getDeviceInfoUncached finished, " +Response._getResponseString(response.getCode()) +" responsecode: " +response.getCode()); 
            
        }

        switch (response.getCode()) {
            case Response.OK:
                info = data;
                return data;
            default:
                throw new PTPException(response.toString());
        }
    }

    ///////////////////////////////////////////////////////////////////
    // INVARIANTS:
    // - caller is synchronized on session
    // - on return, device is always epIn idle/"command ready" state
    // - on return, session was only closed by CloseSession
    // - on PTPException, device (and session!) has been reset
    public Response transactUnsync(Command command, Data data)
    throws PTPException {
        if (!"command".equals(command.getBlockTypeName(command.getBlockType()))) {
            throw new IllegalArgumentException(command.toString());
        }
       // Log.d(TAG, command.toString() + "   Data: " +data.toString());

        // sanity checking
        int opcode = command.getCode();

        if (session.isActive()) {
            if (Command.OpenSession == opcode) {
                throw new IllegalStateException("session already open");
            }
        } else {
            if (Command.GetDeviceInfo != opcode
                    && Command.OpenSession != opcode) {
                throw new IllegalStateException("no session");
            }
        }

        // this would be UnsupportedOperationException ...
        // except that it's not available on jdk 1.1
        if (info != null && !info.supportsOperation(opcode)) {
            throw new UnsupportedOperationException(command.getCodeName(opcode));
        }

        // ok, then we'll really talk to the device
        Response response;
        boolean abort = true;

//        try {
//            OutputStream stream = device.getOutputStream(epOut);

            // issue command
            // rejected commands will stall both EPs
            if (TRACE) {
                System.err.println(command.toString());
            }
            int lenC = mConnection.bulkTransfer(epOut, command.data , command.length , DEFAULT_TIMEOUT);
			Log.d(TAG, "Command " +  command._getOpcodeString(command.getCode()) + " bytes sent " +lenC);

            // may need to terminate request with zero length packet
            if ((command.length % epOut.getMaxPacketSize()) == 0) {
				lenC = mConnection.bulkTransfer(epOut, command.data, 0, DEFAULT_TIMEOUT);
//				Log.d(TAG, "0 sent bytes:" +lenC);
                //stream.write(command.data, 0, 0);
            }

            // data exchanged?
            // errors or cancel (another pollingThread) will stall both EPs
            if (data != null) {

                // write data?
                if (!data.isIn()) {
//                	Log.d(TAG, "Start Write Data");

                    data.offset = 0;
                    data.putHeader(data.getLength(), 2 /*Data*/, opcode,
                            command.getXID());

                    if (TRACE) {
                        System.err.println(data.toString());
                    }

                    // Special handling for the read-from-N-mbytes-file case
                    //TODO yet to be implemented
//                    if (data instanceof FileSendData) {
//                        FileSendData fd = (FileSendData) data;
//                        int len = fd.data.length - fd.offset;
//                        int temp;
//
//                        // fill up the rest of the first buffer
//                        len = fd.read(fd.data, fd.offset, len);
//                        if (len < 0) {
//                            throw new PTPException("eh? " + len);
//                        }
//                        len += fd.offset;
//
//                        for (;;) {
//                            // write data or terminating packet
//                        	mConnection.bulkTransfer(epOut, data , command.length , DEFAULT_TIMEOUT);
//                            //stream.write(fd.data, 0, len);
//                            if (len != fd.data.length) {
//                                break;
//                            }
//
//                            len = fd.read(fd.data, 0, fd.data.length);
//                            if (len < 0) {
//                                throw new PTPException("short: " + len);
//                            }
//                        }
//
//                    } else {
                        // write data and maybe terminating packet
                    byte[] bytes = data.getData();//new byte [data.length];
//    				Log.d(TAG, "send Data");
                    int len = mConnection.bulkTransfer(epOut, bytes , bytes.length, DEFAULT_TIMEOUT);
//    				Log.d(TAG, "bytes sent " +len);
                    if (len < 0) {
                    	throw new PTPException("short: " + len);
                    }

                        //stream.write(data.data, 0, data.length);
                        if ((data.length % epOut.getMaxPacketSize()) == 0) {
//                        	Log.d(TAG, "send 0 Data");
                        	mConnection.bulkTransfer(epOut, bytes , 0, DEFAULT_TIMEOUT);
                            //stream.write(data.data, 0, 0);
//                        }
                    }

                    // read data?
                } else {
// Log.d(TAG, "Start Read Data");
					byte readBuffer[] = new byte[inMaxPS];
					int readLen = 0;
					readLen = mConnection.bulkTransfer(epIn, readBuffer, inMaxPS,
							DEFAULT_TIMEOUT);
                    if (readLen == 0) {
                        // rainx note: 有的时候，端点会返回空包，这个时候需要再次发送请求
                        Log.d(TAG, "rainx note: 有的时候，端点会返回空包，这个时候需要再次发送请求 ");
                        readLen = mConnection.bulkTransfer(epIn, readBuffer, inMaxPS,
                                DEFAULT_TIMEOUT);
                    }
					data.data = readBuffer;
					data.length = readLen;
					if (!"data".equals(data.getBlockTypeName(data.getBlockType()))
							|| data.getCode() != command.getCode()
							|| data.getXID() != command.getXID()) {
                        if (data.getLength() == 0) {
                            readLen = mConnection.bulkTransfer(epIn, readBuffer, inMaxPS,
                                    DEFAULT_TIMEOUT);
                            data.data = readBuffer;
                            data.length = readLen;

                            Log.d(TAG, "read a unkonwn pack , read again:" + byteArrayToHex(data.data));
                        }
						throw new PTPException("protocol err 1, " + data +
                            "\n data:" + byteArrayToHex(data.data));
					}
					
					int totalLen = data.getLength();
					if (totalLen > readLen) {
						ByteArrayOutputStream dataStream = new ByteArrayOutputStream(
								totalLen);
					
						dataStream.write(readBuffer, 0, readLen);
						
						int remaining = totalLen - readLen;
						while (remaining > 0) {
							int toRead = (remaining > inMaxPS )? inMaxPS : remaining;
							readLen = mConnection.bulkTransfer(epIn, readBuffer, toRead,
									DEFAULT_TIMEOUT);
							dataStream.write(readBuffer, 0, readLen);
							remaining -= readLen;
						}
						
						data.data = dataStream.toByteArray();
						data.length = data.length;
					}
                    data.parse();
                }
            }

            // (short) read the response
            // this won't stall anything
            byte buf[] = new byte[inMaxPS];
            Log.d(TAG, "read response");
            int len = mConnection.bulkTransfer(epIn, buf ,inMaxPS , DEFAULT_TIMEOUT);//device.getInputStream(epIn).read(buf);
            Log.d(TAG, "received data bytes: " +len);
            
            // ZLP terminated previous data?
            if (len == 0) {
                len = mConnection.bulkTransfer(epIn, buf ,inMaxPS , DEFAULT_TIMEOUT);// device.getInputStream(epIn).read(buf);
//                Log.d(TAG, "received data bytes: " +len);
            }

            response = new Response(buf, len, this);
            if (TRACE) {
                System.err.println(response.toString());
            }

            abort = false;
            return response;

            //TODO implement stall detection
//       } catch (USBException e) {
//            if (DEBUG) {
//                e.printStackTrace();
//            }
//
//            // PTP devices will stall bulk EPs on error ... recover.
//            if (e.isStalled()) {
//                int status = -1;
//
//                try {
//                    // NOTE:  this is the request's response code!  It can't
//                    // be gotten otherwise; despite current specs, this is a
//                    // "control-and-bulk" protocol, NOT "bulk-only"; or more
//                    // structurally, the protocol handles certain operations
//                    // concurrently.
//                    status = getClearStatus();
//                } catch (PTPException x) {
//                    if (DEBUG) {
//                        x.printStackTrace();
//                    }
//                }
//
//                // something's very broken
//                if (status == Response.OK || status == -1) {
//                    throw new PTPException(e.getMessage(), e);
//                }
//
//                // treat status code as the device's response
//                response = new Response(new byte[Response.HDR_LEN], this);
//                response.putHeader(Response.HDR_LEN, 3 /*response*/,
//                        status, command.getXID());
//                if (TRACE) {
//                    System.err.println("STALLED: " + response.toString());
//                }
//
//                abort = false;
//                return response;
//            }
//            throw new PTPException(e.getMessage(), e);
//
//        } catch (IOException e) {
//            throw new PTPException(e.getMessage(), e);
//
//        } finally {
//            if (abort) {
//                // not an error we know how to recover;
//                // bye bye session!
//                reset();
//            }
//        }
    }

    protected void endpointSanityCheck() throws PTPException {
        if (epIn == null) {
            throw new PTPException("No input end-point found!");
        }

        if (epOut == null) {
            throw new PTPException("No output end-point found!");
        }

        if (epEv == null) {
            throw new PTPException("No input interrupt end-point found!");
        }
        if (DEBUG){
    		Log.d(TAG, "Get: "+device.getInterfaceCount()+" Other: "+device.getDeviceName());
    		Log.d(TAG, "\nClass: "+intf.getInterfaceClass()+","+intf.getInterfaceSubclass()+","+intf.getInterfaceProtocol()
    			      + "\nIendpoints: "+epIn.getMaxPacketSize()+ " Type "+ epIn.getType() + " Dir "+epIn.getDirection()); //512 2 USB_ENDPOINT_XFER_BULK USB_DIR_IN 
    		Log.d(TAG, "\nOendpoints: "+epOut.getMaxPacketSize()+ " Type "+ epOut.getType() + " Dir "+epOut.getDirection()); //512 2 USB_ENDPOINT_XFER_BULK USB_DIR_OUT
    		Log.d(TAG, "\nEendpoints: "+epEv.getMaxPacketSize()+ " Type "+ epEv.getType() + " Dir "+epEv.getDirection()); //8,3 USB_ENDPOINT_XFER_INT USB_DIR_IN
        	
        }
    }

    private void clearHalt(UsbEndpoint e) {
        //
        // TODO: implement clearHalt of an endpoint
        //
    }
    //Ash code
    
	public void writeExtraData(Command command, Data data, int timeout)
	{
		int lenC = mConnection.bulkTransfer(epOut, command.data , command.length , timeout);
		
        if ((command.length % epOut.getMaxPacketSize()) == 0) {
			lenC = mConnection.bulkTransfer(epOut, command.data, 0, timeout);
        }
		////////////////////////////////////	
		int opcode = command.getCode();
		data.offset = 0;
        data.putHeader(data.getLength(), 2 , opcode, command.getXID());
        byte[] bytes = data.getData();
        

        mConnection.bulkTransfer(epOut, data.getData() , data.length , timeout);
        
        if ((data.length % epOut.getMaxPacketSize()) == 0) {
        	mConnection.bulkTransfer(epOut, bytes , 0, timeout);

        }
	
	}
    
    
	/*************************************************************************************
	 *                                                                                   *
	 *	  Methods to be overridden in camera-specific instances of baselineInititator    *
	 *                                                                                   *
	 *************************************************************************************/

	// this is an abstract method to be declared in
	public Response initiateCapture(int storageId, int formatCode)
			throws PTPException {
		return null;
	}
	public Response startBulb () throws PTPException{
		return null;
	}

	public Response stopBulb () throws PTPException{
		return null;
	}
	public Response setShutterSpeed (int speed) throws PTPException{
		return null;
	}  

	public Response setExposure(int exposure) throws PTPException{    	
		return null;
	}

	public Response setISO(int value) throws PTPException{
		return null;
	}

	public Response setAperture(int value) throws PTPException{
		return null;
	}

	public Response setImageQuality(int value) throws PTPException{
		return null;
	}

	// Floating point adapters
	public Response setShutterSpeed (double timeSeconds) throws PTPException{
		return null;
	}

	public Response setAperture(double apertureValue) throws PTPException{
		return null;
	}

	// Sets ISO to 50, 100, 200... or nearest value
	public Response setISO(double isoValue) throws PTPException{
		return null;
	}

	public Response setExposure(double exposureValue) throws PTPException{  
		Log.d(TAG, "Not overriden!!!");
		return null;
	}

	/**  Selects image Quality from "S" to "RAW" in 4 steps
	 * 
	 */
	public Response setImageQuality (String quality) throws PTPException{
		return null;
	}

	////////////////////////Ash
	public Response setDevicePropValueEx (int x, int y) throws PTPException{
		return null;
	}
	
	public Response MoveFocus (int x) throws PTPException{
		return null;
	}
	public Response setPictureStyle (int x) throws PTPException{
		return null;
	}
	public Response setWhiteBalance (int x) throws PTPException{
		return null;
	}
	
	public Response setMetering (int x) throws PTPException{
		return null;
	}
	public Response setDriveMode (int x) throws PTPException{
		return null;
	}
	
	public DevicePropDesc getPropValue (int value) throws PTPException{
		return null;
	}
	
	public void setupLiveview () throws PTPException{
		
	}
	
	public void getLiveView(ImageView x){
		
	}
	
	
	public byte[] read(int timeout)
	{
		Log.d(TAG,"Reading data");
		byte data[] = new byte[inMaxPS];	
		//int lengthOfBytes = mConnection.bulkTransfer(epIn, data , inMaxPS , timeout);
		
		int retries=10;
		int tmp=-1;
		for(int i=0;i<retries;retries--){
			tmp= mConnection.bulkTransfer(epIn, data , inMaxPS , timeout);
			if(tmp<0)
				Log.e(TAG,"Reading failed, retry");
			else
				break;
		}
		
		
		return data;
		
	}
	public void write(byte[] data, int length, int timeout)
	{
		Log.d(TAG,"Sending command");
		mConnection.bulkTransfer(epOut, data , length , timeout);
		
	}

	/////////////////////////
	public void setFocusPos(int x, int y)
	{


	}
	
	public void setZoom(int zoomLevel)
	{


	}
	
	public void doAutoFocus()
	{


	}


    // --- following code added by rainx May 2017
    /**
     * Hack By Rainx To Support Read Event From Interrupt Endpoint
     * @param timeout
     * @return length of content
     */
    public int readInter(int timeout, byte[] data)
    {
        Log.d(TAG,"Reading interrupt data");

        int retries=10;
        int length=-1;
        for(int i=0;i<retries;retries--){
            length= mConnection.bulkTransfer(epEv, data , intrMaxPS , timeout);
            if(length<0)
                Log.e(TAG,"Reading failed, retry");
            else
                break;
        }

        return length;
    }

    /**
     * 实现一个类似Android MtpDevice API里面的imoprtFile的功能
     *
     *
     * ref https://android.googlesource.com/platform/frameworks/av/+/master/media/mtp/MtpDevice.cpp
     *
     * bool MtpDevice::readData(ReadObjectCallback callback,
     *      const uint32_t* expectedLength,
     *      uint32_t* writtenSize,
     *      void* clientData)
     *
     * Copies the data for an object to a file in external storage.
     * This call may block for an arbitrary amount of time depending on the size
     * of the data and speed of the devices.
     *
     * @param objectHandle handle of the object to read
     * @param destPath path to destination for the file transfer.
     *      This path should be in the external storage as defined by
     *      {@link android.os.Environment#getExternalStorageDirectory}
     * @return true if the file transfer succeeds
     */
    public boolean importFile(int objectHandle, String destPath)
            throws PTPException, IOException {

        File outputFile = new File(destPath);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new PTPException("can not import file since the destPath is hit FileNotFoundException");
        }

        synchronized (session) {
            long startDownloadAt = System.currentTimeMillis();
            // step 1 发送指令阶段
            Command command = new Command(Command.GetObject, session, objectHandle);
            if (!session.isActive())
                throw new IllegalStateException("no session");

            // this would be UnsupportedOperationException ...
            // except that it's not available on jdk 1.1
            if (info != null && !info.supportsOperation(Command.GetObject)) {
                throw new UnsupportedOperationException(command.getCodeName(Command.GetObject));
            }

            // ok, then we'll really talk to the device
            Response response;
            boolean abort = true;
            int lenC = mConnection.bulkTransfer(epOut, command.data , command.length , DEFAULT_TIMEOUT);

            // may need to terminate request with zero length packet
            if ((command.length % epOut.getMaxPacketSize()) == 0) {
                lenC = mConnection.bulkTransfer(epOut, command.data, 0, DEFAULT_TIMEOUT);
            }
            // step2 读取数据阶段
            byte readBuffer[] = new byte[inMaxPS];
            int readLen = 0;
            readLen = mConnection.bulkTransfer(epIn, readBuffer, inMaxPS,
                    DEFAULT_TIMEOUT);

            // 获取第一块data buffer
            Data data = new Data(this);
            data.data = readBuffer;
            data.length = readLen;

            // If object size 0 byte, the remote device may reply a response packet without sending any data
            // packets.
            if (data.getBlockType() == Container.BLOCK_TYPE_RESPONSE) {
                response = new Response(data.data, this);
                return response.getCode() == Response.OK;
            }

            if (!"data".equals(data.getBlockTypeName(data.getBlockType()))
                    || data.getCode() != command.getCode()
                    || data.getXID() != command.getXID()) {
                throw new PTPException("protocol err 1, " + data);
            }

            int fullLength = data.getLength();

            if (fullLength < Container.HDR_LEN) {
                Log.v("ptp-error", "fullLength is too short: " + fullLength);
                return false;
            }

            int length = fullLength - Container.HDR_LEN;
            int offset = 0;
            int initialDataLength = data.length - Container.HDR_LEN;

            if (initialDataLength > 0) {
                outputStream.write(data.getData(), Container.HDR_LEN, initialDataLength);
                offset += initialDataLength;
                for(FileTransferListener fileTransferListener: fileTransferListenerList) {
                    fileTransferListener.onFileTranster(BaselineInitiator.this, objectHandle, length, initialDataLength);
                }
            }

            int remaining = fullLength - readLen;
            while (remaining > 0) {
                int toRead = (remaining > inMaxPS )? inMaxPS : remaining;
                readLen = mConnection.bulkTransfer(epIn, readBuffer, toRead,
                        DEFAULT_TIMEOUT);
                if (readLen > inMaxPS) {
                    // should not be true; but it happens
                    Log.d(TAG, "readLen " + readLen + " is bigger than inMaxPS:" + inMaxPS);
                    readLen = inMaxPS;
                }
                outputStream.write(readBuffer, 0, readLen);
                remaining -= readLen;

                for(FileTransferListener fileTransferListener: fileTransferListenerList) {
                    fileTransferListener.onFileTranster(this, objectHandle, length, length-remaining);
                }
            }
            outputStream.close();
            // step3 接收response阶段
            response = readResponse();
            if (response != null && response.getCode() == Response.OK) {
                long downloadDuring = System.currentTimeMillis() - startDownloadAt;
                for(FileDownloadedListener fileDownloadedListener: fileDownloadedListenerList) {
                    fileDownloadedListener.onFileDownloaded(this, objectHandle, outputFile, downloadDuring);
                }
                return true;
            }
        }
        return false;
    }

    public Response readResponse() {
        Response response;
        byte buf[] = new byte[inMaxPS];
//            Log.d(TAG, "read response");
        int len = mConnection.bulkTransfer(epIn, buf ,inMaxPS , DEFAULT_TIMEOUT);//device.getInputStream(epIn).read(buf);
//            Log.d(TAG, "received data bytes: " +len);

        // ZLP terminated previous data?
        if (len == 0) {
            len = mConnection.bulkTransfer(epIn, buf ,inMaxPS , DEFAULT_TIMEOUT);// device.getInputStream(epIn).read(buf);
//                Log.d(TAG, "received data bytes: " +len);
        }

        response = new Response(buf, len, this);
        if (TRACE) {
            System.err.println(response.toString());
        }

        return response;
    }



    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public void resetListeners() {
        resetFileAddedlistener();
        resetFileDownloadedListener();
        resetFileTransferListener();
    }

    public void resetFileAddedlistener() {
        fileAddedListenerList.clear();
    }

    public void resetFileDownloadedListener() {
        fileDownloadedListenerList.clear();
    }

    public void resetFileTransferListener() {
        fileTransferListenerList.clear();
    }

    public void setFileAddedListener(FileAddedListener l) {
        if (!fileAddedListenerList.contains(l)) {
            fileAddedListenerList.add(l);
        }
    }

    public void setFileDownloadedListener(FileDownloadedListener l) {
        if (!fileDownloadedListenerList.contains(l)) {
            fileDownloadedListenerList.add(l);
        }
    }

    public void setFileTransferListener(FileTransferListener l) {
        if (!fileTransferListenerList.contains(l)) {
            fileTransferListenerList.add(l);
        }
    }

    public boolean isAutoDownloadFile() {
        return autoDownloadFile;
    }

    public void setAutoDownloadFile(boolean autoDownloadFile) {
        this.autoDownloadFile = autoDownloadFile;
    }

    public boolean isAutoPollEvent() {
        return autoPollEvent;
    }

    public void setAutoPollEvent(boolean autoPollEvent) {
        this.autoPollEvent = autoPollEvent;
    }

    public String getFileDownloadPath() {
        return fileDownloadPath;
    }

    public void setFileDownloadPath(String fileDownloadPath) {
        this.fileDownloadPath = fileDownloadPath;
    }

    /**
     * 获取所有存储设备id列表
     * @return 设备id数组
     */
    public int[] getStorageIds() throws PTPException {
        Response response;
        Data data = new Data(BaselineInitiator.this);

        synchronized (session) {
            response = transact0(Command.GetStorageIDs, data);
            switch (response.getCode()) {
                case Response.OK:
                    data.parse();
                    /**
                     * added by rainx, 研究了一下PTP协议里面，之类的ID应该用unsgined int ，但是java
                     * 里面int为signed ，所以如果精确起见，要么使用long类型，我实现了一个nextUS32Array
                     * 不过为了和后面的格式兼容，这里暂时还是使用 S32
                     */
                    return data.nextS32Array();
                default:
                    throw new PTPException(response.toString());
            }
        }
    }


    /**
     * 获取所有对象的句柄
     * 参考 MTPDevie
     *
     *
     * Returns the list of object handles for all objects on the given storage unit,
     * with the given format and parent.
     * Information about each object can be accessed via {@link #getObjectInfo}.
     *
     * @param storageId the storage unit to query
     * @param format the format of the object to return, or zero for all formats
     * @param objectHandle the parent object to query, -1 for the storage root,
     *     or zero for all objects
     * @return the object handles
     */

    public int[] getObjectHandles(int storageId, int format, int objectHandle) throws PTPException{
        Response response;
        Data data = new Data(BaselineInitiator.this);

        synchronized (session) {
            response = transact3(Command.GetObjectHandles, data, storageId, format, objectHandle);
            switch (response.getCode()) {
                case Response.OK:
                    data.parse();
                    /**
                     * added by rainx, 研究了一下PTP协议里面，之类的ID应该用unsgined int ，但是java
                     * 里面int为signed ，所以如果精确起见，要么使用long类型，我实现了一个nextUS32Array
                     * 不过为了和后面的格式兼容，这里暂时还是使用 S32
                     */
                    return data.nextS32Array();
                default:
                    throw new PTPException(response.toString());
            }
        }
    }


    /**
     * Retrieves the {@link ObjectInfo} for an object.
     *
     * @param objectHandle the handle of the object
     * @return the MtpObjectInfo
     */
    public ObjectInfo getObjectInfo(int objectHandle) throws PTPException{
        Response response;
        ObjectInfo data = new ObjectInfo(objectHandle, BaselineInitiator.this);

        synchronized (session) {
            response = transact1(Command.GetObjectInfo, data, objectHandle);
            switch (response.getCode()) {
                case Response.OK:
                    data.parse();
                    return data;
                default:
                    throw new PTPException(response.toString());
            }
        }
    }


    ///////////////////////////////////////////////////////////////////
    // rainx added for poll event
    // mandatory for all responders:  generating events
    /**
     * Makes the invoking Thread read and report events reported
     * by the PTP responder, until the Initiator is closed.
     */
    @Override
    public void run() {

        if (syncTriggerMode == SyncParams.SYNC_TRIGGER_MODE_EVENT) {
            runEventPoll();
        } else if (syncTriggerMode == SyncParams.SYNC_TRIGGER_MODE_POLL_LIST){
            try {
                runPollListPoll();
            } catch (PTPException e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * Common Event Poll for device
     */
    protected void runEventPoll() {
        Log.v("PTP_EVENT", "开始event轮询");
        long loopTimes = 0;
        pollEventSetUp();
        if (epEv != null) {
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

                length = readInter(DEFAULT_TIMEOUT, buffer);
                // Log.v("PTP_EVENT", byteArrayToHex(buffer));

                if (length > 0) {
                    Event event = new Event(buffer, null);
                    Log.v("PTP_EVENT","event is : " + event.toString());

                    if (event.getCode() == getObjectAddedEventCode()) {
                        int fileHandle = event.getParam1();
                        waitVendorSpecifiedFileReadySignal();
                        processFileAddEvent(fileHandle, event);
                    }
                }
            }
        }
        Log.v("PTP_EVENT", "结束轮询");
    }

    protected void waitVendorSpecifiedFileReadySignal() {

    }

    protected int getObjectAddedEventCode() {
        return OBJECT_ADDED_EVENT_CODE;
    }

    protected void runPollListPoll() throws PTPException {
        pollThreadRunning = true;
        final String PTP_POLL_LIST = "PTP_POLL_LIST";
        Log.v(PTP_POLL_LIST, "开始event轮询");
        long loopTimes = 0;
        SyncDeviceManager syncDeviceManager;

        // 调用相机的前置准备工作指令
        pollListSetUp();
        // 获取一次现有文件id列表
        List<Integer> oldObjectHandles;

        int[]  sids; // 存储设备id列表
        sids = getStorageIds();
        pollListAfterGetStorages(sids);
        if (syncRecordMode == SyncParams.SYNC_RECORD_MODE_REMEMBER) {
            syncDeviceManager = new SyncDeviceManager(device);
            SyncDevice syncDevice = syncDeviceManager.updateDeviceInfo();
            // 之前记录过
            if (syncDevice.getSyncAt() != null) {
                oldObjectHandles = syncDeviceManager.getIdList();
            } else {
                if (syncMode == SyncParams.SYNC_MODE_SYNC_ALL) {
                    oldObjectHandles = new ArrayList<>();
                } else {
                    oldObjectHandles = getObjectHandlesByStorageIds(sids);
                }
                if (oldObjectHandles != null) {
                    syncDeviceManager.updateIdList(oldObjectHandles);
                } else {
                    Log.d(TAG, "init oldObjectHandles is null");
                }
            }
        } else if (syncRecordMode == SyncParams.SYNC_RECORD_MODE_FORGET) {
            if (syncMode == SyncParams.SYNC_MODE_SYNC_ALL) {
                oldObjectHandles = new ArrayList<>();
            } else {
                oldObjectHandles = getObjectHandlesByStorageIds(sids);
            }
        } else {
            //oops should not be here
            oldObjectHandles = new ArrayList<>();
        }


        Log.v(PTP_POLL_LIST, "初始objectHandle列表: " + oldObjectHandles.toString());
        while(pollThreadRunning) {
            if (!isSessionActive() || !autoPollEvent || mConnection == null) {
                try {
                    Thread.sleep(DEFAULT_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
                continue;
            } else {
                // common timeout
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }

                List<Integer> newObjectHandles = getObjectHandlesByStorageIds(sids);
                List<Integer> newAdded = getAllNewAddedObjectHandles(oldObjectHandles, newObjectHandles);
                Log.v(PTP_POLL_LIST, "New Added objectHandle : " + newAdded.toString());
                List<Integer> newAddedDownloaded = new ArrayList<Integer>();
                if (newAdded.size() > 0) {
                    boolean downloadInterrupted = false;
                    for (int h : newAdded) {
                        if (processFileAddEvent(h, null)) {
                            // 如果文件下载成功，则记录
                            newAddedDownloaded.add(h);
                        } else {
                            // 如果下载失败，退出循环
                            downloadInterrupted = true;
                            break;
                        }
                    }

                    // 更新oldObjectHandle ,到最新的版本
                    if (!downloadInterrupted) {
                        oldObjectHandles = new ArrayList<>(newObjectHandles);
                    } else {
                        // 如果下载终端，则只添加成功下载的handle id
                        oldObjectHandles.addAll(newAddedDownloaded);
                    }

                    if (syncRecordMode == SyncParams.SYNC_RECORD_MODE_REMEMBER) {
                        syncDeviceManager = new SyncDeviceManager(device);
                        syncDeviceManager.updateIdList(oldObjectHandles);
                    }
                }

            }

        }

        // 调用相机的清理工作指令
        pollListTearDown();
        Log.v(PTP_POLL_LIST, "结束轮询");
    }


    private List<Integer> getAllNewAddedObjectHandles(List<Integer> oldHandles, List<Integer> newHandles) {
        List<Integer> newAdded = new ArrayList<>();
        for (Integer newHandle: newHandles) {
            if (!oldHandles.contains(newHandle)) {
                newAdded.add(newHandle);
            }
        }
        return newAdded;
    }

    private List<Integer> getObjectHandlesByStorageIds(int[] sids) throws PTPException {
        List<Integer> objectHandles;
        objectHandles = new ArrayList<Integer>();
        for(int sid : sids) {
            int[] oneStorageObjectHandles = getObjectHandles(sid, getObjectHandleFilterParam, 0);
            for (int h : oneStorageObjectHandles) {
                objectHandles.add(h);
            }
        }
        return objectHandles;
    }

    // 可以被子类覆盖，进行轮询之前的准备工作
    protected void pollListSetUp() {


    }

    // 可以被子类覆盖，进行轮询之后的清理工作
    protected void pollListTearDown() {

    }

    protected void pollListAfterGetStorages(int ids[]) {

    }

    // poll event setup mode
    protected void pollEventSetUp() {

    }

    protected boolean processFileAddEvent(int fileHandle, Object event) {
        Log.v(TAG, "start processFileAddEvent : handle -> " + fileHandle);
        for(FileAddedListener fileAddedListener: fileAddedListenerList) {
            fileAddedListener.onFileAdded(BaselineInitiator.this, fileHandle, event);
        }
        if (autoDownloadFile && fileDownloadPath != null) {
            try {
                File outputFile = new File(new File(fileDownloadPath), "tmp_" + fileHandle + ".jpg");
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                String outputFilePath = outputFile.getPath();
                importFile(fileHandle, outputFilePath);
                return true;
            } catch (PTPException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }


    public int getSyncTriggerMode() {
        return syncTriggerMode;
    }

    public void setSyncTriggerMode(int syncTriggerMode) {
        this.syncTriggerMode = syncTriggerMode;
    }

    public int getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(int syncMode) {
        this.syncMode = syncMode;
    }

    public int getSyncRecordMode() {
        return syncRecordMode;
    }

    public void setSyncRecordMode(int syncRecordMode) {
        this.syncRecordMode = syncRecordMode;
    }

    public int getGetObjectHandleFilterParam() {
        return getObjectHandleFilterParam;
    }

    public void setGetObjectHandleFilterParam(int getObjectHandleFilterParam) {
        this.getObjectHandleFilterParam = getObjectHandleFilterParam;
    }
}
