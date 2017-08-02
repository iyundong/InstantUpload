# Instant Upload 爱云动即时上传解决方案

## 关于即时上传系统

开发缘起是爱云动公司需要开发即时上传模块的需求，作为其影响处理程序的模块之一。所谓即时上传是针对数码相机 （或有拍照功能的智能手机等设备），我们可以将其与手机（安卓系统）连接，在拍照之后即时的将照片传输到手机的功能 （当然后续有可能还会上传到云端等，whatever，我们当前只关注相机到手机的传输部分）。

我们的模块将实现如下的功能：

- 识别相机对手机的连接，拔出等操作，并作出响应
- 对相机型号进行识别
- 针对相机进行记录，保存其文件历史。
- 实现文件自动同步到手机中。
- 支持通过手机增强图片的exif信息，增加地理位置和同步时间信息等。
- 识别相机不同的sd卡，并建立记录？ ...

## 目前可以使用的同步方式

### USB mass storage device class 方式

简称后USB MSC 或者 USC, 可以称作大容量存储设备，很多相机都支持的一种文件传输方式，基于USB数据线传输，这种方式，通常手机使用OTG线与相机连接，手机作为usb host, 相机作为usb device.

简单来说，就是将相机变为类似u盘一样的存储设备和手机交互的方式，在这种方式下，（一般来说）存储设备会被独占，所以无法同时进行拍照操作，适合离线的批量导入的时候使用。

协议的具体知识参见文档：

（下面连接 **需要翻墙访问** ) <https://www.crifan.com/files/doc/docbook/usb_disk_driver/release/htmls/ch02_msc_basic.html>

or

<http://www.cnblogs.com/shangdawei/archive/2013/06/13/3133526.html>

目前新版本的安卓设备在接入OTG线的时候一般会自动识别出MSC设备并映射到文件系统的某个载入点中, 针对这个特性，程序应该可以实现批量文件的导入和管理。

### USB PTP协议方式

和上述方式类似，也是基于OTG连接的USB数据线进行传输的方式，也是我们的模块主要实现的方式，这个方式的好处是PTP协议是专门针对相机的相片传输的协议，并且在传输过程中不会锁定存储设备，也就是同时还可以进行拍照等操作。

改实现方式我们会在后面重点说明

### WIFI传输方式

很多新型相机，如Sony实现了通过WIFI和手机连接传输的方式，并提供了对应的sdk,

<https://developer.sony.com/develop/cameras/get-started/>

无线版本的远程控制接口一般基于http和json，与普通的服务器端程序接口类似。开发起来比较容易。

缺点是，在实际使用中，需要手机连接相机提供的wifi，可能会对手机的网络连接有一定影响。如果要实现同步到云端等功能时，难度会增加。

### 其它端口

#### 蓝牙

蓝牙的传输速度难以满足需求

#### hdmi

一般用来传输预览信息，一般是单向传输，无法对相机进行控制。

## 关于PTP协议

> PTP是英语"图片传输协议(picture transfer protocol)"的缩写。PTP是最早由柯达公司与微软协商制定的一种标准，符合这种标准的图像设备在接入Windows XP系统之后可以更好地被系统和应用程序所共享，尤其在网络传输方面，系统可以直接访问这些设备用于建立网络相册时图片的上传、网上聊天时图片的传送等。

PTP协议被广泛使用在相机厂商用于图片和视频的传输，但是不同的厂商根据协议的基础格式进行了不同的定制和裁剪，所以不同厂商的PTP协议并不兼容。

除了相机厂商定制的PTP协议，微软后来为了适应除了图像类文件传入之外的其它媒体文件传入，对PTP协议进行了扩充，推出了MTP协议，支持更多的文件传入，并扩充了指令集，增强了DRM版权保护等的支持，广泛应用于各类媒体产品中，如手机，MP3等设备。

PTP本身没有约定文件传输使用的物理介质和底层传输协议，目前USB, 蓝牙, TCP/IP等都可以作为底层传输介质存在。

由于速度等约束，目前使用最多的传输介质还是通过USB数据线比较多。

关于PTP最初协议的说明，可以参考下面文档

<http://www.usb.org/developers/docs/devclass_docs/usb_still_img10.zip>

关于MTP协议的定义，参考下面文档

<http://www.usb.org/developers/docs/devclass_docs/MTPv1_1.zip>

关于PTP/IP协议的实现，参考这个链接

<http://gphoto.org/doc/ptpip.php>

## 我们目前实现的同步方式

目前我们使用基于USB的PTP协议作为图片文件的传输方式，由于不同厂商对PTP协议的支持并不相同，所以我们需要对不同的厂商的PTP协议部分分别实现。 后面我们会提到实现的细节。首先我们先对USB协议和PTP的基础概念做一个介绍。

