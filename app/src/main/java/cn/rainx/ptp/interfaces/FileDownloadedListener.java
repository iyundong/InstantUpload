package cn.rainx.ptp.interfaces;

import java.io.File;

import cn.rainx.ptp.usbcamera.BaselineInitiator;

/**
 * Created by rainx on 2017/5/7.
 */

public interface FileDownloadedListener {
    /**
     *
     * @param bi
     * @param fileHandle 相机的file handle
     * @param localFile 下载到本地额文件
     */
    void onFileDownloaded(BaselineInitiator bi, int fileHandle, File localFile);
}
