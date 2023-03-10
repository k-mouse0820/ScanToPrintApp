package jp.daisen_solution.scantoprintapp

import android.animation.ValueAnimator
import android.animation.ArgbEvaluator
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.android.BeepManager
import com.google.zxing.client.android.Intents.Scan
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import jp.co.toshibatec.bcp.library.BCPControl
import jp.co.toshibatec.bcp.library.BCPControl.LIBBcpControlCallBack
import jp.co.toshibatec.bcp.library.LongRef
import jp.co.toshibatec.bcp.library.StringRef
import jp.daisen_solution.scantoprintapp.databinding.ActivityScanToPrintBinding
import jp.daisen_solution.scantoprintapp.databinding.CustomQrCodeScannerBinding


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

class ScanToPrintActivity : AppCompatActivity(), LIBBcpControlCallBack {

    private lateinit var binding: ActivityScanToPrintBinding
    private lateinit var bindingScanner: CustomQrCodeScannerBinding

    private lateinit var context: Context
    private lateinit var mActivity: Activity

    private lateinit var messageTextBG: Drawable
    lateinit var beepManager: BeepManager
    private var lastText = ""

    private var mBcpControl: BCPControl? = null
    private var mConnectionData: ConnectionData? = ConnectionData()
    private var mPrintData: PrintData? = PrintData()
    private var mProgressDlg: ProgressDialog? = null
    private var mPrintDialogDelegate: PrintDialogDelegate? = null

    private var bluetoothDeviceExtra: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanToPrintBinding.inflate(layoutInflater)
        bindingScanner = CustomQrCodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this.applicationContext
        mActivity = this
        binding.progressText.text = ""
        binding.messageText.text = ""
        binding.scanText.text = ""
        binding.printButton.text = ""

        /////////////////////////////////////////////////////////////////////////////////////
        // １．スキャナー起動
        /////////////////////////////////////////////////////////////////////////////////////

        // QRコードスキャン時の処理（コールバック）
        var callback = BarcodeCallback { result ->
            result?.let {
                if (result.text != null && !result.text.equals(lastText)) {
                    // 重複スキャンはしない
                    lastText = it.text
                    binding.scanText.text = it.text
                    binding.printButton.isEnabled = true
                    binding.printButton.text = getString(R.string.printButton)

                    beepManager.playBeepSoundAndVibrate()
                    animateBackground()
                }
            }
        }

        binding.barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory()
        binding.barcodeView.initializeFromIntent(intent)
        binding.barcodeView.decodeContinuous(callback)

        beepManager = BeepManager(this)
        beepManager.isVibrateEnabled = true

        messageTextBG = binding.messageText.background

