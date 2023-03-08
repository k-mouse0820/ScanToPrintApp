package jp.daisen_solution.scantoprintapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import jp.co.toshibatec.bcp.library.BCPControl
import jp.co.toshibatec.bcp.library.BCPControl.LIBBcpControlCallBack
import jp.co.toshibatec.bcp.library.LongRef
import jp.co.toshibatec.bcp.library.StringRef
import kotlinx.android.synthetic.main.activity_blp2d_print.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class Blp2dPrintActivity : AppCompatActivity(), LIBBcpControlCallBack {

    private var mBcpControl: BCPControl? = null
    private var mConnectionData: ConnectionData? = ConnectionData()
    private var mPrintData: PrintData? = PrintData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blp2d_print)

        val extras = intent.extras
        printText.text = extras!!.getString("printData")

        statusText.text = getText(R.string.connectionProcess)
        messageText.text = getText(R.string.wait)
        progressBar.visibility = View.VISIBLE

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

        // Bluetoothデバイス情報取得　　　--> あとでデバイスは選択可能に修正するが、とりあえずbluetoothデバイスが1台のみとして決め打ち
        val deviceHardwareAddress = getBluetoothAddress()

        // 通信パラメータの設定
        mConnectionData!!.issueMode = Consts.AsynchronousMode   // 1:送信完了復帰  2:発行完了復帰
        mConnectionData!!.portSetting = "Bluetooth:$deviceHardwareAddress"

        // 通信ポートのオープン
        if (!mConnectionData!!.isOpen.get()) {
            CoroutineScope(Dispatchers.Main).launch {
                openPort()
            }
        } else {
            Log.v("openPort", "Already opened - skip")
        }

        // 印刷処理
        printButton.setOnClickListener {

            mPrintData = PrintData()
            val result = LongRef(0)
            mPrintData!!.currentIssueMode = mConnectionData!!.issueMode
            mPrintData!!.printCount = 1  // とりあえず、決め打ちで１枚とする

            val printItemList = HashMap<String?, String?>()

            // 品番データ（8桁）
            val hinban = printText.text.toString()
            if (hinban.length > 8) {
                printItemList[getString(R.string.hinbanData)] = hinban.substring(0,7)
            } else {
                printItemList[getString(R.string.hinbanData)] = hinban
            }

            // 品名データ（14桁）
            val hinmei = "テスト品名"
            if (hinmei.length > 14) {
                printItemList[getString(R.string.hinmeiData)] = hinmei.substring(0,15)
            } else {
                printItemList[getString(R.string.hinmeiData)] = hinmei
            }

            // 仕入先データ（14桁）
            val siiresaki = "テスト仕入先"
            if (siiresaki.length > 14) {
                printItemList[getString(R.string.siiresakiData)] = siiresaki.substring(0,15)
            } else {
                printItemList[getString(R.string.siiresakiData)] = siiresaki
            }

            // QRCODE
            val qrcode = printText.text.toString()
            printItemList[getString(R.string.qrcodeData)] = qrcode

            // 印刷データをセット
            mPrintData!!.objectDataList = printItemList

            // lfmファイルをセット
            val filePathName =
                systemPath + "/tempLabel.lfm"
            mPrintData!!.lfmFileFullPath = filePathName

            // 印刷実行スレッドの起動
            CoroutineScope(Dispatchers.Main).launch {
                printExecute()
            }

        }
    }

    private fun getBluetoothAddress(): String {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        val enableBtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        }
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices   // 権限ない場合の対応はとりあえず考慮しない
        var deviceName = ""
        var deviceHardwareAddress = ""
        pairedDevices?.forEach { device ->  //とりあえずbluetoothデバイスが1台のみとして決め打ち
            deviceName = device.name   // 権限ない場合の対応はとりあえず考慮しない
            deviceHardwareAddress = device.address // MAC address
            Log.i("detected bluetooth device ", "$deviceName : $deviceHardwareAddress" )
        }
        return deviceHardwareAddress
    }


    private suspend fun openPort() {
        progressBar.visibility = View.VISIBLE
        statusText.text = getText(R.string.connectionProcess)
        messageText.text = getText(R.string.wait)
        mBcpControl!!.portSetting = mConnectionData!!.portSetting

        val result = LongRef(0)
        Log.v("open port", "open start")
        val resultOpen = mBcpControl!!.OpenPort(mConnectionData!!.issueMode, result)
        Log.v("open port", "result : ${result.longValue}")
        mConnectionData!!.isOpen = AtomicBoolean(resultOpen)
        progressBar.visibility = View.GONE
        if (!resultOpen) {
            val message = StringRef("")
            if (!mBcpControl!!.GetMessage(result.longValue, message)) {
                statusText.text = ""
                messageText.text = getString(R.string.msg_OpenPorterror)
                Log.e("open port",getString(R.string.msg_OpenPorterror))
            } else {
                statusText.text = ""
                messageText.text = message.getStringValue()
                Log.e("open port", message.getStringValue())
            }
        } else {
            messageText.text = getString(R.string.msg_success)
            printButton.isEnabled = true
            Log.i("open port","open complete")
        }

        printButton.isEnabled=true

    }


    private suspend fun printExecute() {

        progressBar.visibility = View.VISIBLE
        statusText.text = getText(R.string.runPrint)
        messageText.text = getText(R.string.wait)
        printButton.isEnabled=false

        Log.i("issuePrint","start")
        mPrintData!!.result = 0
        mPrintData!!.statusMessage = ""


        // Load lfm file
        Log.i("", "--------loadLfmFile start")
        var result = LongRef(0)
        Log.v("",mPrintData!!.lfmFileFullPath)
        if (!mBcpControl!!.LoadLfmFile(mPrintData!!.lfmFileFullPath, result)) {
            Log.e("","--------loadLfmFile error")
            progressBar.visibility = View.GONE
            statusText.text = getString(R.string.msg_executePrintError)
            messageText.text = String.format("loadLfmFile Error = %08x %s", result.longValue, mPrintData!!.lfmFileFullPath)
            Log.v("",messageText.text.toString())
            return
        }

        // set object
        Log.i("","--------setObjectDataEX start")
        // 全てのキー値を取得
        val keySet: Set<*> = mPrintData!!.objectDataList!!.keys
        val keyIte = keySet.iterator()
        // ループ。反復子iteratorによるキー取得
        while (keyIte.hasNext()) {
            val key = keyIte.next() as String
            if (!mBcpControl!!.SetObjectDataEx(key, mPrintData!!.objectDataList!![key], result)) {
                Log.e("","--------setObjectDataEX error")
                progressBar.visibility = View.GONE
                statusText.text = getString(R.string.msg_executePrintError)
                messageText.text = String.format("setObjectDataEX Error = %08x %s", result.longValue)
                return
            }
        }

        // print
        val printerStatus = StringRef("")
        val cutInterval = 10 // 10msec
        var printResult = ""
        Log.i("","--------issue start")
        if (!mBcpControl!!.Issue(mPrintData!!.printCount, cutInterval, printerStatus, result)) {
            mPrintData!!.result = result.longValue
            val message = StringRef("")
            if (result.longValue == 0x800A044EL) {     // プリンタからステータスを受信
                val errCode = printerStatus.getStringValue().substring(0,2)
                mBcpControl!!.GetMessage(errCode, message)
            } else {
                if (!mBcpControl!!.GetMessage(result.longValue, message)) {
                    message.setStringValue(String.format("executePrint Error = %08x %s", result.longValue))
                }
            }
            mPrintData!!.statusMessage = message.getStringValue()

            // リトライ可能エラー有無の確認
            if (mBcpControl!!.IsIssueRetryError()) {
                printResult = getString(R.string.msg_RetryError)
            } else {
                printResult ="Error"
            }
        } else {
            mPrintData!!.result = result.longValue
            mPrintData!!.statusMessage = getString(R.string.msg_success)
            printResult = getString(R.string.msg_success)
        }

        progressBar.visibility = View.GONE
        statusText.text = getString(R.string.endPrint)
        messageText.text = printResult


        when (printResult) {
            getString(R.string.msg_success) -> {
                this.showDialog(2)
            }
            getString(R.string.msg_RetryError) -> {
                this.showDialog(1)
            }
            else -> {
                this.showDialog(3)
            }
        }
        printButton.isEnabled=true
        return
    }

    override fun BcpControl_OnStatus(PrinterStatus: String?, Result: Long) {

        var strMessage = ""
        val bcp = BCPControl(null)
        val message = StringRef("")
        strMessage = if (! mBcpControl!!.GetMessage(Result, message)) {
            String.format(
                getString(R.string.statusReception) + " %s : %s ",
                PrinterStatus,
                "failed to error message"
            )
        } else {
            String.format(
                getString(R.string.statusReception) + " %s : %s ",
                PrinterStatus,
                message.getStringValue()
            )
        }

        Log.i("Blp2dPrintActivity",strMessage)
        /*
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setMessage(strMessage)
        alertBuilder.setCancelable(true)
        alertBuilder.setPositiveButton("YES",null)
        val alertDialog = alertBuilder.create()
        alertDialog.show()q
        */
    }

}