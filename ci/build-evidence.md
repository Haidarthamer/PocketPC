# Build evidence

| field | value |
| --- | --- |
| commit | `971f55fc6bb84be76ee2894fa2816ec1e6e6ba67` |
| run | [42891138883](https://github.com/Haidarthamer/PocketPC/actions/runs/42891138883) |
| date | 2026-11-10T20:04:52Z |
| java | openjdk version "17.0.21" 2026-10-20 |
| gradle | Gradle 8.11.1 |
| result | **success** |

## Unit tests
```
<testsuite name="dev.pocketpc.system.capability.CapabilityEvaluatorTest" tests="17" skipped="0" failures="0" errors="0" timestamp="2026-11-10T20:03:33" hostname="runnervmwuk1x" time="0.048">
```

## Gradle output (tail)
```

BUILD SUCCESSFUL in 26s
4 actionable tasks: 4 executed
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.11.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
> Task :app:preBuild UP-TO-DATE
> Task :system:capability:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :system:capability-android:preBuild UP-TO-DATE
> Task :system:capability-android:preDebugBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:generateDebugResValues
> Task :system:capability-android:generateDebugResValues
> Task :system:capability-android:writeDebugAarMetadata
> Task :system:capability-android:generateDebugResources
> Task :app:generateDebugResources
> Task :system:capability:compileKotlin UP-TO-DATE
> Task :system:capability:compileJava NO-SOURCE
> Task :system:capability:processResources NO-SOURCE
> Task :system:capability:classes UP-TO-DATE
> Task :system:capability:jar UP-TO-DATE
> Task :system:capability-android:packageDebugResources
> Task :system:capability-android:extractDeepLinksDebug
> Task :system:capability-android:processDebugManifest
> Task :system:capability-android:compileDebugLibraryResources
> Task :system:capability-android:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :system:capability-android:javaPreCompileDebug
> Task :system:capability-android:mergeDebugShaders
> Task :system:capability-android:compileDebugShaders NO-SOURCE
> Task :system:capability-android:generateDebugAssets UP-TO-DATE
> Task :system:capability-android:packageDebugAssets
> Task :system:capability-android:parseDebugLocalResources
> Task :system:capability-android:mergeDebugJniLibFolders
> Task :system:capability-android:mergeDebugNativeLibs NO-SOURCE
> Task :system:capability-android:copyDebugJniLibsProjectOnly
> Task :system:capability-android:generateDebugRFile
> Task :app:checkDebugAarMetadata
> Task :app:mapDebugSourceSetPaths
> Task :app:packageDebugResources
> Task :app:createDebugCompatibleScreenManifests
> Task :app:extractDeepLinksDebug FROM-CACHE
> Task :app:parseDebugLocalResources
> Task :app:processDebugMainManifest
> Task :app:mergeDebugResources
> Task :app:processDebugManifest
> Task :app:javaPreCompileDebug FROM-CACHE
> Task :app:mergeDebugShaders
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets
> Task :app:compressDebugAssets
> Task :app:desugarDebugFileDependencies
> Task :app:mergeDebugJniLibFolders
> Task :app:processDebugManifestForPackage
> Task :app:checkDebugDuplicateClasses

> Task :system:capability-android:compileDebugKotlin
w: file:///home/runner/work/PocketPC/PocketPC/system/capability-android/src/main/kotlin/dev/pocketpc/system/capability/android/AndroidHardwareSource.kt:55:63 'static field CPU_ABI: String!' is deprecated. Deprecated in Java.

> Task :system:capability-android:compileDebugJavaWithJavac NO-SOURCE
> Task :app:processDebugResources
> Task :app:mergeExtDexDebug
> Task :system:capability-android:bundleLibCompileToJarDebug
> Task :system:capability-android:bundleLibRuntimeToJarDebug
> Task :system:capability-android:processDebugJavaRes
> Task :system:capability-android:bundleLibRuntimeToDirDebug
> Task :app:mergeLibDexDebug
> Task :app:mergeDebugNativeLibs
> Task :app:validateSigningDebug
> Task :app:writeDebugAppMetadata
> Task :app:writeDebugSigningConfigVersions

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so.

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
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_5-1794341017218.json

BUILD SUCCESSFUL in 1m 16s
56 actionable tasks: 52 executed, 2 from cache, 2 up-to-date
## APK
path: /home/runner/work/PocketPC/PocketPC/app/build/outputs/apk/debug/app-debug.apk
-rw-r--r-- 1 runner runner 9485534 Nov 10 20:04 /home/runner/work/PocketPC/PocketPC/app/build/outputs/apk/debug/app-debug.apk
sha256: 9a93de283c637ace71d0a12ba83a8c40a7cdb198a8b2e20387762d8b2f0f7564
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
bytes: 9485534
download: https://github.com/Haidarthamer/PocketPC/actions/runs/42891138883/artifacts
```
