package cool.scanner.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.permissionx.guolindev.PermissionX
import cool.scanner.R
import cool.scanner.databinding.ActivityMainBinding
import cool.scanner.utils.getClipboard
import java.util.EnumMap


class MainActivity : AppCompatActivity() {

    private val QR_REQUEST_CODE = 1
    private val SELECT_FROM_GALLERY = 2

    lateinit var binding: ActivityMainBinding
    lateinit var clipBoardManager: ClipboardManager

    fun checkXposed() = false

    override fun onResume() {
        super.onResume()

        if(checkXposed()){
            Snackbar.make(window.decorView, "XPosed已激活", Snackbar.LENGTH_LONG).show()
        }

        // 由xposed调用
        if (intent.getBooleanExtra("startFromMIUIControlCentre", false)) {
            binding.btnScanQR.callOnClick()
        }

        //接收来自其他应用的分享
        if (intent.action.equals(Intent.ACTION_SEND) && intent.type.equals("image/*")) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            var bitmap: Bitmap? = null
            uri?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            }
            bitmap?.let { scanQrFromBitmapAndSetView(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //相关系统服务
        clipBoardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager


        binding.btnPaste.setOnClickListener {
            binding.etInputQrContent.setText(getClipboard(this@MainActivity))
        }

        binding.btnClear.setOnClickListener {
            Log.e("", "")
            binding.etInputQrContent.setText("")
            binding.etInputQrContent.clearFocus()
        }
        /*binding.btnClear.setOnLongClickListener {
            (it as FloatingActionButton).setImageResource(R.mipmap.cr200j)
            false
        }*/
        binding.btnClear.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    (view as FloatingActionButton).setImageResource(R.mipmap.cr200j)
                }

                MotionEvent.ACTION_UP -> {
                    (view as FloatingActionButton).setImageResource(R.drawable.baseline_delete_24)
                }
            }
            false
        }


        binding.sbQrSize.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            //当滑块进度改变时，会执行该方法下的代码
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val size = (i.toFloat() / 100.0 * resources.displayMetrics.widthPixels).toInt()
                val params = binding.ivGeneratedQr.layoutParams
                params.width = size
                params.height = size
                binding.ivGeneratedQr.layoutParams = params
