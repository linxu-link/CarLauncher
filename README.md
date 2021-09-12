在上一篇[Android车载应用开发与分析（1） - Android Automotive概述与编译](https://www.jianshu.com/p/bbc02e0f6575)中我们了解了如何下载以及编译面向车载IVI的Android系统，一切顺利的话，运行模拟器可以看到如下的车载android的桌面，而这就是本篇文章的重点 - CarLauncher。

![](https://upload-images.jianshu.io/upload_images/3146091-28ca2d7a795858d3?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

本篇文章以解析Android 11 源码中`CarLauncher`为主。为了便于阅读源码，现将`CarLauncher`的源码整理成可以导入Android Studio的结构，源码地址：https://github.com/linux-link/CarLauncher。由于`CarLauncher`对于源码存在依赖，该项目不能直接运行，引入jar依赖的方式也不完全正确，仅供阅读使用。

## Launcher 与 CarLauncher

> Launcher是安卓系统中的桌面启动器，安卓系统的桌面UI统称为Launcher。Launcher是安卓系统中的主要程序组件之一，安卓系统中如果没有Launcher就无法启动[安卓桌面](https://baike.baidu.com/item/安卓桌面)，Launcher出错的时候，安卓系统会出现“进程 com.android.launcher 意外停止”的提示窗口。这时需要重新启动Launcher。
> 来自《百度百科 - launcher》

`Launcher`是android系统的桌面，是用户接触到的第一个带有界面的APP。它本质上就是一个系统级APP，和普通的APP一样，它界面也是在Activity上绘制出来的。

虽然`Launcher`也是一个APP，但是它的技术难度却比一般的APP要高不少。CarLauncher作为IVI系统的桌面，往往还需要支持在桌面上动态显示如地图、音乐在内各个APP内部的信息，如下图，在桌面显示GoogleMap并与之进行简单的交互。地图开发的工作量极大，`Launcher`显然不可能引入地图的SDK在桌面上再开发一个地图应用，那么如何在不扩大工作量的前提下动态的显示地图就成了`CarLauncher`的一个技术难点。

![](https://upload-images.jianshu.io/upload_images/3146091-083334c1b7ea4c5d?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![](https://upload-images.jianshu.io/upload_images/3146091-6509a19ff2275774?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## CarLauncher 分析

本篇源码分析基于*android-11.0.0_r43，*`CarLauncher`源码位于 ***packages/apps/Car/Launche***r

### Android.dp

`CarLauncher`的android.bp相对比较简单，定义了CarLauncher的源码结构，和依赖的类库。如果你对android.bp完全不了解，可以先看一下 [Android.bp入门教程](https://www.jianshu.com/p/f23e18933122) 学习一下基础的语法，再来回过头来看`CarLauncher`的android.bp相信会容易理解很多。

```
android_app {
    name: "CarLauncher",
    srcs: ["src/**/*.java"],
    resource_dirs: ["res"],
    // 允许使用系统的hide api
    platform_apis: true,
    required: ["privapp_whitelist_com.android.car.carlauncher"],
    // 签名类型 ： platform
    certificate: "platform",
    // 设定apk安装路径为priv-app
    privileged: true,
    // 覆盖其它类型的Launcher
    overrides: [
        "Launcher2",
        "Launcher3",
        "Launcher3QuickStep",
    ],
    optimize: {
        enabled: false,
    },
    dex_preopt: {
        enabled: false,
    },
    // 引入静态库
    static_libs: [
        "androidx-constraintlayout_constraintlayout-solver",
        "androidx-constraintlayout_constraintlayout",
        "androidx.lifecycle_lifecycle-extensions",
        "car-media-common",
        "car-ui-lib",
    ],
    libs: ["android.car"],
    product_variables: {
        pdk: {
            enabled: false,
        },
    },
}

```

上述Android.bp中我们需要注意一个属性`overrides`，它表示覆盖的意思。在系统编译时`Launcher2`、`Launcher3`和`Launcher3QuickStep`都会被`CarLauncher`取代，前面三个Launcher是手机系统的桌面，车载系统中会用`CarLauncher`这个定制新的桌面取代掉手机系统桌面。同样的，如果我们不想使用系统中自带的`CarLauncher`，那么的我们也需要在`overrides`中覆盖掉`CarLauncher`。

在自主开发的Car Android系统中这个属性我们会经常用到，用我们自己定制的各种APP来取代系统中默认的APP，比如系统设置等等。

### AndroidManifest.xml

Manifest文件中我们可以看到CarLauncher所需要的权限，以及入口Activity。

```
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.android.car.carlauncher">

    <uses-permission android:name="android.car.permission.ACCESS_CAR_PROJECTION_STATUS" />
    <!--  System permission to host maps activity  -->
    <uses-permission android:name="android.permission.ACTIVITY_EMBEDDING" />
    <!--  System permission to send events to hosted maps activity  -->
    <uses-permission android:name="android.permission.INJECT_EVENTS" />
    <!--  System permission to use internal system windows  -->
    <uses-permission android:name="android.permission.INTERNAL_SYSTEM_WINDOW" />
    <!--  System permissions to bring hosted maps activity to front on main display  -->
    <uses-permission android:name="android.permission.MANAGE_ACTIVITY_STACKS" />
    <!--  System permission to query users on device  -->
    <uses-permission android:name="android.permission.MANAGE_USERS" />
    <!--  System permission to control media playback of the active session  -->
    <uses-permission android:name="android.permission.MEDIA_CONTENT_CONTROL" />
    <!--  System permission to get app usage data  -->
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
    <!--  System permission to query all installed packages  -->
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.REORDER_TASKS" />
    <!--  To connect to media browser services in other apps, media browser clients
        that target Android 11 need to add the following in their manifest  -->
    <queries>
        <intent>
            <action android:name="android.media.browse.MediaBrowserService" />
        </intent>
    </queries>
    <application
        android:icon="@drawable/ic_launcher_home"
        android:label="@string/app_title"
        android:supportsRtl="true"
        android:theme="@style/Theme.Launcher">
        <activity
            android:name=".CarLauncher"
            android:clearTaskOnLaunch="true"
            android:configChanges="uiMode|mcc|mnc"
            android:launchMode="singleTask"
            android:resumeWhilePausing="true"
            android:stateNotNeeded="true"
            android:windowSoftInputMode="adjustPan">
            <meta-data
                android:name="distractionOptimized"
                android:value="true" />
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
        <activity
            android:name=".AppGridActivity"
            android:exported="true"
            android:launchMode="singleInstance"
            android:theme="@style/Theme.Launcher.AppGridActivity">
            <meta-data
                android:name="distractionOptimized"
                android:value="true" />
        </activity>
    </application>
</manifest>

```

关于Manifest我们重点来了解其中一些不常用的标签即可。

#### **<queries/>**

`<queries/>`是在Android 11 上为了收紧应用权限而引入的。用于，指定当前应用程序要与之交互的其他应用程序集，这些其他应用程序可以通过 package、intent、provider。示例：

```
<queries>
    <package android:name="string" />
    <intent>
        ...
    </intent>
    <provider android:authorities="list" />
    ...
</queries>

```

更多内容可以参考：[Android Developers | <queries>](https://developer.android.google.cn/guide/topics/manifest/queries-element?hl=cn)

#### **android:clearTaskOnLaunch = "true"**

每次启动都启动根Activity，并清理其他的Activity。

#### **android:configChanges="uiMode|mcc|mnc"**

不废话，直接上一张相对完整的表格供参考。
|VALUE  | DESCRIPTION  |
|--|--|
| mcc | 国际移动用户识别码所属国家代号是改变了,sim被侦测到了，去更新mcc    MCC是移动用户所属国家代号 |
| mnc | 国际移动用户识别码的移动网号码是改变了, sim被侦测到了，去更新mnc    MNC是移动网号码，最多由两位数字组成，用于识别移动用户所归属的移动通信网|
| locale | 用户所在区域发生变化。例如：用户切换了语言时，切换后的语言会显示出来|
| touchscreen | 触摸屏发生改变|
| keyboard | 键盘发生了改变。例如：用户介入了外部的键盘|
| keyboardHidden | 键盘的可用性发生了改变 |
| navigation | 导航发生了变化 |
| screenLayout | 屏幕的显示发生了变化。例如：不同的显示被激活 |
| fontScale | 字体比例发生了变化。例如：选择了不同的全局字体 |
| uiMode | 用户的模式发生了变化 |
| orientation | 屏幕方向改变了。例如：横竖屏切换 |
| smallestScreenSize | 屏幕的物理大小改变了。例如：连接到一个外部的屏幕上 |

#### **android:resumeWhilePausing = "true"**

当前一个Activity还在执行onPause()方法时（即在暂停过程中，还没有完全暂停），允许该Activity显示（此时Activity不能申请任何其他额外的资源，比如相机）

#### **android:stateNotNeeded="true"**

这个属性默认情况为false，若设为true，则当Activity重新启动时不会调用onSaveInstanceState方法，onCreate()方法中的Bundle参数将永远为null。在一些特殊场合下，由于用户按了Home键，该属性设置为true时，可以保证不用保存原先的状态引用，一定程度上节省空间资源。

## 总结

在实际的Car Launcher开发中，在Launcher中显示动态地图一直是一个比较头疼的技术难点，`CarLauncher`则选择使用`ActivityView`解决了这个问题，`ActivityView`属于AOSP的系统级模块，普通App无法引入，在Android Studio中也看不到它的源码。
`Carauncher`的源码分析因为源码比较多，导致篇幅拉长，为了阅读的舒适度决定单独写成一篇，关于`ActivityView`的使用以及原理，我们留到 Android车载应用开发与分析（2） - CarLauncher(下) 中再来做详细的分析。

## 参考资料

[Android Developers | <queries>](https://developer.android.google.cn/guide/topics/manifest/queries-element?hl=cn)
