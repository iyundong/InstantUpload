# API文档

## 即时上传

## Exif 帮助

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
