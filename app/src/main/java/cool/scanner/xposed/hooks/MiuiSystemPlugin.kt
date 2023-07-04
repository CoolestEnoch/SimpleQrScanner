package cool.scanner.xposed.hooks

import android.content.Context
import android.content.Intent
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import cool.scanner.xposed.helper.getCurrentContext
import cool.scanner.xposed.helper.isTPlus
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

fun hookMiuiSystemPlugin(lpparam: XC_LoadPackage.LoadPackageParam) {
    val pluginLoaderClass = when (isTPlus()) {
        true -> "com.android.systemui.shared.plugins.PluginInstance\$Factory"
        else -> "com.android.systemui.shared.plugins.PluginManagerImpl"
    }

    findMethod(pluginLoaderClass) {
        name == "getClassLoader"
    }.hookAfter {
        val mClassLoader = it.result as ClassLoader

        // 单击进入轻扫
        findMethod("miui.systemui.quicksettings.ScannerTile", classLoader = mClassLoader) {
            name == "handleClick"
        }.hookBefore {
            XposedBridge.log("hooked")
            try {
                /*val intent = Intent(getCurrentContext(), MainActivity::class.java).apply {
                    putExtra("startFromMIUIControlCentre", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }*/
                // 收起控制中心
                findMethod("miui.systemui.quicksettings.ScannerTile", classLoader = mClassLoader){name == "collapseStatusBar"}
                    .invoke(it.thisObject, it.thisObject.getObjectAs<Context>("mPluginContext", Context::class.java))

                // 拉起轻扫
                val intent = Intent("cool.scanner.START_SCAN_FROM_MIUI_CONTROL_CENTER").apply {
                    putExtra("startFromMIUIControlCentre", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                getCurrentContext().startActivity(intent)

                it.result = Unit
            } catch (e: Exception) {
                XposedBridge.log(e)
            }
        }

        // 长按进入小米扫一扫
        findMethod("miui.systemui.quicksettings.ScannerTile", classLoader = mClassLoader) {
            name == "getLongClickIntent"
        }.hookBefore {
            XposedBridge.log("hook long click")
            try {
                it.result = Intent().apply {
                    setClassName("com.xiaomi.scanner","com.xiaomi.scanner.app.ScanActivity")
                    action = "android.intent.action.MAIN"
                }
            } catch (e: Exception) {
                XposedBridge.log(e)
            }

        }
    }
}