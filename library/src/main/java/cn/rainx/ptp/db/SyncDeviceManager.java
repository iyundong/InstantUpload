package cn.rainx.ptp.db;

import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.rainx.ptp.detect.CameraDetector;

/**
 * !!! Not thread safe !!!
 * 理论上我们的程序应该运行在单一线程，所以为了性能考虑，没有加锁
 * Created by rainx on 2017/5/27.
 */

public class SyncDeviceManager {
    public static final String TAG = "SyncDeviceManager";

    private UsbDevice device;

    public SyncDeviceManager(UsbDevice device) {
        this.device = device;
    }


    /**
     * 更新同步设备的基本信息， 如果设备信息已经存在，则只更新时间
     *
     * @return true: if first time create
     *          null: if already exists
     *
     */
    public SyncDevice updateDeviceInfo()  {
        String uuid = getUUIDFromDevice(device);
        SyncDevice syncDevice = getSyncDevice(device);

        if (syncDevice == null) {
            syncDevice = new SyncDevice(
                    uuid,
                    device.getDeviceName(),
                    device.getProductName(),
                    device.getManufacturerName(),
                    device.getVendorId(),
                    device.getDeviceId(),
                    device.getProductId(),
                    device.getSerialNumber(),
                    "", //device.getVersion() require android version 23;
                    "", // empty set
                    false,
                    null,
                    new Date().getTime(),
                    new Date().getTime()
            );
            syncDevice.save();
            Log.d(TAG, "device recording");
        } else {
            // 设备已经记录了
            syncDevice.setUpdatedAt((new Date()).getTime());
            syncDevice.save();
            Log.d(TAG, "device already recorded");
        }
        return syncDevice;
    }

    /**
     * 在当前的手机上清除拍照设备信息
     *
     * @return 是否执行了删除操作
     */
    public boolean removeDevice() {
        SyncDevice syncDevice = getSyncDevice(device);
        if (syncDevice != null) {
            return syncDevice.delete();
        } else {
            return false;
        }
    }

    /**
     * 开始同步
     */
    public void startSync() {
        SyncDevice syncDevice = getSyncDevice(device);

        if (syncDevice.getSyncAt() == null || syncDevice.getSyncAt() == 0) {
            syncDevice.setSyncAt(new Date().getTime());
        }

        syncDevice.setUpdatedAt(new Date().getTime());
        syncDevice.setSyncing(true);
        syncDevice.save();
    }

    /**
     * 停止同步
     */
    public void stopSync() {
        SyncDevice syncDevice = getSyncDevice(device);

        syncDevice.setUpdatedAt(new Date().getTime());
        syncDevice.setSyncing(false);
        syncDevice.save();
    }


    /**
     * 更新id列表
     * @param ids
     */
    public void updateIdList(List<Integer> ids) {
        StringBuffer strIdListBuffer = new StringBuffer();
        if (ids != null) {
            int i = 1;
            for (Integer id : ids){
                strIdListBuffer.append(id);
                if (i++ != ids.size()) {
                    strIdListBuffer.append(",");
                }
            }
        }
        String strIdList = strIdListBuffer.toString();

        SyncDevice syncDevice = getSyncDevice(device);
        syncDevice.setSyncIdList(strIdList);
        syncDevice.setSyncAt(new Date().getTime());
        syncDevice.setUpdatedAt(new Date().getTime());
        syncDevice.save();
    }

    /**
     * 获取id列表
     * @return
     */
    public List<Integer> getIdList() {
        SyncDevice syncDevice = getSyncDevice(device);
        return getIdList(syncDevice);
    }

    /***
     * 根据syncDevice获取id列表
     */
    public List<Integer> getIdList(SyncDevice syncDevice) {
        String strIdList = syncDevice.getSyncIdList();
        List<Integer> idList = new ArrayList<>();
        if (strIdList == null || strIdList.equals("")) {
            return idList;
        }

        String[] strIdArr = strIdList.split(",");
        for(String id : strIdArr) {
            idList.add(Integer.valueOf(id));
        }
        return idList;
    }

    /**
     * 获取数据库中的同步设备ORM
     * @param device
     * @return
     */
    public SyncDevice getSyncDevice(UsbDevice device) {
        String uuid = getUUIDFromDevice(device);
        Log.d(TAG, "getSyncDevice by uuid:" + uuid );
        List<SyncDevice> syncDevices = SyncDevice.find(SyncDevice.class, "DEVICE_UUID=?", uuid);
        if (syncDevices!= null && syncDevices.size() > 0) {
            return syncDevices.get(0);
        } else {
            Log.d(TAG, "is null");
            return null;
        }
    }


    public UsbDevice getDevice() {
        return device;
    }

    public void setDevice(UsbDevice device) {
        this.device = device;
    }

    /**
     * 获取所有同步设备信息
     * @return
     */
    public List<SyncDevice> getAllSyncDevices() {
        return SyncDevice.listAll(SyncDevice.class);
    }

    private String getUUIDFromDevice(UsbDevice device) {
        CameraDetector cd = new CameraDetector(device);
        String uuid = cd.getDeviceUniqName();
        return uuid;
    }



}