### USB的协议

USB是通用穿行总线的缩写， USB数据协议是一个不对等协议，传输的双方分别是Host(主机)和 Device或Accessory或Function(设备或者附件), 数据指令的请求总是从Host发起的，不过Device可以使用INTERRUPT传输机制来对HOST发起通知。

![http://hiphotos.baidu.com/aggio/pic/item/4742fbd6aea9a130a08bb77e.jpg?_=3591452](USB设备)

为了对USB设备不同功能的复用，USB设备将自己的功能作为一个configuration上报给HOST, 一个USB可以有多套configuration, 对应多个 Interface, 每个Interface对应多个Endpoint, 每个Endpoint对应不同的传输方向， 可以有 Bulk In , Bulk Out, Interrupt In 等（相机一般对应上面的3个端点） Endpoint一般有四种传输方式 （<http://www.beyondlogic.org/usbnutshell/usb4.shtml#Interrupt> ）

- Control Transfers
- Interrupt Transfers
- Isochronous Transfers
- Bulk Transfers

PTP一般对应Bulk In , Bulk Out, Interrupt In三个端点。 其中的In 和 Out的方向是以Host作为视角确认的方向，比如Bulk In 就是从Device拉取数据到Host的传输。

### PTP over USB

PTP/USB 是在USB协议技术上, 指令了一组指令集，负责相机和Host的交互，早期的Host的角色一般由个人电脑来承担，现在，安卓手机等设备也可以做为Host设备，当然，手机早期也可以作为Device存在的，所以可以把两台手机通过OTG数据线链接起来，这个时候一台作为Host，一台作为Device。

在PTP的定义里面，Host被定义为Initiator, Device被定义为Responder，在PTP协议里面，有下面几个比较重要的概念

Operation : Operation可以理解为一次指令的抽象，所有的Operation 都通过 Initiator 发起，会经过 Operation -> Data Phase -> Response 的一个组合完成一个事物（Transaction)， 其中 Operation 必定由 Initiator 发出到 Responder, Response是Responder -> Initiator 。 Data 的方向既可以是 Initiator -> Responder 也可以是 Responder -> Initiator。 依赖与 Operation 的类型，Host可以定义 Session ，用来为一组 Operation 定义一个统一的上下文环境，除了 Operation 之外， Device 也可以（一般通过 Interrupt Endpoint)发送 Event 给Host，用来传递事件。另外，相机可以通过特定的指令来设置，获取相机的属性（Property）

Object ： PTP协议里面，定义每一个（照片，影像）文件为一个Object, 每一个Object可以对应到一个图片，每一个Object都可以用一个32位的(无符号)整型作为句柄，可以称作 Object Handle。 在PTP协议里规定，在同一个Session里面，同一个文件的Object Handle必须相同，但是一般来说，几乎所有的实现（我目前看到的），即使在不同的Session的情况下，同一个文件的Object Handle也是相同的。

### 我们的实现

针对上面的描述和PTP常用的指令，我们实现了下面2钟主要的方式用来进行图片的同步

#### 事件模式 Event

事件模式基于PTP协议的ObjectAdded事件（通过Interrupt Endpoint)，在ObjectAdded事件中获取到Object Handle, 然后使用 GetObject 指令获取文件。

ObjectAdded 模式有如下几个问题

1. 部分相机，如Canon并不使用Interrupt Endpoint，而是使用CheckEvent指令来模拟获取ObjectAdded事件，这种方式需要利用到Bulk in 和 Bulk out端点轮询来实现，和文件传输会有冲突。
2. 还是Canon相机，在文件传输过程中，如果文件传输过程中有新的事件并且新的事件比较多，有可能在CheckEvent的时候会丢失一些事件，导致在拍照频繁的时候同步文件会有部分丢失。
3. Event模式只能处理插入USB线并开启Session之后的所有新增文件，对离线增加的文件无法同步。

#### 列表轮询模式 PollList

列表轮询模式通过轮询调用GetObjectHandles指令，获取Storage上的Object列表，检查两次获取之间Object列表的变化，识别新增的文件。并对新增的文件调用GetObject指令进行下载。这个方式的优点是相对稳定和可靠，可以识别出USB没有插入的时候的文件变化并同步。不过有如下的缺点。

1. PollList使用轮询的方式，如果设计轮询的时间过长，会影像发现新文件增加的时间。
2. PTP协议并没有说明不同Session下同一个文件的Object Handle必须相同，所以这个可能会是一个隐患(虽然暂时没有发现这个现象)。
3. 部分相机，如Sony的某些型号不支持在拍照时同时调用GetObjectHandles指令

## 我们系统中的参数定义

