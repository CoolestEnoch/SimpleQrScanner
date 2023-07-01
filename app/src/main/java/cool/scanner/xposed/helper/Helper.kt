package cool.scanner.xposed.helper

import android.app.AndroidAppHelper
import android.content.Context
import android.os.Build


fun getCurrentContext() = AndroidAppHelper.currentApplication().createPackageContext(
    AndroidAppHelper.currentPackageName(),
    Context.CONTEXT_IGNORE_SECURITY
)

fun isTPlus(): Boolean {
    return Build.VERSION.SDK_INT >= 33
}