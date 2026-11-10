# Build evidence

| field | value |
| --- | --- |
| commit | `3138a34d85b5c6732127a2265442270385cffeed` |
| run | [42906093407](https://github.com/Haidarthamer/PocketPC/actions/runs/42906093407) |
| date | 2026-11-10T21:36:21Z |
| java | openjdk version "17.0.21" 2026-10-20 |
| gradle | Gradle 8.11.1 |
| result | **success** |

## Unit tests
```
<testsuite name="dev.pocketpc.graphics.GpuCapabilityIntegrationTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:55" hostname="runnervmwuk1x" time="0.09">
<testsuite name="dev.pocketpc.graphics.SoftwareRendererDetectorTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:55" hostname="runnervmwuk1x" time="0.003">
<testsuite name="dev.pocketpc.graphics.GpuVerifierTest" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:55" hostname="runnervmwuk1x" time="0.008">
<testsuite name="dev.pocketpc.desktop.shell.KeyBindingsTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:55" hostname="runnervmwuk1x" time="0.006">
<testsuite name="dev.pocketpc.desktop.shell.CalculatorTest" tests="9" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:55" hostname="runnervmwuk1x" time="0.025">
<testsuite name="dev.pocketpc.desktop.shell.ShellModelTest" tests="9" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:55" hostname="runnervmwuk1x" time="0.004">
<testsuite name="dev.pocketpc.desktop.wm.PlacementSnapTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:56" hostname="runnervmwuk1x" time="0.011">
<testsuite name="dev.pocketpc.desktop.shell.AppRegistryTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:55" hostname="runnervmwuk1x" time="0.036">
<testsuite name="dev.pocketpc.desktop.wm.WindowManagerTest" tests="29" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:56" hostname="runnervmwuk1x" time="0.012">
<testsuite name="dev.pocketpc.desktop.wm.SnapWidthRegressionTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:56" hostname="runnervmwuk1x" time="0.004">
<testsuite name="dev.pocketpc.desktop.wm.WorkspaceManagerTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:56" hostname="runnervmwuk1x" time="0.006">
<testsuite name="dev.pocketpc.desktop.wm.TileLayoutTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:56" hostname="runnervmwuk1x" time="0.004">
<testsuite name="dev.pocketpc.system.capability.CapabilityEvaluatorTest" tests="26" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:34:49" hostname="runnervmwuk1x" time="0.145">
```

Failure details:
```
(none)
```

Totals:
```
13 test suites, 120 tests, 0 failures, 0 errors, 0 skipped
```

## Gradle output (tail)
```
> Task :graphics:compositor:compileDebugLibraryResources
> Task :system:capability-android:compileDebugLibraryResources
> Task :graphics:compositor:parseDebugLocalResources UP-TO-DATE
> Task :system:capability-android:parseDebugLocalResources FROM-CACHE
> Task :graphics:compositor:generateDebugRFile UP-TO-DATE
> Task :graphics:compositor:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :system:capability-android:generateDebugRFile
> Task :system:capability-android:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :graphics:compositor:compileDebugKotlin UP-TO-DATE
> Task :graphics:compositor:javaPreCompileDebug UP-TO-DATE
> Task :graphics:compositor:compileDebugJavaWithJavac NO-SOURCE
> Task :graphics:compositor:bundleLibCompileToJarDebug UP-TO-DATE
> Task :graphics:compositor:mergeDebugShaders
> Task :graphics:compositor:compileDebugShaders NO-SOURCE
> Task :graphics:compositor:generateDebugAssets UP-TO-DATE
> Task :graphics:compositor:packageDebugAssets
> Task :graphics:compositor:bundleLibRuntimeToJarDebug UP-TO-DATE
> Task :system:capability-android:javaPreCompileDebug FROM-CACHE
> Task :system:capability-android:mergeDebugShaders
> Task :system:capability-android:compileDebugShaders NO-SOURCE
> Task :system:capability-android:generateDebugAssets UP-TO-DATE
> Task :system:capability-android:packageDebugAssets
> Task :graphics:compositor:processDebugJavaRes UP-TO-DATE
> Task :graphics:compositor:bundleLibRuntimeToDirDebug
> Task :graphics:compositor:mergeDebugJniLibFolders
> Task :system:capability-android:mergeDebugJniLibFolders
> Task :system:capability-android:mergeDebugNativeLibs NO-SOURCE
> Task :graphics:compositor:mergeDebugNativeLibs NO-SOURCE
> Task :graphics:compositor:copyDebugJniLibsProjectOnly
> Task :system:capability-android:copyDebugJniLibsProjectOnly
> Task :app:checkDebugAarMetadata
> Task :app:mapDebugSourceSetPaths
> Task :app:createDebugCompatibleScreenManifests
> Task :app:parseDebugLocalResources
> Task :app:extractDeepLinksDebug FROM-CACHE
> Task :app:mergeDebugResources
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:javaPreCompileDebug FROM-CACHE
> Task :app:mergeDebugShaders
> Task :app:processDebugManifestForPackage
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets
> Task :app:compressDebugAssets
> Task :app:desugarDebugFileDependencies
> Task :app:processDebugResources
> Task :app:mergeDebugJniLibFolders
> Task :app:checkDebugDuplicateClasses
> Task :app:mergeDebugNativeLibs

> Task :system:capability-android:compileDebugKotlin
w: file:///home/runner/work/PocketPC/PocketPC/system/capability-android/src/main/kotlin/dev/pocketpc/system/capability/android/AndroidHardwareSource.kt:55:63 'static field CPU_ABI: String!' is deprecated. Deprecated in Java.

> Task :system:capability-android:compileDebugJavaWithJavac NO-SOURCE
> Task :app:mergeExtDexDebug
> Task :system:capability-android:bundleLibCompileToJarDebug
> Task :system:capability-android:processDebugJavaRes
> Task :system:capability-android:bundleLibRuntimeToJarDebug
> Task :system:capability-android:bundleLibRuntimeToDirDebug
> Task :app:mergeLibDexDebug

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so.

> Task :app:validateSigningDebug
> Task :app:writeDebugAppMetadata
> Task :app:writeDebugSigningConfigVersions
> Task :app:compileDebugKotlin
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:dexBuilderDebug
> Task :app:mergeDebugGlobalSynthetics
> Task :app:processDebugJavaRes
> Task :app:mergeProjectDexDebug
> Task :app:mergeDebugJavaResource
> Task :app:packageDebug
> Task :app:createDebugApkListingFileRedirect
> Task :app:assembleDebug
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_6-1794346500090.json

BUILD SUCCESSFUL in 1m 21s
77 actionable tasks: 55 executed, 8 from cache, 14 up-to-date
## APK
path: /home/runner/work/PocketPC/PocketPC/app/build/outputs/apk/debug/app-debug.apk
-rw-r--r-- 1 runner runner 9851652 Nov 10 21:36 /home/runner/work/PocketPC/PocketPC/app/build/outputs/apk/debug/app-debug.apk
sha256: 95eacb5ee350c819992ae25d838eadd7c15bdc8e5cb037e5f0ae1a50c92d8ea4
package: name='dev.pocketpc.app' versionCode='2' versionName='0.2.0-phase2' platformBuildVersionName='15' platformBuildVersionCode='35' compileSdkVersion='35' compileSdkVersionCodename='15'
targetSdkVersion:'35'
application-label:'Pocket PC'
application-icon-160:'res/mipmap-anydpi-v26/ic_launcher.xml'
application-icon-240:'res/mipmap-anydpi-v26/ic_launcher.xml'
application-icon-320:'res/mipmap-anydpi-v26/ic_launcher.xml'
application-icon-480:'res/mipmap-anydpi-v26/ic_launcher.xml'
application-icon-640:'res/mipmap-anydpi-v26/ic_launcher.xml'
application-icon-65534:'res/mipmap-anydpi-v26/ic_launcher.xml'
application: label='Pocket PC' icon='res/mipmap-anydpi-v26/ic_launcher.xml'
launchable-activity: name='dev.pocketpc.app.MainActivity'  label='Pocket PC' icon=''

Icon resources inside the APK:
  res/drawable/ic_launcher_background.xml (572 bytes)
  res/drawable/ic_launcher_foreground.xml (1748 bytes)
  res/drawable/ic_launcher_monochrome.xml (1480 bytes)
  res/mipmap-anydpi-v26/ic_launcher.xml (548 bytes)
  res/mipmap-anydpi-v26/ic_launcher_round.xml (548 bytes)
  res/mipmap-hdpi-v4/ic_launcher.png (2641 bytes)
  res/mipmap-hdpi-v4/ic_launcher_round.png (3351 bytes)
  res/mipmap-mdpi-v4/ic_launcher.png (1864 bytes)
  res/mipmap-mdpi-v4/ic_launcher_round.png (2328 bytes)
  res/mipmap-xhdpi-v4/ic_launcher.png (3540 bytes)
  res/mipmap-xhdpi-v4/ic_launcher_round.png (4631 bytes)
  res/mipmap-xxhdpi-v4/ic_launcher.png (5233 bytes)
  res/mipmap-xxhdpi-v4/ic_launcher_round.png (7221 bytes)
  res/mipmap-xxxhdpi-v4/ic_launcher.png (7023 bytes)
  res/mipmap-xxxhdpi-v4/ic_launcher_round.png (9622 bytes)

## Downloadable artifact
name: PocketPC-debug-apk
file: app-debug.apk
bytes: 9851652
download: https://github.com/Haidarthamer/PocketPC/actions/runs/42906093407/artifacts
```
