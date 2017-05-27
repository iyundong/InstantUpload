package cn.rainx.ptp.db;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;

/**
 *
 *
 * device_uuid string  unique
 * device_name string
 * product_name string
 * manufacturer_name string
 * vendor_id integer
 * device_id integer
 * product_id integer
 * serial_number    string
 * version   string  (version of device)
 * sync_idlist_json text
 * is_syncing boolean
 * sync_at unix_timestamp
 * updated_at unix_timestamp
 * created_at unix_timestamp
 *
 * Created by rainx on 2017/5/27.
 */

public class SyncDevice extends SugarRecord {
    @Unique
    String deviceUUID;
    String deviceName;
    String productName;
    String manufacturerName;
    Integer vendorId;
    Integer deviceId;
    Integer productId;
    String serialNumber;
    String version;
    String syncIdList;
    boolean isSyncing;
    Long syncAt;
    Long updatedAt;
    Long createdAt;

    public SyncDevice() {
        // empty constructor
    }

    public SyncDevice(String deviceUUID, String deviceName, String productName, String manufacturerName, Integer vendorId, Integer deviceId, Integer productId, String serialNumber, String version, String syncIdList, boolean isSyncing, Long syncAt, Long updatedAt, Long createdAt) {
        this.deviceUUID = deviceUUID;
        this.deviceName = deviceName;
        this.productName = productName;
        this.manufacturerName = manufacturerName;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.productId = productId;
        this.serialNumber = serialNumber;
        this.version = version;
        this.syncIdList = syncIdList;
        this.isSyncing = isSyncing;
        this.syncAt = syncAt;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public void setDeviceUUID(String deviceUUID) {
        this.deviceUUID = deviceUUID;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public Integer getVendorId() {
        return vendorId;
    }

    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSyncIdList() {
        return syncIdList;
    }

    public void setSyncIdList(String syncIdList) {
        this.syncIdList = syncIdList;
    }

    public boolean isSyncing() {
        return isSyncing;
    }

    public void setSyncing(boolean syncing) {
        isSyncing = syncing;
    }

    public Long getSyncAt() {
        return syncAt;
    }

    public void setSyncAt(Long syncAt) {
        this.syncAt = syncAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

}
