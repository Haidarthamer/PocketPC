# Build evidence

| field | value |
| --- | --- |
| commit | `e7456f4a4f250d1bc28391414b2e84cb4022b88b` |
| run | [42900686214](https://github.com/Haidarthamer/PocketPC/actions/runs/42900686214) |
| date | 2026-11-10T21:03:06Z |
| java | openjdk version "17.0.21" 2026-10-20 |
| gradle | Gradle 8.11.1 |
| result | **success** |

## Unit tests
```
<testsuite name="dev.pocketpc.system.capability.CapabilityEvaluatorTest" tests="26" skipped="0" failures="0" errors="0" timestamp="2026-11-10T21:01:48" hostname="runnervmwuk1x" time="0.052">
```

## Gradle output (tail)
```
> Task :system:capability-android:extractDeepLinksDebug FROM-CACHE
> Task :graphics:compositor:processDebugManifest
> Task :system:capability-android:processDebugManifest
> Task :system:capability-android:compileDebugLibraryResources
> Task :graphics:compositor:compileDebugLibraryResources
> Task :system:capability-android:parseDebugLocalResources
> Task :graphics:compositor:parseDebugLocalResources
> Task :graphics:compositor:generateDebugRFile
> Task :system:capability-android:generateDebugRFile
> Task :graphics:compositor:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :system:capability-android:checkKotlinGradlePluginConfigurationErrors SKIPPED

> Task :system:capability-android:compileDebugKotlin
w: file:///home/runner/work/PocketPC/PocketPC/system/capability-android/src/main/kotlin/dev/pocketpc/system/capability/android/AndroidHardwareSource.kt:55:63 'static field CPU_ABI: String!' is deprecated. Deprecated in Java.

> Task :graphics:compositor:javaPreCompileDebug
> Task :system:capability-android:javaPreCompileDebug FROM-CACHE
> Task :system:capability-android:compileDebugJavaWithJavac NO-SOURCE
> Task :graphics:compositor:compileDebugKotlin
> Task :graphics:compositor:compileDebugJavaWithJavac NO-SOURCE
> Task :system:capability-android:bundleLibCompileToJarDebug
> Task :graphics:compositor:bundleLibCompileToJarDebug
> Task :graphics:compositor:mergeDebugShaders
> Task :system:capability-android:mergeDebugShaders
> Task :graphics:compositor:compileDebugShaders NO-SOURCE
> Task :graphics:compositor:generateDebugAssets UP-TO-DATE
> Task :system:desktop:compileKotlin
> Task :system:capability-android:compileDebugShaders NO-SOURCE
> Task :system:capability-android:generateDebugAssets UP-TO-DATE
> Task :system:desktop:compileJava NO-SOURCE
> Task :system:desktop:classes UP-TO-DATE
> Task :system:capability-android:packageDebugAssets
> Task :graphics:compositor:packageDebugAssets
> Task :system:capability-android:bundleLibRuntimeToJarDebug
> Task :system:desktop:jar
> Task :app:checkDebugAarMetadata
> Task :graphics:compositor:bundleLibRuntimeToJarDebug
> Task :app:mapDebugSourceSetPaths
> Task :app:packageDebugResources
> Task :app:parseDebugLocalResources
> Task :app:createDebugCompatibleScreenManifests
> Task :app:extractDeepLinksDebug FROM-CACHE
> Task :graphics:compositor:processDebugJavaRes
> Task :system:capability-android:processDebugJavaRes
> Task :system:capability-android:bundleLibRuntimeToDirDebug
> Task :graphics:compositor:bundleLibRuntimeToDirDebug
> Task :app:processDebugMainManifest
> Task :app:mergeDebugResources
> Task :graphics:compositor:mergeDebugJniLibFolders
> Task :graphics:compositor:mergeDebugNativeLibs NO-SOURCE
> Task :system:capability-android:mergeDebugJniLibFolders
> Task :system:capability-android:mergeDebugNativeLibs NO-SOURCE
> Task :graphics:compositor:copyDebugJniLibsProjectOnly
> Task :system:capability-android:copyDebugJniLibsProjectOnly
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
> Task :app:processDebugManifestForPackage
> Task :app:mergeDebugNativeLibs
> Task :app:processDebugResources
> Task :app:mergeExtDexDebug
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
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_5-1794344511784.json

BUILD SUCCESSFUL in 1m 11s
77 actionable tasks: 69 executed, 6 from cache, 2 up-to-date
## APK
path: /home/runner/work/PocketPC/PocketPC/app/build/outputs/apk/debug/app-debug.apk
-rw-r--r-- 1 runner runner 9851652 Nov 10 21:03 /home/runner/work/PocketPC/PocketPC/app/build/outputs/apk/debug/app-debug.apk
sha256: cffc950a89cc626bcfc3f15e98791423a8f0549bd383816457e0387c7ca0538d
package: name='dev.pocketpc.app' versionCode='1' versionName='0.1.0-phase1' platformBuildVersionName='15' platformBuildVersionCode='35' compileSdkVersion='35' compileSdkVersionCodename='15'
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
download: https://github.com/Haidarthamer/PocketPC/actions/runs/42900686214/artifacts
```
