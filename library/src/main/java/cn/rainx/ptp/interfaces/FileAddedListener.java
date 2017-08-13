package cn.rainx.ptp.interfaces;

import cn.rainx.ptp.usbcamera.BaselineInitiator;
import cn.rainx.ptp.usbcamera.Container;

/**
 * Created by rainx on 2017/5/7.
 */

public interface FileAddedListener {
    /**
     * 当相机有新的文件被添加的时候触发
     * @param bi BaselineInitiator的子类
     * @param fileHandle 文件句柄
     * @param data 对应的Event（尼康）或者Response （佳能）数据
     */
    void onFileAdded(BaselineInitiator bi, int fileHandle, Object data);
}
