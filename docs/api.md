# API文档

## Exif 帮助

和它相关的内容在 `cn.rainx.exif` 包下。

### ExifUtils

使用exifinterface 更新exif信息，根据安卓版本的不同，可以支持jpeg 和部分 raw 格式，如果更新失败，返回false

```java
/**
 *
 * @param filePath 图片文件路径
 * @param latitude 纬度
 * @param longitude 经度
 * @param date 更新时间
 * @return
 */
public static boolean updateExifLocation(String filePath, double latitude, double longitude, Date date)
```

### GPS

一些GPS操作的辅助函数， 主要负责从将经纬度从普通的浮点数格式转化为DMS(degree minute second)格式

如`-79.948862` => `79/1,56/1,55903/1000`

```java
/**
     * convert latitude into DMS (degree minute second) format. For instance<br/>
     * -79.948862 becomes<br/>
     *  79/1,56/1,55903/1000<br/>
     * It works for latitude and longitude<br/>
     * @param latitude could be longitude.
     * @return
     */
    synchronized public static final String convert(double latitude) {
```

## 即时上传

和它相关的内容在 `cn.rainx.ptp` 包下。

改内容主要负责进行即时上传相关功能, 这里的即时上传，我们主要使用ptp协议进行。

我们会对已经接入的设备进行保存，这样，可以在手机端对已经倒入的设备进行追踪。

使用方法，可以参考 `mtpcontrollerdemo`下的`cn.rainx.demo`包的`ControllerActivity`

### AndroidManifiest.xml 配置

在`AndroidManifiest.xml`文件中，我们需要加入如下的内容

- 配置SugarApp, 由于我们使用了SugarOrm作为我们的持久化方案，所以，我们需要在我们的项目里配置`SugarApp` 作为我们的Application Context ，`或者它的子类`

```xml
<application android:allowBackup="true" android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name" android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true" android:theme="@style/AppTheme"
        android:name="com.orm.SugarApp">
```

- 配置数据库

```xml

  <meta-data android:name="DATABASE" android:value="mtp_device_sync_info.db">
    <meta-data android:name="VERSION" android:value="2">
    <meta-data android:name="QUERY_LOG" android:value="true">
    <meta-data android:name="DOMAIN_PACKAGE_NAME" android:value="cn.rainx.ptp">
  </meta-data>
  </meta-data>
  </meta-data>
  </meta-data>
```

- 在对应的Activity(后者Service)配置intent-filter，这一步不是必须的，配置之后，可以在USB设备插拔之后得到系统通知

```xml
<intent-filter>
    <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
</intent-filter>

<meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
    android:resource="@xml/device_filter" />

<intent-filter>
    <action android:name="android.hardware.usb.action.USB_DEVICE_DETACHED" />
</intent-filter>

<meta-data android:name="android.hardware.usb.action.USB_DEVICE_DETACHED"
    android:resource="@xml/device_filter" />
```

### 编码部分

- 连接到USB设备，这里使用标准的`UsbManager`来获取`UsbDevice`,

```python
// 连接到ptp/mtp设备
void connectMTPDevice(){
    UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
    Log.v("usb manager get", usbManager.toString());
    Map<String, UsbDevice> map = usbManager.getDeviceList();
    Set<String> set = map.keySet();

    if (set.size() == 0) {
        log("无法获取设备信息，请确保相机已经连接或者处于激活状态");
    }

    for (String s : set) {
        UsbDevice device = map.get(s);
        if( !usbManager.hasPermission(device) ){
            registerUsbPermission(device);
            return;
        }else {
            performConnect(device);
        }
    }
}
```

- 如果需要USB使用权限

这部分您也在AndroidManifiest文件中做预先设置，如果在代码中动态配置，则需要加上：

```java

/**
 * 注册操作usb设备需要的权限
 * @param usbDevice
 */
void registerUsbPermission( UsbDevice usbDevice ){
    log("请求USB设备访问权限");
    UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    registerReceiver(usbPermissionReceiver, filter);
    PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    usbManager.requestPermission(usbDevice, mPermissionIntent);
}

// 处理权限
private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    //user choose YES for your previously popup window asking for grant perssion for this usb device
                    if(null != usbDevice){
                        performConnect(usbDevice);
                    }
                }
                else {
                    //user choose NO for your previously popup window asking for grant perssion for this usb device
                    log("Permission denied for device" + usbDevice);
                }
            }
        }
    }
};
```

然后，我们的`performConnect`方法来处理进行USB连接的初始化，这里比较关键，系统将会初始化我们的BaseIntiator对象。

