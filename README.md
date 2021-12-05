在之前的[Android车载应用开发与分析（1） - Android Automotive概述与编译](https://www.jianshu.com/p/bbc02e0f6575)中了解了如何下载以及编译面向车载IVI的Android系统，一切顺利的话，运行模拟器，等待启动动画播放完毕后，我们所能看到的第一个APP就是车载android的桌面，而这就是本篇文章的重点 - CarLauncher。

本篇文章以解析Android 11 源码中`CarLauncher`为主。为了便于阅读源码，现将`CarLauncher`的源码整理成可以导入Android Studio的结构，源码地址：https://github.com/linux-link/CarLauncher。由于`CarLauncher`对于源码存在依赖，该项目不能直接运行，引入jar依赖的方式也不完全正确，仅供阅读使用。

本篇文章中的功能以及源码分析基于*android-11.0.0_r43，*`CarLauncher`源码位于 ***packages/apps/Car/Launcher***

## Launcher 与 CarLauncher
> Launcher是安卓系统中的桌面启动器，安卓系统的桌面UI统称为Launcher。Launcher是安卓系统中的主要程序组件之一，安卓系统中如果没有Launcher就无法启动[安卓桌面](https://baike.baidu.com/item/安卓桌面)，Launcher出错的时候，安卓系统会出现“进程 com.android.launcher 意外停止”的提示窗口。这时需要重新启动Launcher。
> 来自《百度百科 - launcher》

`Launcher`是android系统的桌面，是用户接触到的第一个带有界面的APP。它本质上就是一个系统级APP，和普通的APP一样，它界面也是在Activity上绘制出来的。
虽然`Launcher`也是一个APP，但是它涉及到的技术点却比一般的APP要多。CarLauncher作为IVI系统的桌面，需要显示系统中所有**用户可用app**的入口，显示最近用户使用的APP，同时还需要支持在桌面上动态显示如地图、音乐在内各个APP内部的信息，在桌面显示地图并与之进行简单的交互。地图开发的工作量极大，`Launcher`显然不可能引入地图的SDK再开发一个地图应用，那么如何在不扩大工作量的前提下动态的显示地图就成了`CarLauncher`的一个技术难点(该内容涉及的知识点多且杂，还没整理好`=_=||`就放到之后再介绍吧)。

## CarLauncher 功能分析
原生的`Carlaunher`代码并不复杂，主要是协同SystemUI完成以下两个功能。
*   显示 可以快捷的首页
    ![](https://upload-images.jianshu.io/upload_images/3146091-c215149f464b1fe1?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
*   显示 所有APP入口的桌面
    ![](https://upload-images.jianshu.io/upload_images/3146091-83b1e2578c354e4e?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

需要注意的是，只有红框中的内容才属于`CarLauncher`的内容，红框之外的属于`SystemUI`的内容。虽然`SystemUI`在下方的NaviBar有6个按钮，但是只有点击**首页**和**App桌面**才会进入**CarLauncher**，点击其它按钮都会进入其它APP，所以都不在本篇文章的分析范围。

## CarLauncher 源码分析
`CarLauncher`的源码结构如下：
![](https://upload-images.jianshu.io/upload_images/3146091-10208cd2926fc728?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
### Android.dp
**CarLauncher**的`android.bp`相对比较简单，定义了CarLauncher的源码结构，和依赖的类库。如果你对`android.bp`完全不了解，可以先看一下 [Android.bp入门教程](https://www.jianshu.com/p/f23e18933122) 学习一下基础的语法，再来回过头来看**CarLauncher**的`android.bp`相信会容易理解很多。

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

上述Android.bp中我们需要注意一个属性`overrides`，它表示覆盖的意思。在系统编译时`Launcher2`、`Launcher3`和`Launcher3QuickStep`都会被`CarLauncher`取代，前面三个Launcher是手机系统的桌面，车载系统中会用`CarLauncher`这个定制新的桌面取代掉手机系统桌面。同样的，如果我们不想使用系统中自带的`CarLauncher`，那么也需要在`overrides`中覆盖掉`CarLauncher`。在自主开发的车载Android系统中这个属性我们会经常用到，用我们自己定制的各种APP来取代系统中默认的APP，比如系统设置等等。

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

关于android:configChanges就不废话了，直接上一张相对完整的表格供参考。

| **VALUE** | **DESCRIPTION**|
| ----|-----|
|mcc| 国际移动用户识别码所属国家代号是改变了,sim被侦测到了，去更新mcc MCC是移动用户所属国家代号|
|mnc|国际移动用户识别码的移动网号码是改变了, sim被侦测到了，去更新mnc MNC是移动网号码，最多由两位数字组成，用于识别移动用户所归属的移动通信网|
|locale|用户所在区域发生变化。例如：用户切换了语言时，切换后的语言会显示出来|
|touchscreen| 触摸屏发生改变|
|keyboard|键盘发生了改变。例如：用户介入了外部的键盘|
|keyboardHidden|键盘的可用性发生了改变|
|navigation|导航发生了变化|
|screenLayout|屏幕的显示发生了变化。例如：不同的显示被激活|
|fontScale|字体比例发生了变化。例如：选择了不同的全局字体|
|uiMode|用户的模式发生了变化|
|orientation|屏幕方向改变了。例如：横竖屏切换|
|smallestScreenSize|屏幕的物理大小改变了。例如：连接到一个外部的屏幕上|

#### **android:resumeWhilePausing = "true"**
当前一个Activity还在执行onPause()方法时（即在暂停过程中，还没有完全暂停），允许该Activity显示（此时Activity不能申请任何其他额外的资源，比如相机）

#### **android:stateNotNeeded="true"**
这个属性默认情况为false，若设为true，则当Activity重新启动时不会调用onSaveInstanceState方法，onCreate()方法中的Bundle参数将永远为null。在一些特殊场合下，由于用户按了Home键，该属性设置为true时，可以保证不用保存原先的状态引用，一定程度上节省空间资源。

#### **android:name="distractionOptimized"**
设定当前Activity处于活动状态，是否导致驾驶员分心，在国外车载Android应用程序需要遵守Android官方制定《驾驶员分心指南》，这个规则在国内使用的很少，具体请参考[Driver Distraction Guidelines  |  Android Open Source Project](https://source.android.google.cn/devices/automotive/driver_distraction/guidelines)

### AppGridActivity
`AppGridActivity`用来显示系统中所有的APP，为用户的使用提供入口。
![image](https://upload-images.jianshu.io/upload_images/3146091-77901fda6490bf03?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

作为应用开发者，我们需要关注以下两个功能是如何实现的。
*   显示系统中所有的APP，并过滤掉一些不需要显示在桌面的APP（例如：后台的Service）
*   显示最近使用的APP

#### 显示系统中所有的APP（All App）
`CarLauncher`中用于筛选所有APP的方法都集中在`AppLauncherUtils`
```
/**
 * 获取我们希望在启动器中以未排序的顺序看到的所有组件，包括启动器活动和媒体服务。
 *
 * @param blackList             要隐藏的应用程序（包名称）列表（可能为空）
 * @param customMediaComponents 不应在Launcher中显示的媒体组件（组件名称）列表（可能为空），因为将显示其应用程序的Launcher活动
 * @param appTypes              要显示的应用程序类型（例如：全部或仅媒体源）
 * @param openMediaCenter       当用户选择媒体源时，启动器是否应导航到media center。
 * @param launcherApps          {@link LauncherApps}系统服务
 * @param carPackageManager     {@link CarPackageManager}系统服务
 * @param packageManager        {@link PackageManager}系统服务
 * @return 一个新的 {@link LauncherAppsInfo}
 */
@NonNull
static LauncherAppsInfo getLauncherApps(
        @NonNull Set<String> blackList,
        @NonNull Set<String> customMediaComponents,
        @AppTypes int appTypes,
        boolean openMediaCenter,
        LauncherApps launcherApps,
        CarPackageManager carPackageManager,
        PackageManager packageManager,
        CarMediaManager carMediaManager) {

    if (launcherApps == null || carPackageManager == null || packageManager == null
            || carMediaManager == null) {
        return EMPTY_APPS_INFO;
    }
    // 检索所有符合给定intent的服务
    List<ResolveInfo> mediaServices = packageManager.queryIntentServices(
            new Intent(MediaBrowserService.SERVICE_INTERFACE),
            PackageManager.GET_RESOLVED_FILTER);
    // 检索指定packageName的Activity的列表
    List<LauncherActivityInfo> availableActivities =
            launcherApps.getActivityList(null, Process.myUserHandle());

    Map<ComponentName, AppMetaData> launchablesMap = new HashMap<>(
            mediaServices.size() + availableActivities.size());
    Map<ComponentName, ResolveInfo> mediaServicesMap = new HashMap<>(mediaServices.size());

    // Process media services
    if ((appTypes & APP_TYPE_MEDIA_SERVICES) != 0) {
        for (ResolveInfo info : mediaServices) {
            String packageName = info.serviceInfo.packageName;
            String className = info.serviceInfo.name;
            ComponentName componentName = new ComponentName(packageName, className);
            mediaServicesMap.put(componentName, info);
            if (shouldAddToLaunchables(componentName, blackList, customMediaComponents,
                    appTypes, APP_TYPE_MEDIA_SERVICES)) {
                final boolean isDistractionOptimized = true;

                Intent intent = new Intent(Car.CAR_INTENT_ACTION_MEDIA_TEMPLATE);
                intent.putExtra(Car.CAR_EXTRA_MEDIA_COMPONENT, componentName.flattenToString());

                AppMetaData appMetaData = new AppMetaData(
                    info.serviceInfo.loadLabel(packageManager),
                    componentName,
                    info.serviceInfo.loadIcon(packageManager),
                    isDistractionOptimized,
                    context -> {
                        if (openMediaCenter) {
                            AppLauncherUtils.launchApp(context, intent);
                        } else {
                            selectMediaSourceAndFinish(context, componentName, carMediaManager);
                        }
                    },
                    context -> {
                        // 返回系统中所有MainActivity带有Intent.CATEGORY_INFO 和 Intent.CATEGORY_LAUNCHER的intent
                        Intent packageLaunchIntent =
                                packageManager.getLaunchIntentForPackage(packageName);
                        AppLauncherUtils.launchApp(context,
                                packageLaunchIntent != null ? packageLaunchIntent : intent);
                    });
                launchablesMap.put(componentName, appMetaData);
            }
        }
    }

    // Process activities
    if ((appTypes & APP_TYPE_LAUNCHABLES) != 0) {
        for (LauncherActivityInfo info : availableActivities) {
            ComponentName componentName = info.getComponentName();
            String packageName = componentName.getPackageName();
            if (shouldAddToLaunchables(componentName, blackList, customMediaComponents,
                    appTypes, APP_TYPE_LAUNCHABLES)) {
                boolean isDistractionOptimized =
                    isActivityDistractionOptimized(carPackageManager, packageName,
                        info.getName());

                Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setComponent(componentName)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // 获取app的name，和 app的图标
                AppMetaData appMetaData = new AppMetaData(
                    info.getLabel(),
                    componentName,
                    info.getBadgedIcon(0),
                    isDistractionOptimized,
                    context -> AppLauncherUtils.launchApp(context, intent),
                    null);
                launchablesMap.put(componentName, appMetaData);
            }
        }
    }

    return new LauncherAppsInfo(launchablesMap, mediaServicesMap);
}
```
上述代码为我们展示了，通过**LauncherApps.getActivityList()**返回的**List<LauncherActivityInfo>**即包含了系统中所有配置了`Intent#ACTION_MAIN` 和 `Intent#CATEGORY_LAUNCHER`的Activity信息。
**String LauncherActivityInfogetLabel()** : 获取app的name
**String LauncherActivityInfo.getComponentName()** : 获取app的Mainactivity信息
**Drawable LauncherActivityInfo.getBadgedIcon(0)** : 获取App的图标
最后，当用户点击图标时，虽然也是通过startActivity启动App，但是ActivityOptions可以让我们决定目标APP在哪个屏幕上启动，这对当前车载多屏系统而言非常重要。

```
static void launchApp(Context context, Intent intent) {
    ActivityOptions options = ActivityOptions.makeBasic();
    // 在当前的屏幕上启动目标App的Activity
    options.setLaunchDisplayId(context.getDisplayId());
    context.startActivity(intent, options.toBundle());
}
```

#### 显示最近使用的APP （Recent APP）
Android系统中提供了`UsageStatusManager`来提供对设备使用情况历史记录和统计信息的访问，`UsageStatusManager` 使用以下方法时不需要添加额外的权限。
`android.provider.Settings#ACTION_USAGE_ACCESS_SETTINGS`
`getAppStandbyBucket()`
`queryEventsForSelf(long,long)`
但是除此以外的方法都需要`android.permission.PACKAGE_USAGE_STATS`权限。
```
/**
 * 请注意，为了从上一次boot中获得使用情况统计数据，设备必须经过干净的关闭过程。
 */
private List<AppMetaData> getMostRecentApps(LauncherAppsInfo appsInfo) {
    ArrayList<AppMetaData> apps = new ArrayList<>();
    if (appsInfo.isEmpty()) {
        return apps;
    }

    // 获取从1年前开始的使用情况统计数据，返回如下条目：
    // "During 2017 App A is last used at 2017/12/15 18:03"
    // "During 2017 App B is last used at 2017/6/15 10:00"
    // "During 2018 App A is last used at 2018/1/1 15:12"
    List<UsageStats> stats =
            mUsageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_YEARLY,
                    System.currentTimeMillis() - DateUtils.YEAR_IN_MILLIS,
                    System.currentTimeMillis());

    if (stats == null || stats.size() == 0) {
        return apps; // empty list
    }

    stats.sort(new LastTimeUsedComparator());

    int currentIndex = 0;
    int itemsAdded = 0;
    int statsSize = stats.size();
    int itemCount = Math.min(mColumnNumber, statsSize);
    while (itemsAdded < itemCount && currentIndex < statsSize) {
        UsageStats usageStats = stats.get(currentIndex);
        String packageName = usageStats.mPackageName;
        currentIndex++;

        // 不包括自己
        if (packageName.equals(getPackageName())) {
            continue;
        }

        // TODO(b/136222320): 每个包都可以获得UsageStats，但一个包可能包含多个媒体服务。我们需要找到一种方法来获取每个服务的使用率统计数据。
        ComponentName componentName = AppLauncherUtils.getMediaSource(mPackageManager,
                packageName);
        // 免除媒体服务的后台和启动器检查
        if (!appsInfo.isMediaService(componentName)) {
            // 不要包括仅在后台运行的应用程序
            if (usageStats.getTotalTimeInForeground() == 0) {
                continue;
            }
            // 不要包含不支持从启动器启动的应用程序
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null || !intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                continue;
            }
        }

        AppMetaData app = appsInfo.getAppMetaData(componentName);
        // 防止重复条目
        // e.g. app is used at 2017/12/31 23:59, and 2018/01/01 00:00
        if (app != null && !apps.contains(app)) {
            apps.add(app);
            itemsAdded++;
        }
    }
    return apps;
}
```
### 参考资料
[Android Developers | <queries>](https://developer.android.google.cn/guide/topics/manifest/queries-element?hl=cn)
