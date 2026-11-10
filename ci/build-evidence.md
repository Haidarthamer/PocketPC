# Build evidence

| field | value |
| --- | --- |
| commit | `320a608786a9ce3fc131ba95807314249a1fdcb1` |
| run | [42905125390](https://github.com/Haidarthamer/PocketPC/actions/runs/42905125390) |
| date | 2026-11-10T21:29:35Z |
| java | openjdk version "17.0.21" 2026-10-20 |
| gradle | Gradle 8.11.1 |
| result | **success** |

## Unit tests
```
<testsuite name="dev.pocketpc.graphics.GpuCapabilityIntegrationTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:29" hostname="runnervmwuk1x" time="0.085">
<testsuite name="dev.pocketpc.graphics.SoftwareRendererDetectorTest" tests="4" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:29" hostname="runnervmwuk1x" time="0.001">
<testsuite name="dev.pocketpc.graphics.GpuVerifierTest" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:29" hostname="runnervmwuk1x" time="0.003">
<testsuite name="dev.pocketpc.desktop.shell.KeyBindingsTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.005">
<testsuite name="dev.pocketpc.desktop.shell.CalculatorTest" tests="9" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.017">
<testsuite name="dev.pocketpc.desktop.shell.ShellModelTest" tests="9" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.003">
<testsuite name="dev.pocketpc.desktop.wm.PlacementSnapTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.011">
<testsuite name="dev.pocketpc.desktop.shell.AppRegistryTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.023">
<testsuite name="dev.pocketpc.desktop.wm.WindowManagerTest" tests="29" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.009">
<testsuite name="dev.pocketpc.desktop.wm.SnapWidthRegressionTest" tests="3" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.002">
<testsuite name="dev.pocketpc.desktop.wm.WorkspaceManagerTest" tests="6" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.004">
<testsuite name="dev.pocketpc.desktop.wm.TileLayoutTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:30" hostname="runnervmwuk1x" time="0.002">
<testsuite name="dev.pocketpc.system.capability.CapabilityEvaluatorTest" tests="26" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:28:26" hostname="runnervmwuk1x" time="0.105">
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
> Task :system:capability-android:packageDebugResources
> Task :system:desktop:compileJava NO-SOURCE
> Task :system:desktop:classes UP-TO-DATE
> Task :system:desktop:jar UP-TO-DATE
> Task :system:capability-android:extractDeepLinksDebug FROM-CACHE
> Task :graphics:compositor:processDebugManifest
> Task :system:capability-android:processDebugManifest
> Task :system:capability-android:compileDebugLibraryResources
> Task :graphics:compositor:compileDebugLibraryResources
> Task :graphics:compositor:parseDebugLocalResources UP-TO-DATE
> Task :system:capability-android:parseDebugLocalResources FROM-CACHE
> Task :graphics:compositor:generateDebugRFile UP-TO-DATE
> Task :graphics:compositor:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :graphics:compositor:compileDebugKotlin UP-TO-DATE
> Task :graphics:compositor:javaPreCompileDebug UP-TO-DATE
> Task :graphics:compositor:compileDebugJavaWithJavac NO-SOURCE
> Task :graphics:compositor:bundleLibCompileToJarDebug UP-TO-DATE
> Task :system:capability-android:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :system:capability-android:javaPreCompileDebug FROM-CACHE
> Task :graphics:compositor:mergeDebugShaders
> Task :system:capability-android:generateDebugRFile
> Task :graphics:compositor:compileDebugShaders NO-SOURCE
> Task :graphics:compositor:generateDebugAssets UP-TO-DATE
> Task :graphics:compositor:packageDebugAssets
> Task :graphics:compositor:bundleLibRuntimeToJarDebug UP-TO-DATE
> Task :system:capability-android:mergeDebugShaders
> Task :system:capability-android:compileDebugShaders NO-SOURCE
> Task :system:capability-android:generateDebugAssets UP-TO-DATE
> Task :system:capability-android:packageDebugAssets
> Task :graphics:compositor:processDebugJavaRes UP-TO-DATE
> Task :graphics:compositor:bundleLibRuntimeToDirDebug
> Task :graphics:compositor:mergeDebugJniLibFolders
> Task :graphics:compositor:mergeDebugNativeLibs NO-SOURCE
> Task :system:capability-android:mergeDebugJniLibFolders
> Task :system:capability-android:mergeDebugNativeLibs NO-SOURCE
> Task :graphics:compositor:copyDebugJniLibsProjectOnly
> Task :system:capability-android:copyDebugJniLibsProjectOnly

> Task :system:capability-android:compileDebugKotlin
w: file:///home/runner/work/PocketPC/PocketPC/system/capability-android/src/main/kotlin/dev/pocketpc/system/capability/android/AndroidHardwareSource.kt:55:63 'static field CPU_ABI: String!' is deprecated. Deprecated in Java.

> Task :system:capability-android:compileDebugJavaWithJavac NO-SOURCE
> Task :system:capability-android:bundleLibRuntimeToJarDebug
> Task :system:capability-android:bundleLibCompileToJarDebug
> Task :system:capability-android:processDebugJavaRes
> Task :system:capability-android:bundleLibRuntimeToDirDebug
> Task :app:checkDebugAarMetadata
> Task :app:mapDebugSourceSetPaths
> Task :app:packageDebugResources
> Task :app:createDebugCompatibleScreenManifests
> Task :app:extractDeepLinksDebug FROM-CACHE
> Task :app:parseDebugLocalResources
> Task :app:mergeDebugResources
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:javaPreCompileDebug FROM-CACHE
> Task :app:mergeDebugShaders
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets
> Task :app:compressDebugAssets
> Task :app:desugarDebugFileDependencies
> Task :app:mergeDebugJniLibFolders
> Task :app:checkDebugDuplicateClasses
> Task :app:mergeDebugNativeLibs
> Task :app:processDebugManifestForPackage
> Task :app:mergeLibDexDebug

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so.

> Task :app:processDebugResources
> Task :app:validateSigningDebug
> Task :app:writeDebugAppMetadata
> Task :app:writeDebugSigningConfigVersions
> Task :app:mergeExtDexDebug
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
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_6-1794346113055.json

BUILD SUCCESSFUL in 1m 1s
77 actionable tasks: 55 executed, 8 from cache, 14 up-to-date
## APK
path: /home/runner/work/PocketPC/PocketPC/app/build/outputs/apk/debug/app-debug.apk
-rw-r--r-- 1 runner runner 9851652 Nov 10 21:29 /home/runner/work/PocketPC/PocketPC/app/build/outputs/apk/debug/app-debug.apk
sha256: f3991e9261907bd6bafc173915d1f6eb9dadead382cd271c3eb6cae9efbbe254
package: name='dev.pocketpc.app' versionCode='2' versionName='0.2.0-phase2' platformBuildVersionName='15' platformBuildVersionCode='35' compileSdkVersion='35' compileSdkVersionCodename='15'
minSdkVersion:'26'
targetSdkVersion:'35'
uses-permission: name='dev.pocketpc.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
application-label:'Pocket PC'
application-label-af:'Pocket PC'
application-label-am:'Pocket PC'
application-label-ar:'Pocket PC'
application-label-as:'Pocket PC'
application-label-az:'Pocket PC'
application-label-be:'Pocket PC'
application-label-bg:'Pocket PC'
application-label-bn:'Pocket PC'
application-label-bs:'Pocket PC'
application-label-ca:'Pocket PC'
application-label-cs:'Pocket PC'
application-label-da:'Pocket PC'
application-label-de:'Pocket PC'
application-label-el:'Pocket PC'
application-label-en-AU:'Pocket PC'

## Downloadable artifact
name: PocketPC-debug-apk
file: app-debug.apk
bytes: 9851652
download: https://github.com/Haidarthamer/PocketPC/actions/runs/42905125390/artifacts
```