```java
void performConnect(UsbDevice device) {
    UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

    if( !isOpenConnected ){
        try {
            bi = InitiatorFactory.produceInitiator(device, usbManager);
            bi.getClearStatus(); // ?????
            bi.setSyncTriggerMode(SyncParams.SYNC_TRIGGER_MODE_POLL_LIST);
            if (bi instanceof SonyInitiator) {
                // 索尼只能支持event 模式
                bi.setSyncTriggerMode(SyncParams.SYNC_TRIGGER_MODE_EVENT);
            }
            bi.openSession();

            isOpenConnected = true;



            bi.setFileDownloadPath(getExternalCacheDir().getAbsolutePath());
            bi.setFileTransferListener(new FileTransferListener() {
                @Override
                public void onFileTranster(BaselineInitiator bi, int fileHandle, int totalByteLength, int transterByteLength) {
                    //Log.v(TAG, "[filehandle]" + fileHandle + ",totalByte:" + totalByteLength + ",transfter:" + transterByteLength);
                }
            });

            bi.setFileDownloadedListener(new FileDownloadedListener() {
                @Override
                public void onFileDownloaded(BaselineInitiator bi, int fileHandle, File localFile, long timeduring) {
                    log("file ("  + fileHandle + ") downloaded at " + localFile.getAbsolutePath() + ",time: " + timeduring + "ms");
                }
            });

        } catch (PTPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log(e.toString());
        }
    } else {
        log("设备已经连接，无需重联");
    }
}
```

其中，比较重要的是

1. 初始化`BaseIntiator`对象。

  ```java
  bi = InitiatorFactory.produceInitiator(device, usbManager);
  ```

2. `bi.setSyncTriggerMode` 设置同步触发模式，共有两种同步模式

  ```java
  SyncParams.SYNC_TRIGGER_MODE_POLL_LIST // 轮询模式
  SyncParams.SYNC_TRIGGER_MODE_EVENT  // 时间模式
  ```

3. `bi.setSyncMode` 设置同步模式，共有两种模式

  ```java
  SyncParams.SYNC_MODE_SYNC_ALL // 全部同步
  SyncParams.SYNC_MODE_SYNC_NEW_ADDED // 同步更新文件
  ```

4. `bi.setSyncRecordMode` 设置同步记录模式，共有两种记录模式

  ```java
  SyncParams.SYNC_RECORD_MODE_REMEMBER //记录同步进度
  SyncParams.SYNC_RECORD_MODE_FORGET // 不记录同步进度
  ```

5. `public void setFileDownloadPath(String fileDownloadPath)`设置文件下载路径，从相机上同步的数据文件将会下载到手机的本地指定的路径下

  ```java
  bi.setFileDownloadPath(getExternalCacheDir().getAbsolutePath());
  ```

6. `public void setFileTransferListener(FileTransferListener l)` 设置文件传输毁回调监听器，这里使用Observer模式，您可以实现一个FileTransferListener接口的实例，并实现其`onFileTranster方法`,我们可以根据此回调来处理文件传入过程中的进度监控等。其定义为，

  ```java
  /**
  *
  * @param bi
  * @param fileHandle
  * @param totalByteLength 文件总长度
  * @param transterByteLength 文件已传输长度
  */
  void onFileTranster(BaselineInitiator bi, int fileHandle, int totalByteLength, int transterByteLength);
  ```

7. `public void setFileDownloadedListener(FileDownloadedListener l)` 设置文件下载完成的监听器， 在理使用Observer模式， 您可以实现一个`FileDownloadedListener`接口的实例，并实现其`onFileDownloaded`方法，我们可以在这个回调里面处理文件从相机同步到手机之后的下一步操作，如添加注释或者上传到云存储系统等。其定义为：

  ```java
  /**
  *
  * @param bi
  * @param fileHandle 相机的file handle
  * @param localFile 下载到本地额文件
  * @param timeduring 所花的时间
  */
  void onFileDownloaded(BaselineInitiator bi, int fileHandle, File localFile, long timeduring);
  ```

8. `bi.openSession`, 这个操作非常重要，系统将开启一个PTP Session,除此之外，会开启一个线程作为即使上传同步的工作线程。

9. 在连接结束时，我们需要调用`bi.close()`关闭bi实例，已进行工作线程的终止以及内存的清理等工作。通常我们会在 USB设备detach的时候或者Activity结束的时候进行此操作

  ```java

  @Override protected void onDestroy() { super.onDestroy(); unregisterReceiver(mUsbReceiver); try {

  unregisterReceiver(usbPermissionReceiver);

  } catch (Exception e) {

  // 如果之前没有注册, 则会抛出异常
  e.printStackTrace();

  } detachDevice(); }
  ```

