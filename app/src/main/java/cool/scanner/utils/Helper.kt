package cool.scanner.utils

import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity

fun getClipboard(context: Context): String? {
    var rtn: String? = null
    val manager: ClipboardManager =
        context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
    manager.let {
        if (manager.hasPrimaryClip() && manager.primaryClip!!.getItemCount() > 0) {
            val addedText: CharSequence = manager.primaryClip!!.getItemAt(0).getText()
            val content = addedText.toString()
            if (!TextUtils.isEmpty(content)) {
                rtn = content
            } else {
                rtn = null
            }
        }
    }
    return rtn
}