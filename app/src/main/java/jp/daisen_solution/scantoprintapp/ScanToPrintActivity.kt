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
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
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
        // ???????????????????????????
        /////////////////////////////////////////////////////////////////////////////////////

        // QR?????????????????????????????????????????????????????????
        var callback = BarcodeCallback { result ->
            result?.let {
                if (result.text != null && !result.text.equals(lastText)) {
                    // ??????????????????????????????
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

        // ??????????????????ON/OFF?????????Listener?????????
        bindingScanner.flashSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.barcodeView.setTorchOn()
            } else {
                binding.barcodeView.setTorchOff()
            }
        }

        /////////////////////////////////////////////////////////////////////////////////////
        // ??????????????????????????????
        /////////////////////////////////////////////////////////////////////////////////////
        mBcpControl = BCPControl(this)
        mPrintDialogDelegate = PrintDialogDelegate(this, mBcpControl!!, mPrintData)

        // systemPath?????????
        val systemPath = Environment.getDataDirectory().path + "/data/" + this.packageName
        Log.i("set systemPath", systemPath)
        mBcpControl!!.systemPath = systemPath

        // ????????????????????????????????????????????????????????????????????????????????????
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

        // ?????????????????????????????????   B-LP2D??????27???
        mBcpControl!!.usePrinter = 27

        // Bluetooth????????????????????????(MainActivity??????????????????????????????
        bluetoothDeviceExtra = intent.getStringExtra(Consts.bluetoothDeviceExtra).toString()
        val bdAddress = bluetoothDeviceExtra!!.substring(
            bluetoothDeviceExtra!!.indexOf("(") + 1,
            bluetoothDeviceExtra!!.indexOf(")")
        )

        if (bdAddress == null || bdAddress.isEmpty()) {
            util.showAlertDialog(this, this.getString(R.string.bdAddrNotSet) )
            confirmationEndDialog(this)
        }

        // ??????????????????????????????
        mConnectionData!!.issueMode = Consts.AsynchronousMode   // 1:??????????????????  2:??????????????????
        mConnectionData!!.portSetting = "Bluetooth:$bdAddress"

        /////////////////////////////////////////////////////////////////////////////////////
        // ????????????????????????????????????????????????????????????
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
                    Log.i("openPort","isOpen = " + mConnectionData!!.isOpen.toString())
                } else {
                    util.showAlertDialog(context, resultMessage)
                }
            }
        } else {
            Log.v("openPort", "Already opened - skip")
        }

        /////////////////////////////////////////////////////////////////////////////////////
        // ???????????????????????????????????????
        /////////////////////////////////////////////////////////////////////////////////////
        binding.printButton.setOnClickListener {

            mPrintData = PrintData()
            val result = LongRef(0)
            mPrintData!!.currentIssueMode = mConnectionData!!.issueMode
            mPrintData!!.printCount = 1  // ??????????????????????????????

            val printItemList = HashMap<String?, String?>()

            // ??????????????????8??????
            val hinban = binding.scanText.text.toString()
            if (hinban.length > 8) {
                printItemList[getString(R.string.hinbanData)] = hinban.substring(0,7)
            } else {
                printItemList[getString(R.string.hinbanData)] = hinban
            }

            // ??????????????????14??????
            val hinmei = "???????????????"
            if (hinmei.length > 14) {
                printItemList[getString(R.string.hinmeiData)] = hinmei.substring(0,13)
            } else {
                printItemList[getString(R.string.hinmeiData)] = hinmei
            }

            // ?????????????????????14??????
            val siiresaki = "??????????????????"
            if (siiresaki.length > 14) {
                printItemList[getString(R.string.siiresakiData)] = siiresaki.substring(0,13)
            } else {
                printItemList[getString(R.string.siiresakiData)] = siiresaki
            }

            // QRCODE
            val qrcode = binding.messageText.text.toString()
            printItemList[getString(R.string.qrcodeData)] = qrcode

            // ???????????????????????????
            mPrintData!!.objectDataList = printItemList

            // lfm????????????????????????
            val filePathName =
                systemPath + "/tempLabel.lfm"
            mPrintData!!.lfmFileFullPath = filePathName

            // ?????????????????????????????????
            var mPrintExecuteTask = PrintExecuteTask(this,mBcpControl, mPrintData)
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.text = getString(R.string.msg_executingPrint)

            CoroutineScope(Dispatchers.Main).launch {
                var resultMessage = mPrintExecuteTask.print()
                binding.progressBar.visibility = View.INVISIBLE
                binding.progressText.text = ""
                when (resultMessage) {
                    getString(R.string.msg_success) -> {
                        // mActivity.showDialog(PrintDialogDelegate.Companion.PRINT_COMPLETEMESSAGE_DIALOG)
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
    // ???????????????Bluetooth??????????????????????????????????????????
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun closeBluetoothPort() {
        Log.i("closePort","close port start")
        if (mConnectionData!!.isOpen.get()) {
            Log.i("closePort","close port start2")
            val Result = LongRef(0)
            if (! mBcpControl!!.ClosePort(Result)) {
                val Message = StringRef("")
                if (! mBcpControl!!.GetMessage(Result.longValue, Message)) {
                    Log.e("closePort",String.format(R.string.msg_PortCloseErrorcode.toString() + "= %08x", Result.longValue))
                    util.showAlertDialog(
                        this,
                        String.format(R.string.msg_PortCloseErrorcode.toString() + "= %08x", Result.longValue)
                    )
                } else {
                    Log.e("closePort",Message.getStringValue())
                    util.showAlertDialog(this, Message.getStringValue())
                }
            } else {
                Log.i("closePort",this.getString(R.string.msg_PortCloseSuccess))
                //util.showAlertDialog(this, this.getString(R.string.msg_PortCloseSuccess))
                mConnectionData!!.isOpen = AtomicBoolean(false)
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ???????????????????????????????????????????????????
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
            this.closeBluetoothPort()
            mBcpControl = null
            mConnectionData = null
            mPrintData = null
            mPrintDialogDelegate = null
            finish()
        }
        alertBuilder.setNegativeButton(R.string.msg_No) { _, _ ->
            // ???????????????
        }
        val alertDialog = alertBuilder.create()
        alertDialog.show()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ?????????????????????????????????????????????
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
    // Dialog????????????
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
    // ??????????????????
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
    // ??????onResume, onPause ???
    // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        // ????????????????????????????????????????????????????????????
        bindingScanner.flashSwitch.isChecked = false
        binding.barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }




}