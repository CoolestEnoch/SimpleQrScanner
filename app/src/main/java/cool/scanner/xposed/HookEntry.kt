package cool.scanner.xposed

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.*
import cool.scanner.xposed.hooks.hookMiuiSystemPlugin
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        val packageName: String = lpparam.packageName
        EzXHelperInit.initHandleLoadPackage(lpparam)


        XposedBridge.log("initd")

        //模块激活状态
        if (lpparam.packageName == "cool.scanner") {
            findMethod("cool.scanner.ui.MainActivity") {
                name == "checkXposed" && returnType == Boolean::class.java
            }.hookBefore {
                it.result = true
            }
        }

        // 系统界面组件
        if("com.android.systemui" == packageName){
            hookMiuiSystemPlugin(lpparam)
        }
    }
}