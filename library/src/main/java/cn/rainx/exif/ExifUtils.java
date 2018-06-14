package cn.rainx.exif;

import android.location.Location;
import android.support.media.ExifInterface;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.support.media.ExifInterface.TAG_DATETIME;
import static android.support.media.ExifInterface.TAG_SUBSEC_TIME;


/**
 * Created by rainx on 2017/7/2.
 */

public class ExifUtils {

    public static final String TAG = "ExifUtils";
    private static SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);

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
     * @param filePath 文件路径
     * @param location LocationManager返回格式的GPS信息
     */
    public static boolean updateExifGPS(String filePath, Location location) {
        // handle gps
        try {
            ExifInterface exif = new ExifInterface(filePath);
            exif.setGpsInfo(location);
            exif.saveAttributes();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "can not handle this file format, file is : " + filePath);
            return false;
        }
    }

    public static String printExifSummary(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);

            return "TAG_GPS_LATITUDE: " +
                    exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) +
                    "\n" +
                    "TAG_GPS_LATITUDE_REF: " +
                    exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) +
                    "\n" +
                    "TAG_GPS_LONGITUDE: " +
                    exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) +
                    "\n" +
                    "TAG_GPS_LONGITUDE_REF: " +
                    exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) +
                    "\n" +
                    "TAG_GPS_ALTITUDE: " +
                    exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) +
                    "\n" +
                    "TAG_GPS_ALTITUDE_REF: " +
                    exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF) +
                    "\n" +
                    "TAG_DATETIME_ORIGINAL: " +
                    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) +
                    "\n" +
                    "TAG_DATETIME_DIGITIZED: " +
                    exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED) +
                    "\n" +
                    "TAG_DATETIME: " +
                    exif.getAttribute(TAG_DATETIME) +
                    "\n" +
                    "TAG_GPS_DATESTAMP: " +
                    exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP) +
                    "\n" +
                    "TAG_GPS_TIMESTAMP: " +
                    exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP) +
                    "\n";
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "can not handle this file format, file is : " + filePath);
            return "";
        }
    }

    public boolean updatePhotoDateTime(String filePath, Date date) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            long sub = date.getTime() % 1000;
            exif.setAttribute(TAG_DATETIME, sFormatter.format(date));
            exif.setAttribute(TAG_SUBSEC_TIME, Long.toString(sub));
            exif.saveAttributes();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateLatlng(String filePath, double latitude, double longitude) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            exif.setLatLong(latitude, longitude);
            exif.saveAttributes();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Date getDateTime(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            String dateString = exif.getAttribute(TAG_DATETIME);
            return sFormatter.parse(dateString);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public double[] getLatLong(String filePath){
        //The first element is the latitude,
        //and the second element is the longitude
        try {
            ExifInterface exif = new ExifInterface(filePath);
            return exif.getLatLong();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
