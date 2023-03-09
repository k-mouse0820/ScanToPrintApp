package jp.daisen_solution.scantoprintapp

import android.app.ProgressDialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import com.google.zxing.client.android.Intents.Scan
import com.journeyapps.barcodescanner.CaptureManager
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
import java.util.concurrent.atomic.AtomicBoolean

class ScanToPrintActivity : AppCompatActivity(), LIBBcpControlCallBack {

    private lateinit var binding: ActivityScanToPrintBinding
    private lateinit var bindingScanner: CustomQrCodeScannerBinding
    private lateinit var capture: CaptureManager
    private var mBcpControl: BCPControl? = null
    private var mConnectionData: ConnectionData? = ConnectionData()
    private var mPrintData: PrintData? = PrintData()
    private var mProgressDlg: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanToPrintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /////////////////////////////////////////////////////////////////////////////////////
        // １．スキャナー起動
        /////////////////////////////////////////////////////////////////////////////////////
        // CaptureManager使用してカメラ画面の起動を行う
        capture = CaptureManager(this, binding.barcodeView).apply {
            initializeFromIntent(intent, savedInstanceState)
        }
        capture.decode()

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
        val pairedBluetoothDeviceName = this.getSharedPreferences(Consts.bcpSectionName, Context.MODE_PRIVATE).getString(Consts.pairingNameKey, "")
        val bdAddress = pairedBluetoothDeviceName.substring(
            pairedBluetoothDeviceName.indexOf("(") + 1,
            pairedBluetoothDeviceName.indexOf(")")
        )
        if (bdAddress == null || bdAddress.isEmpty()) {
            util.showAlertDialog(this, this.getString(R.string.bdAddrNotSet) )
            return
        }

        // 通信パラメータの設定
        mConnectionData!!.issueMode = Consts.AsynchronousMode   // 1:送信完了復帰  2:発行完了復帰
        mConnectionData!!.portSetting = "Bluetooth:$bdAddress"

        // 通信ポートのオープン
        if (!mConnectionData!!.isOpen.get()) {
            CoroutineScope(Dispatchers.Main).launch {
                openPort()
            }
        } else {
            Log.v("openPort", "Already opened - skip")
        }


    }

    private suspend fun openPort() {

        // プログレスダイアログの表示
        mProgressDlg = ProgressDialog(this)
        mProgressDlg!!.setTitle(R.string.connectionProcess)
        mProgressDlg!!.setMessage(this.getString(R.string.wait))
        mProgressDlg!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        mProgressDlg!!.setCancelable(true)
        mProgressDlg!!.show()

        // プリンタのBluetoothポートをオープン
        mBcpControl!!.portSetting = mConnectionData!!.portSetting
        val result = LongRef(0)
        Log.v("openPort", "ポートオープン処理：開始")
        val resultOpen = mBcpControl!!.OpenPort(mConnectionData!!.issueMode, result)
        Log.v("openPort", "result : ${result.longValue}")
        mConnectionData!!.isOpen = AtomicBoolean(resultOpen)

        if (!resultOpen) {
            val message = StringRef("")
            if (!mBcpControl!!.GetMessage(result.longValue, message)) {
                mProgressDlg!!.setMessage(this.getString(R.string.msg_OpenPorterror))
                Log.e("openPort",getString(R.string.msg_OpenPorterror))
                bindingScanner.printButton.isEnabled = false
                return
            } else {
                mProgressDlg!!.setMessage(message.getStringValue())
                Log.e("openPort", message.getStringValue())
                bindingScanner.printButton.isEnabled = false
                return
            }
        } else {
            mProgressDlg!!.setMessage(this.getString(R.string.msg_success))
            bindingScanner.printButton.isEnabled = true
            Log.i("openPort","ポートオープン処理：成功")
        }

        // プログレスダイアログを消す
        mProgressDlg!!.dismiss()
    }


    // 以下onResume, onPause, onDestroyの３つは
    // アクティビティのライフサイクルとカメラのライフサイクルを合わせるために実装
    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        // カメラをポーズする前にフラッシュをオフに
        bindingScanner.flashSwitch.isChecked = false
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

}