```java
/**- usb插拔接收器 */
BroadcastReceiver mUsbReceiver = new BroadcastReceiver() { public void onReceive(Context context, Intent intent) {

  String action = intent.getAction();
  if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) { log("usb设备已接入"); isOpenConnected = false; } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) { log("usb设备已拔出"); isOpenConnected = false; detachDevice(); }

  }
};
```

```java
void detachDevice() { if (bi != null) {

 try {
     bi.close();
 } catch (PTPException e) {
     e.printStackTrace();
 }

  } }
```

以上的步骤即可完成即使上传系统的全部配置，通常情况下，在下面介绍一下如何使用一些手工的指令做一些更细粒度的控制。

- FileAddedListener，当相机有新的文件被添加的时候触发

```java
public interface FileAddedListener {
    /**
     * 当相机有新的文件被添加的时候触发
     * @param bi BaselineInitiator的子类
     * @param fileHandle 文件句柄
     * @param data 对应的Event（尼康）或者Response （佳能）数据
     */
    void onFileAdded(BaselineInitiator bi, int fileHandle, Object data);
}
```

结合 FileAddedListener 和 FileDownloadedListener ，我们可以进行基础的文件传输速率检测工作。

- 关于相机的检测，内部使用`CameraDetector`来完成，它主要使用venderId,和deviceId来识别唯一的设备。

- 获取设备信息

  ```java
  public void getDevicePTPInfoVersion2() {
    if (isOpenConnected) {
        log("准备获取ptp信息v2");
        try {
            DeviceInfo deviceInfo = bi.getDeviceInfo();
            log("device info:" + deviceInfo.toString());
        } catch (PTPException e) {e.printStackTrace();}
    } else {
        log("mtp/ptp 设备未连接v2");
    }
  }
  ```

- 获取所有的storageId以及storage中的所有文件

  ```java
  int[] sids = bi.getStorageIds();
  for (int sid : sids) {
      log("--------");
      log("检查storage id: " + sid);
      log("--------");
      int[] objectHandles = bi.getObjectHandles(sid, 0, 0);
      log("获取sid (" + sid + ")中的对象句柄");
      List<String> strHandleList = new ArrayList<String>(objectHandles.length);

      for (int objectHandle : objectHandles) {
          strHandleList.add(objectHandle + "");
      }
      log(TextUtils.join(",", strHandleList));
  ```

- 获取对象（文件）的信息

  ```java
    public void getObjectInfo() {

    try {
        if (isOpenConnected) {
            final String oh = etPtpObjectInfoName.getText().toString();
            if (oh.trim() == "") {
                log("请输入object handle , 为数字类型");
                return;
            }

            log("准备获取信息");

            ObjectInfo info = bi.getObjectInfo(Integer.valueOf(oh));
            if (info != null) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                info.dump(ps);
                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                log(content);
            }
        }
    }catch (PTPException e) {
        e.printStackTrace();
    }
    }
  ```

- 传输单个文件

  ```python
  public void transferObject() {
     if (isOpenConnected) {
         final String oh = etPtpObjectName.getText().toString();
         if (oh.trim() == "") {
             log("请输入object handle , 为数字类型");
             return;
         }
         final int ohandle = Integer.valueOf(oh);

         (new Thread(new Runnable() {
             @Override
             public void run() {
                 File tmp = new File(getExternalCacheDir(), "tmp_" + oh + ".jpg");
                 String outputFilePath = tmp.getPath();
                 log("准备传输数据");
                 try {
                     boolean transfer = bi.importFile(ohandle, outputFilePath);

                     if (transfer) {
                         log("传输成功 : " + outputFilePath);
                     } else {
                         log("传输失败");
                     }
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
         })).start();
     }
  }
  ```

- 对BI进行进一步控制的参数配置, 下列属性在BI中都有对应的setter进行操作

```java
/// 是否自动下载文件
protected boolean autoDownloadFile = true;
// 获取get object handle 时的过滤参数 0 为全部文件
protected int getObjectHandleFilterParam = 0;
// 使用文件名或者文件object handle ID
protected int fileNameRule = SyncParams.FILE_NAME_RULE_HANDLE_ID;
/* 注意这里

使用 SyncParams.FILE_NAME_RULE_HANDLE_ID 我们将用ObjectHandle ID 作为下载文件的文件名

使用 SyncParams.FILE_NAME_RULE_OBJECT_NAME 我们会调用getObjectInfo 并使用Object的Name作为文件名，这通常就是手机SD卡中的照片文件对应的文件名。
*/

// 在open session 的时候，有的时候会出现相机设备已经open session 了，但是手机并不知道这个消息，这个时候需
// 要重新关闭之后再进行openSession操作。
protected boolean autoCloseSessionIfSessionAlreadyOpenWhenOpenSession = true;
```
