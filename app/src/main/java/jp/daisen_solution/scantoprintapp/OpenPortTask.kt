package jp.daisen_solution.scantoprintapp

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.widget.Button
import jp.co.toshibatec.bcp.library.BCPControl
import jp.co.toshibatec.bcp.library.LongRef
import jp.co.toshibatec.bcp.library.StringRef
import java.util.concurrent.atomic.AtomicBoolean

class OpenPortTask(context: Activity?, bcpControl: BCPControl?, connectionData: ConnectionData?) {

    private var mContext: Activity? = context
    private var mBcpControl: BCPControl? = bcpControl
    private var mConnectionData: ConnectionData? = connectionData
    private var mProgressDlg: ProgressDialog? = null
    private var bluetoothDeviceExtra: String = ""

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // プリンタのBluetoothポートをオープンするメソッド
    ////////////////////////////////////////////////////////////////////////////////////////////////
    suspend fun openBluetoothPort() {

        // プログレスダイアログの表示
        mProgressDlg = ProgressDialog(mContext)
        mProgressDlg!!.setTitle(R.string.connectionProcess)
        mProgressDlg!!.setMessage(mContext!!.getString(R.string.wait))
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

        var resultDialogMessage =""
        if (!resultOpen) {
            val message = StringRef("")
            if (!mBcpControl!!.GetMessage(result.longValue, message)) {
                resultDialogMessage = mContext!!.getString(R.string.msg_OpenPorterror)
                Log.e("openPort",mContext!!.getString(R.string.msg_OpenPorterror))
            } else {
                resultDialogMessage = message.getStringValue()
                Log.e("openPort", message.getStringValue())
            }
        } else {
            resultDialogMessage = mContext!!.getString(R.string.msg_success)
            mContext!!.getSharedPreferences(Consts.bcpSectionName, Context.MODE_PRIVATE).edit()
                .putString(Consts.pairingNameKey, bluetoothDeviceExtra).apply()
            Log.i("openPort","ポートオープン処理：成功")
        }

        mProgressDlg!!.dismiss()
        mPrintButton!!.isEnabled = resultOpen
        util.showAlertDialog(mContext!!, resultDialogMessage)
        mContext = null
        mBcpControl = null
        mPrintButton = null
    }

}