        // フラッシュのON/OFFをするListenerの設定
        bindingScanner.flashSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.barcodeView.setTorchOn()
            } else {
                binding.barcodeView.setTorchOff()
            }
        }

        /////////////////////////////////////////////////////////////////////////////////////
        // ２．プリンタ初期設定
        /////////////////////////////////////////////////////////////////////////////////////
        mBcpControl = BCPControl(this)
        mPrintDialogDelegate = PrintDialogDelegate(this, mBcpControl!!, mPrintData)

        // systemPathを設定
        val systemPath = Environment.getDataDirectory().path + "/data/" + this.packageName
        Log.i("set systemPath", systemPath)
        mBcpControl!!.systemPath = systemPath

        // プリンタ設定ファイル、ラベルフォーマットファイルのセット
        val newfile = File(systemPath)
        if (!newfile.exists()) {
            if (newfile.mkdirs()) {}
        }
        try {
            util.asset2file(applicationContext, "PrtList.ini", systemPath, "PrtList.ini")
            util.asset2file(applicationContext, "PRTEP2G.ini", systemPath, "PRTEP2G.ini")
            util.asset2file(applicationContext, "PRTEP4T.ini", systemPath, "PRTEP4T.ini")
            util.asset2file(applicationContext, "PRTEP2GQM.ini", systemPath, "PRTEP2GQM.ini")
            util.asset2file(applicationContext, "PRTEP4GQM.ini", systemPath, "PRTEP4GQM.ini")
            util.asset2file(applicationContext, "PRTEV4TT.ini", systemPath, "PRTEV4TT.ini")
            util.asset2file(applicationContext, "PRTEV4TG.ini", systemPath, "PRTEV4TG.ini")
            util.asset2file(applicationContext, "PRTLV4TT.ini", systemPath, "PRTLV4TT.ini")
            util.asset2file(applicationContext, "PRTLV4TG.ini", systemPath, "PRTLV4TG.ini")
            util.asset2file(applicationContext, "PRTFP2DG.ini", systemPath, "PRTFP2DG.ini")
            util.asset2file(applicationContext, "PRTFP3DG.ini", systemPath, "PRTFP3DG.ini")
            util.asset2file(applicationContext, "PRTBA400TG.ini", systemPath, "PRTBA400TG.ini")
            util.asset2file(applicationContext, "PRTBA400TT.ini", systemPath, "PRTBA400TT.ini")
            util.asset2file(applicationContext, "PRTBV400G.ini", systemPath, "PRTBV400G.ini")
            util.asset2file(applicationContext, "PRTBV400T.ini", systemPath, "PRTBV400T.ini")

            util.asset2file(applicationContext, "ErrMsg0.ini", systemPath, "ErrMsg0.ini")
            util.asset2file(applicationContext, "ErrMsg1.ini", systemPath, "ErrMsg1.ini")
            util.asset2file(applicationContext, "resource.xml", systemPath, "resource.xml")

            util.asset2file(applicationContext, "EP2G_scanToPrint.lfm", systemPath, "tempLabel.lfm")
        } catch (e: Exception) {
            util.showAlertDialog(this,
                "Failed to copy ini and lfm files.")
            e.printStackTrace()
            return
        }

        // 使用するプリンタの設定   B-LP2Dは「27」
        mBcpControl!!.usePrinter = 27

        // Bluetoothデバイス情報取得(MainActivityで選択したデバイス）
        bluetoothDeviceExtra = intent.getStringExtra(Consts.bluetoothDeviceExtra).toString()
        val bdAddress = bluetoothDeviceExtra!!.substring(
            bluetoothDeviceExtra!!.indexOf("(") + 1,
            bluetoothDeviceExtra!!.indexOf(")")
        )

        if (bdAddress == null || bdAddress.isEmpty()) {
            util.showAlertDialog(this, this.getString(R.string.bdAddrNotSet) )
            confirmationEndDialog(this)
        }

        // 通信パラメータの設定
        mConnectionData!!.issueMode = Consts.AsynchronousMode   // 1:送信完了復帰  2:発行完了復帰
        mConnectionData!!.portSetting = "Bluetooth:$bdAddress"

        /////////////////////////////////////////////////////////////////////////////////////
        // ３．通信ポートのオープン　（非同期処理）
        /////////////////////////////////////////////////////////////////////////////////////
        var mOpenPortTask = OpenPortTask(this,mBcpControl, mConnectionData)

        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.text = getString(R.string.msg_connectingPrinter)

        if (!mConnectionData!!.isOpen.get()) {
            CoroutineScope(Dispatchers.Main).launch {
                var resultMessage = mOpenPortTask.openBluetoothPort()
                binding.progressBar.visibility = View.INVISIBLE
                if (resultMessage.equals(getString(R.string.msg_success))) {
                    binding.progressText.text = ""
                    binding.messageText.text = getString(R.string.msg_readQR)
                } else {
                    util.showAlertDialog(context, resultMessage)
                }
            }
        } else {
            Log.v("openPort", "Already opened - skip")
        }

        /////////////////////////////////////////////////////////////////////////////////////
        // ４．印刷ボタン押下時の処理
        /////////////////////////////////////////////////////////////////////////////////////
        binding.printButton.setOnClickListener {

            mPrintData = PrintData()
            val result = LongRef(0)
            mPrintData!!.currentIssueMode = mConnectionData!!.issueMode
            mPrintData!!.printCount = 1  // 決め打ちで１枚とする

            val printItemList = HashMap<String?, String?>()

            // 品番データ（8桁）
            val hinban = binding.scanText.text.toString()
            if (hinban.length > 8) {
                printItemList[getString(R.string.hinbanData)] = hinban.substring(0,7)
            } else {
                printItemList[getString(R.string.hinbanData)] = hinban
            }

            // 品名データ（14桁）
            val hinmei = "テスト品名"
            if (hinmei.length > 14) {
                printItemList[getString(R.string.hinmeiData)] = hinmei.substring(0,13)
            } else {
                printItemList[getString(R.string.hinmeiData)] = hinmei
            }

            // 仕入先データ（14桁）
            val siiresaki = "テスト仕入先"
            if (siiresaki.length > 14) {
                printItemList[getString(R.string.siiresakiData)] = siiresaki.substring(0,13)
            } else {
                printItemList[getString(R.string.siiresakiData)] = siiresaki
            }

            // QRCODE
            val qrcode = binding.messageText.text.toString()
            printItemList[getString(R.string.qrcodeData)] = qrcode

            // 印刷データをセット
            mPrintData!!.objectDataList = printItemList

            // lfmファイルをセット
            val filePathName =
                systemPath + "/tempLabel.lfm"
            mPrintData!!.lfmFileFullPath = filePathName

            // 印刷実行スレッドの起動
            var mPrintExecuteTask = PrintExecuteTask(this,mBcpControl, mPrintData)
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.text = getString(R.string.msg_executingPrint)

            CoroutineScope(Dispatchers.Main).launch {
                var resultMessage = mPrintExecuteTask.print()
                binding.progressBar.visibility = View.INVISIBLE
                binding.progressText.text = ""
                when (resultMessage) {
                    getString(R.string.msg_success) -> {
                        mActivity.showDialog(PrintDialogDelegate.Companion.PRINT_COMPLETEMESSAGE_DIALOG)
                        binding.scanText.text = ""
                    }
                    getString(R.string.msg_RetryError) -> {
                        mActivity.showDialog(PrintDialogDelegate.Companion.RETRYERRORMESSAGE_DIALOG)
                    }
                    else -> {
                        mActivity.showDialog(PrintDialogDelegate.Companion.ERRORMESSAGE_DIALOG)
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // プリンタのBluetoothポートをクローズするメソッド
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun closeBluetoothPort() {
        if (mConnectionData!!.isOpen == AtomicBoolean(true)) {
            val Result = LongRef(0)
            if (! mBcpControl!!.ClosePort(Result)) {
                val Message = StringRef("")
                if (! mBcpControl!!.GetMessage(Result.longValue, Message)) {
                    util.showAlertDialog(
                        this,
                        String.format(R.string.msg_PortCloseErrorcode.toString() + "= %08x", Result.longValue)
                    )
                } else {
                    util.showAlertDialog(this, Message.getStringValue())
                }
            } else {
                util.showAlertDialog(this, this.getString(R.string.msg_PortCloseSuccess))
                mConnectionData!!.isOpen = AtomicBoolean(false)
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 「前画面に戻る」ボタン押下時の処理
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event!!.action == KeyEvent.ACTION_DOWN) {
            if (event!!.keyCode == KeyEvent.KEYCODE_BACK) {
                confirmationEndDialog(this)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    private fun confirmationEndDialog(activity: Activity) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setMessage(R.string.confirmBack)
        alertBuilder.setCancelable(false)
        alertBuilder.setPositiveButton(R.string.msg_Ok) { _, _ ->
            closeBluetoothPort()
            mBcpControl = null
            mConnectionData = null
            mPrintData = null
            mPrintDialogDelegate = null
            finish()
        }
        alertBuilder.setNegativeButton(R.string.msg_No) { _, _ ->
            // 何もしない
        }
        val alertDialog = alertBuilder.create()
        alertDialog.show()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // プリンターからのメッセージ受信
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun BcpControl_OnStatus(PrinterStatus: String?, Result: Long) {
        var strMessage = ""
        val message = StringRef("")
        strMessage = if (! mBcpControl!!.GetMessage(Result, message)) {
                            String.format(getString(R.string.statusReception) + " %s : %s ", PrinterStatus, "failed to error message")
                     } else {
                            String.format(getString(R.string.statusReception) + " %s : %s ", PrinterStatus, message.getStringValue())
                     }
        Log.i("onStatus", strMessage)
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Dialog作成処理
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onPrepareDialog(id: Int, dialog: Dialog) {
        if (false == mPrintDialogDelegate!!.prepareDialog(id, dialog)) {
            super.onPrepareDialog(id, dialog)
        }
    }
    override fun onCreateDialog(id: Int): Dialog {
        var dialog: Dialog? = null
        dialog = mPrintDialogDelegate!!.createDialog(id)
        if (null == dialog) {
            dialog = super.onCreateDialog(id)
        }
        return dialog!!
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 背景点滅処理
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun animateBackground() {
        val colorFrom = resources.getColor(R.color.purple_200, theme)
        val colorTo = resources.getColor(com.google.zxing.client.android.R.color.zxing_transparent, theme)
        val colorAnimation =
            ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 250 // milliseconds

        colorAnimation.addUpdateListener { animator -> binding.messageText.setBackgroundColor(animator.animatedValue as Int)}
        colorAnimation.start()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 以下onResume, onPause は
    // アクティビティのライフサイクルとカメラのライフサイクルを合わせるために実装
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        // カメラをポーズする前にフラッシュをオフに
        bindingScanner.flashSwitch.isChecked = false
        binding.barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }




}