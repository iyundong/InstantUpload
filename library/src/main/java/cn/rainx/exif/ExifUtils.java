package cn.rainx.exif;

import android.media.ExifInterface;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by rainx on 2017/7/2.
 */

public class ExifUtils {

    public static final String TAG= "ExifUtils";

    // 使用exifinterface 更新exif信息，根据安卓版本的不同，可以支持jpeg 和部分 raw 格式，如果更新失败，返回false
    /*
    GPS:

        https://stackoverflow.com/questions/5280479/how-to-save-gps-coordinates-in-exif-data-on-android

        GPSLatitude

        Indicates the latitude. The latitude is expressed as three  RATIONAL values giving the degrees, minutes, and seconds, respectively. If latitude is expressed as degrees, minutes and seconds, a typical format would be dd/1,mm/1,ss/1. When degrees and minutes are used and, for example, fractions of minutes are given up to two decimal places, the format would be dd/1,mmmm/100,0/1.

        https://docs.google.com/viewer?url=http%3A%2F%2Fwww.exif.org%2FExif2-2.PDF

    Datetime：

        Cant set Date Taken/DateTime tag using the ExifInterface in Android

        https://stackoverflow.com/questions/9004462/cant-set-date-taken-datetime-tag-using-the-exifinterface-in-android

     */

    /**
     *
     * @param filePath 图片文件路径
     * @param latitude 纬度
     * @param longitude 经度
     * @param date 更新时间
     * @return
     */
    public static boolean updateExifLocation(String filePath, double latitude, double longitude, Date date) {
        ExifInterface exif = null;
        try {
            // handle gps
            exif = new ExifInterface(filePath);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS.convert(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPS.latitudeRef(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPS.convert(longitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPS.longitudeRef(longitude));

            // handle datetime
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            exif.setAttribute("DateTimeOriginal", dateTimeFormat.format(date));
            exif.setAttribute("DateTimeDigitized", dateTimeFormat.format(date));
            exif.setAttribute(ExifInterface.TAG_DATETIME,dateTimeFormat.format(date));
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP,dateFormat.format(date));
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP,timeFormat.format(date));

            exif.saveAttributes();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "can handle this file format, file is : " + filePath);
            return false;
        }
    }
}