//                Snackbar.make(window.decorView, "$size", Snackbar.LENGTH_LONG).show()
            }

            //当开始滑动滑块时，会执行该方法下的代码
            override fun onStartTrackingTouch(seekBar: SeekBar) {
//                Toast.makeText(this@MainActivity, "我seekbar开始滑动了", Toast.LENGTH_SHORT).show()
            }

            //当结束滑动滑块时，会执行该方法下的代码
            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                Toast.makeText(this@MainActivity, "我seekbar结束滑动了", Toast.LENGTH_SHORT).show()
            }
        })
        val size =
            (binding.sbQrSize.progress.toFloat() / 100 * resources.displayMetrics.widthPixels).toInt()
        val params = binding.ivGeneratedQr.layoutParams
        params.width = size
        params.height = size
        binding.ivGeneratedQr.layoutParams = params

        binding.btnScanQR.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // 无权限，申请授权
                Snackbar.make(window.decorView, "请授权相机访问以扫码", Snackbar.LENGTH_LONG).show()
                grantPermission(this as AppCompatActivity) {
                    // 有权限，开始扫码
                    Snackbar.make(window.decorView, "有权限，开始扫码", Snackbar.LENGTH_LONG).show()

                    val scanOptions = ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                        .setPrompt("hhh")
                        .setCameraId(0)
                        .setBeepEnabled(true)

                    val mIntent = ScanContract()
                        .createIntent(this@MainActivity, scanOptions)

                    /*startActivityIfNeeded(
                        Intent(
                            this@MainActivity,
                            CaptureActivity::class.java
                        ), QR_REQUEST_CODE
                    )*/
                    startActivityIfNeeded(
                        mIntent, QR_REQUEST_CODE
                    )
                }
            } else {
                val scanOptions = ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                    .setPrompt("hhh")
                    .setCameraId(0)
                    .setBeepEnabled(true)

                val mIntent = ScanContract()
                    .createIntent(this@MainActivity, scanOptions)

                /*startActivityIfNeeded(
                    Intent(
                        this@MainActivity,
                        CaptureActivity::class.java
                    ), QR_REQUEST_CODE
                )*/
                startActivityIfNeeded(
                    mIntent, QR_REQUEST_CODE
                )
            }
        }

        binding.btnScanFromGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityIfNeeded(intent, SELECT_FROM_GALLERY)
        }


        binding.btnOpenUri.setOnClickListener {
            val scanResult = binding.etScanResult.text.toString()
            val scheme = Uri.parse(scanResult).scheme.toString()
            if (!scheme.startsWith("http")) {
                Snackbar.make(
                    window.decorView,
                    "这不是网页链接,继续打开可能会拉起应用程序,继续?",
                    Snackbar.LENGTH_LONG
                ).setAction("继续") {
                    try {
                        startActivity(Intent().apply {
                            action = "android.intent.action.VIEW"
                            data = Uri.parse(scanResult)
                        })
                    } catch (e: Exception) {
                        popupErrorWindow(this@MainActivity, e)
                    }
                }.show()
            } else {
                Snackbar.make(
                    window.decorView,
                    "请确认链接安全性,继续?",
                    Snackbar.LENGTH_LONG
                ).setAction("继续") {
                    try {
                        Intent("")
                        startActivity(Intent().apply {
                            action = "android.intent.action.VIEW"
                            data = Uri.parse(scanResult)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    } catch (e: Exception) {
                        popupErrorWindow(this@MainActivity, e)
                    }
                }.show()
            }
        }

        binding.btnCopy.setOnClickListener {
            if (!TextUtils.isEmpty(binding.etScanResult.text.toString())) {// 创建普通字符型ClipData
                copy(binding.etScanResult.text.toString())
            } else {
                Snackbar.make(window.decorView, "复制了个寂寞", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.etInputQrContent.doAfterTextChanged {
            if (!TextUtils.isEmpty(it.toString())) {
                binding.ivGeneratedQr.visibility = View.VISIBLE
//                var qrImg = Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
                var qrImg = BarcodeEncoder().encodeBitmap(
                    it.toString(),
                    BarcodeFormat.QR_CODE,
                    dp2px(this@MainActivity, 350),
                    dp2px(this@MainActivity, 350)
                )
                Glide.with(this@MainActivity)
                    .asBitmap()
                    .load(
                        qrImg
                    )
                    .into(binding.ivGeneratedQr)
            } else {
                binding.ivGeneratedQr.visibility = View.GONE
            }

        }

        binding.etScanResult.setOnLongClickListener {
            Snackbar.make(window.decorView, "清空?", Snackbar.LENGTH_LONG).setAction("确认") {
                binding.etScanResult.setText("")
                binding.etScanResult.clearFocus()
            }.show()
            true
        }
    }

    fun popupErrorWindow(context: Context, e: Exception) {
        val errLog = StringBuilder().apply {
            for (msg in e.stackTrace) {
                this.append("$msg \n")
            }
        }.toString()
        AlertDialog.Builder(context).apply {
            setTitle("发生错误,请将此日志反馈给开发者:")
            setView(ScrollView(context).apply {
                addView(LinearLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    addView(TextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        text = errLog
                        setOnClickListener {
                            copy(errLog)
                        }
                    })
                })
            })
            setPositiveButton("复制") { _, _ ->
                copy(errLog)
            }
        }.show()
    }

    fun copy(str: String) {
        val mClipData = ClipData.newPlainText("Label", str)
        // 将ClipData内容放到系统剪贴板里。
        clipBoardManager.setPrimaryClip(mClipData)
        Snackbar.make(window.decorView, "已复制到剪贴板!", Snackbar.LENGTH_LONG).show()
    }

    fun decodeQR(srcBitmap: Bitmap?): Result? {
        var result: Result? = null
        if (srcBitmap != null) {
            val width = srcBitmap.width
            val height = srcBitmap.height
            val pixels = IntArray(width * height)
            srcBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            // 新建一个RGBLuminanceSource对象
            val source = RGBLuminanceSource(width, height, pixels)
            // 将图片转换成二进制图片
            val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
            val reader = QRCodeReader() // 初始化解析对象
            try {
                val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
                val formats = ArrayList<BarcodeFormat>()
                formats.add(BarcodeFormat.QR_CODE)
                hints.put(DecodeHintType.POSSIBLE_FORMATS, formats)
                result = reader.decode(binaryBitmap, hints) // 开始解析
            } catch (e: NotFoundException) {
                e.printStackTrace()
            } catch (e: ChecksumException) {
                e.printStackTrace()
            } catch (e: FormatException) {
                e.printStackTrace()
            }
        }
        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            QR_REQUEST_CODE -> {
                val data = data?.extras?.getString("SCAN_RESULT")
                if (data == "null") {
                    Snackbar.make(window.decorView, "没有扫描到任何内容", Snackbar.LENGTH_LONG)
                        .show()
                } else {
                    binding.etScanResult.setText(data)
                    // 判断是不是可打开的uri
                    val authority = Uri.parse(data).authority
                    if (!TextUtils.isEmpty(authority)) {
                        if(data!!.startsWith("https://") || data!!.startsWith("http://")){
                            binding.btnOpenUri.setImageResource(R.drawable.baseline_link_24)
                        }else{
                            binding.btnOpenUri.setImageResource(R.drawable.baseline_android_24)
                        }
                        binding.btnOpenUri.visibility = View.VISIBLE
                    } else {
                        binding.btnOpenUri.visibility = View.GONE
                    }
                }
            }

            SELECT_FROM_GALLERY -> {
                var bitmap: Bitmap? = null
                data?.data?.let { uri ->
                    bitmap = contentResolver.openFileDescriptor(uri, "r")
                        ?.use { BitmapFactory.decodeFileDescriptor(it.fileDescriptor) }
                }
                bitmap?.let { scanQrFromBitmapAndSetView(it) }
            }
        }
    }

    fun scanQrFromBitmapAndSetView(bitmap: Bitmap) {
        val scanResult = decodeQR(bitmap).toString()
        if (scanResult == "null") {
            Snackbar.make(window.decorView, "没有扫描到任何内容", Snackbar.LENGTH_LONG)
                .show()
        } else {
            binding.etScanResult.setText(scanResult)

            // 判断是不是可打开的uri
            val authority = Uri.parse(scanResult).authority
            if (!TextUtils.isEmpty(authority)) {
                if(scanResult!!.startsWith("https://") || scanResult!!.startsWith("http://")){
                    binding.btnOpenUri.setImageResource(R.drawable.baseline_link_24)
                }else{
                    binding.btnOpenUri.setImageResource(R.drawable.baseline_android_24)
                }
                binding.btnOpenUri.visibility = View.VISIBLE
            } else {
                binding.btnOpenUri.visibility = View.GONE
            }
        }
    }

    fun dp2px(context: Context, dpValue: Int): Int {
        //获取屏幕分辨率
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }


    private fun grantPermission(activity: AppCompatActivity, grantedToDo: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 24) {
            // 相机权限
            PermissionX.init(activity)
                .permissions(
                    Manifest.permission.CAMERA
                )
                .explainReasonBeforeRequest()
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(
                        deniedList,
                        "扫描二维码所需权限",
                        "好",
                        "取消"
                    )
                }
                .onForwardToSettings { scope, deniedList ->
                    scope.showForwardToSettingsDialog(
                        deniedList,
                        "不授予权限则无法扫码!",
                        "好",
                        "取消"
                    )
                }
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        Snackbar.make(
                            activity.window.decorView,
                            "权限状态正常",
                            Snackbar.LENGTH_LONG
                        )
                        grantedToDo()
                    } else {
                        Snackbar.make(
                            activity.window.decorView,
                            "以下权限被拒绝: $deniedList",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}