我们的系统里面，定义了若干参数，参数的选项定义在 cn.rainx.ptp.params.SyncParams 下， 参数通过定义BaseIntiator下的若干组getter/setter来获取/设置。

下面解释一下常用的设定

### setFileNameRule，设定文件名称的命名方式

- FILE_NAME_RULE_HANDLE_ID ： 设定为Object Handle
- FILE_NAME_RULE_OBJECT_NAME : 设定为Object的名称，就是相机SD里卡保存的文件名，依赖于多发送一个GetObjectInfo指令。

### setSyncTriggerMode, 设定同步出发模式

- SYNC_TRIGGER_MODE_EVENT ： 事件模式。
- SYNC_TRIGGER_MODE_POLL_LIST ： 设置为列表轮询模式。

### setSyncMode， 设置同步模式

- SYNC_MODE_SYNC_ALL : 同步所有内容
- SYNC_MODE_SYNC_NEW_ADDED ： 同步新增内容

### setSyncRecordMode， 设置同步记录模式

- SYNC_RECORD_MODE_REMEMBER: 记住上次的同步点，如果设置次选项，我们会在设备插入的时候，针对设备信息建立数据表中的记录，以便于下次插入数据线的时候，可以继续上次的同步点进行新的内容的同步。
- SYNC_RECORD_MODE_FORGET： 不记住上次的同步点

对于 SYNC_MODE 和 SYNC_RECORD_MODE 公有四种组合

SYNC_MODE \ SYNC_RECORD_MODE | SYNC_RECORD_MODE_REMEMBER | SYNC_RECORD_MODE_FORGET：
---------------------------- | ------------------------- | ------------------------
SYNC_MODE_SYNC_ALL           | polllist                  | polllist
SYNC_MODE_SYNC_NEW_ADDED     | polllist                  | polllist/event

其中 EVENT 模式只能支持SYNC_MODE_SYNC_NEW_ADDED + SYNC_RECORD_MODE_FORGET 的组合， POLLLIST模式支持所有四种模式

下面分别介绍一下下面几种组合的行为：

模式                                                   | 解释
---------------------------------------------------- | -----------------------------------------------------------------------------------------
SYNC_RECORD_MODE_FORGET + SYNC_MODE_SYNC_NEW_ADDED   | 每次连接只同步插入USB数据线之后的新增文件
SYNC_RECORD_MODE_FORGET + SYNC_MODE_SYNC_ALL         | 每次连接都同步所有文件
SYNC_RECORD_MODE_REMEMBER + SYNC_MODE_SYNC_NEW_ADDED | 在第一次插入相机之后开始同步文件，不同步第一次插入相机之前的文件，在拔出USB线再次连接之后，同步上次同步完成的文件之后新增的所有文件（无论图片是否是在插入USB线的时候拍摄的）
SYNC_RECORD_MODE_REMEMBER + SYNC_MODE_SYNC_ALL       | 第一次插入相机之后开始同步相机内的所有文件，在拔出USB线再次连接之后，同步上次同步完成的文件之后新增的所有文件（无论图片是否是在插入USB线的时候拍摄的）

## 如何针对不同的相机进行调试

不同的相机对指令的支持程度不一致，导致针对不同的相机，实现方式不同。目前的开源项目中， gphoto对相机设备的支持是比较全面的，它内部使用了libptp2来连接不同的设备，因为厂商的PTP协议一般是不对外开放的，我们一般可以通过查看gphoto的实现方式来理解不同相机的调用指令。

gphoto是一个命令行工具，可以在*nix类型的系统执行，并提供了一组指令来实现相机的常用功能，比如

```
gphoto2 --wait-event
```

用来监听相机的事件，如果遇到ObjectAdded事件，它会自动进行下载。

我们可以通过 `--debug`指令来查看一些详细的调试信息。 `--debug-logfile` 将它输出到日志文件中, 如：

```
gphoto2 --wait-event --debug --debug-logfile=/tmp/5d_wait-event-cap.log
```

对于gphoto2不支持的相机，如果其有官方的软件可以进行交互的话，我们可以使用一些USB嗅探工具来监听USB数据包来分析其交互。

## 未来的优化展望

1. 在程序库的组织上，目前的实现是基于之前开源的一个半成品的基础上继续开发的，有一个问题是BaseIntiator里面的代码过多，需要将部分功能抽离出来，如将 EventPoll 抽离出来。
2. 对于Canon的Event模式，之前使用的方式会发现在GetObject获取较大文件的时候，有可能会丢部分事件，可以考虑使用GetObjectPartial将一个文件的获取拆成很多小的请求，通过分配时间片的方式，将GetObjectPartial 和CheckEvent请求轮番发送，保证在下载文件的同时持续可以获取Event.
