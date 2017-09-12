# 性能指标，Bug以及设备收集

## 性能指标

表名 : `iu_perf_tracker`

字段                       | 类型           | 备注
------------------------ | ------------ | ----------------------------------------
id                       | integer      | 自增
vender_id                | integer      |
device_id                | integer      |
serial                   | varchar(255) | 设备串号
android_device_unique_id | varchar(255) | 安卓设备唯一编号
android_user_unique_id   | integer      | 使用用户的唯一id编号
android_device_info      | varchar(255) | Build.BRAND + Build.DEVICE + Build.MODEL
android_os_ver           | integer      | 操作系统版本
lib_version              | integer      | 所使用的库的版本
sync_trigger_mode        | integer      | 所使用的同步触发模式
sync_mode                | integer      | 同步模式
sync_record_mode         | integer      | 同步记录模式
filesize                 | integer      | 传输文件尺寸
start_ts                 | float        | 开始时间
end_ts                   | float        | 结束时间
cost_ts                  | float        | 消耗时间
created_at               | float        | 记录创建时间

## Bug信息收集

表名 : `iu_bug_tracker`

字段                       | 类型           | 备注
------------------------ | ------------ | ----------------------------------------
vender_id                | integer      |
device_id                | integer      |
serial                   | varchar(255) | 设备串号
android_device_unique_id | varchar(255) | 安卓设备唯一编号
android_user_unique_id   | integer      | 使用用户的唯一id编号
android_device_info      | varchar(255) | Build.BRAND + Build.DEVICE + Build.MODEL
android_os_ver           | integer      | 操作系统版本
lib_version              | integer      | 所使用的库的版本
sync_trigger_mode        | integer      | 所使用的同步触发模式
sync_mode                | integer      | 同步模式
sync_record_mode         | integer      | 同步记录模式
exception                | text         | 产生的异常
traceback                | text         | 调用堆栈
fileinfo                 | text         | 如果当前正在传输文件，则记录文件的一些信息
created_at               | float        | 记录创建时间

## 设备信息收集

表名`iu_device_info_tracker`

字段                       | 类型           | 备注
------------------------ | ------------ | ----------------------------------------
id                       | integer      | 自增
vender_id                | integer      |
device_id                | integer      |
serial                   | varchar(255) | 设备串号
android_device_unique_id | varchar(255) | 安卓设备唯一编号
android_user_unique_id   | integer      | 使用用户的唯一id编号
android_device_info      | varchar(255) | Build.BRAND + Build.DEVICE + Build.MODEL
lib_version              | integer      | 所使用的库的版本
android_os_ver           | integer      | 操作系统版本

device_info | text | 设备信息 | created_at | float | 记录创建时间

## TraceBack获取

<https://stackoverflow.com/questions/1069066/get-current-stack-trace-in-java>

- Arrays.toString(Thread.currentThread().getStackTrace()); simple enough. –
- To print it nicely you can use apache StringUtils: StringUtils.join(currentThread().getStackTrace(), "\n");
- String fullStackTrace = org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e);

## Exception获取

- String errorMessage = e.getMessage();
- e.toString()

```java
Exception e = new Exception("hello");
Log.v(TAG, e.toString());
Log.v(TAG, e.getMessage());
Log.v(TAG, e.getClass().toString());
```

result

```java
09-09 18:59:22.779 10394-10409/cn.rainx.demo I/TestRunner: started: testException(cn.rainx.demo.ExampleInstrumentedTest)
09-09 18:59:22.781 10394-10409/cn.rainx.demo V/cn.rainx.demo.ExampleInstrumentedTest: java.lang.Exception: hello
09-09 18:59:22.781 10394-10409/cn.rainx.demo V/cn.rainx.demo.ExampleInstrumentedTest: hello
09-09 18:59:22.781 10394-10409/cn.rainx.demo V/cn.rainx.demo.ExampleInstrumentedTest: class java.lang.Exception
09-09 18:59:22.781 10394-10409/cn.rainx.demo I/TestRunner: finished: testException(cn.rainx.demo.ExampleInstrumentedTest)
```

## tracker 记录设备信息

每次device 信息report 应该是根据 手机uuid + 相机serial 来唯一标识确保只上传一次。
