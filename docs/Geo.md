# 数据采集，地理信息的同步

## 如何向文件写入地理位置信息

关于地理信息的同步，可以使用代码中的，

cn.rainx.exif 包下的`ExifUtils`类实现, 尤其对与Jpeg格式的写入，相对比较简单，如果要支持`Raw`格式的`Exif`信息的写入，可以参考：

如下实现：

- java版 libraw <https://www.libraw.org/>
- c++11版本 libopenraw <https://github.com/hfiguiere/libopenraw>

## 地理位置信息如何取值

- 地理信息，通常可以使用Android自带的LocationManager获取
- 利用百度，高德等第三方SDK获取GPS信息

建议使用第三方SDK,这样在调用的渐变性，适应性以及省电等方面都不需要自己过多考虑

- 高德地图定位api <http://lbs.amap.com/api/android-location-sdk/locationsummary>
- 百度地图定位api <http://lbsyun.baidu.com/index.php?title=android-locsdk>

## 坐标体系的转换

不同的坐标体系，在互相使用的时候，需进行转换，请注意

百度的坐标系说明 <http://lbsyun.baidu.com/index.php?title=android-locsdk/guide/coorinfo>

```
目前国内主要有以下三种坐标系：
1\. WGS84：为一种大地坐标系，也是目前广泛使用的GPS全球卫星定位系统使用的坐标系；
2\. GCJ02：表示经过国测局加密的坐标；
3\. BD09：为百度坐标系，其中bd09ll表示百度经纬度坐标，bd09mc表示百度墨卡托米制坐标；
```

高德的坐标转换

<http://lbs.amap.com/api/android-location-sdk/guide/additional-func/amap-calculate-tool>

## 何时记录

安卓客户端最好根据时间序列缓存自己的位置信息，在上传的时候，从本地缓存的位置信息里面获取位置信息，并对文件进行更新，如果在文件更新的时候，安卓设备还没有位置信息，可以暂时不进行更新，并对未更新位置信息的照片文件进行记录，等待获取位置信息之后再进行更新。

## 参考

- exif信息对照 <https://segmentfault.com/a/1190000005133854>
- 高德地图API <http://lbs.amap.com/>
- 百度地图API
