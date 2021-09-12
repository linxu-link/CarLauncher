Android车载应用开发与分析（2） - CarLauncher
在上一篇Android车载应用开发与分析（1） - Android Automotive概述与编译中我们了解了如何下载以及编译面向车载IVI的Android系统，一切顺利的话，运行模拟器可以看到如下的车载android的桌面，而这就是本篇文章的重点 - CarLauncher。


Launcher 与 CarLauncher
Launcher是安卓系统中的桌面启动器，安卓系统的桌面UI统称为Launcher。Launcher是安卓系统中的主要程序组件之一，安卓系统中如果没有Launcher就无法启动安卓桌面，Launcher出错的时候，安卓系统会出现“进程 com.android.launcher 意外停止”的提示窗口。这时需要重新启动Launcher。
来自《百度百科 - launcher》
Luancher是android系统的桌面，是用户接触到的第一个带有界面的APP。是的，它本质上就是一个系统级APP，和普通的APP一样，它界面也是在Activity上绘制出来的。

CarLauncher 源码分析
本篇源码分析基于android-11.0.0_r43，CarLauncher源码位置在 packages/apps/Car/Launcher
Android.dp
CarLauncher的android.bp相对比较简单，定义了CarLauncher的源码结构，和依赖的类库。如果你对android.bp完全不了解，可以先看一下 Android.bp入门教程 学习一下基础的语法，再来回过头来看CarLauncher的android.bp相信会容易理解很多。
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

上述Android.bp中我们需要注意一个属性overrides，它表示覆盖的意思。在系统编译时Launcher2、Launcher3和Launcher3QuickStep都会被CarLauncher取代，前面三个Launcher是手机系统的桌面，车载系统中会用CarLauncher来定制一个新的桌面。如果我们不希望使用系统中自带的CarLauncher，那么同样的我们也需要覆盖掉CarLauncher。
在自主开发的Car Android系统中这个属性我们会经常用到，用我们自己定制的各种APP来取代系统中默认的APP，比如系统设置等等。
AndroidManifest.xml
Manifest文件中我们可以看到CarLauncher所需要的权限，以及入口Activity。
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

关于Manifest我们重点来了解其中一些不常用的标签即可。
<queries/>
<queries/>是在Android 11 上为了收紧应用权限而引入的。用于，指定当前应用程序要与之交互的其他应用程序集，这些其他应用程序可以通过 package、intent、provider。示例：
<queries>
    <package android:name="string" />
    <intent>
        ...
    </intent>
    <provider android:authorities="list" />
    ...
</queries>

更多内容可以参考：Android Developers | <queries>
android:clearTaskOnLaunch = "true"
每次启动都启动根Activity，并清理其他的Activity。
android:configChanges="uiMode|mcc|mnc"
不废话，直接上一张相对完整的表格供参考。
VALUE
DESCRIPTION
mcc
国际移动用户识别码所属国家代号是改变了,sim被侦测到了，去更新mcc    MCC是移动用户所属国家代号
mnc
国际移动用户识别码的移动网号码是改变了, sim被侦测到了，去更新mnc    MNC是移动网号码，最多由两位数字组成，用于识别移动用户所归属的移动通信网
locale
 用户所在区域发生变化，一般是用户切换了语言时，切换后的语言会显示出来
touchscreen
触摸屏发生改变
keyboard
键盘发生了改变。例如：用户介入了外部的键盘
keyboardHidden
键盘的可用性发生了改变
navigation
导航发生了变化
screenLayout
屏幕的显示发生了变化。例如：不同的显示被激活
fontScale
 字体比例发生了变化。例如：选择了不同的全局字体
uiMode
用户的模式发生了变化
orientation
屏幕方向改变了。例如：横竖屏切换
smallestScreenSize
屏幕的物理大小改变了。例如：连接到一个外部的屏幕上

android:resumeWhilePausing = "true"
当前一个Activity还在执行onPause()方法时（即在暂停过程中，还没有完全暂停），允许该Activity显示（此时Activity不能申请任何其他额外的资源，比如相机）
android:stateNotNeeded="true"
这个属性默认情况为false，若设为true，则当Activity重新启动时不会调用onSaveInstanceState方法，onCreate()方法中的Bundle参数将永远为null。在一些特殊场合下，由于用户按了Home键，该属性设置为true时，可以保证不用保存原先的状态引用，一定程度上节省空间资源。
CarLauncherActivity
在实际的Car Launcher开发中，在Launcher中显示Map是一个比较头疼的技术点
CarLauncher 展望
参考资料
Android Developers | <queries>
