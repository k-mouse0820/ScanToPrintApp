package jp.daisen_solution.scantoprintapp

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.PaintDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import jp.daisen_solution.scantoprintapp.databinding.ActivityMainBinding
import kotlinx.coroutines.selects.select
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val con = this.applicationContext
        try {
            /////////////////////////////////////////////////////////////////////////////////////
            // ペアリング済みのBluetooth機器一覧を作成
            /////////////////////////////////////////////////////////////////////////////////////
            val adapter =
                ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice)
            val bluetoothManager =
                this.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val pairedBluetoothDevices = bluetoothManager.adapter.bondedDevices
            if (pairedBluetoothDevices == null) {
                Toast.makeText(this, R.string.msg_NoPaireddevice, Toast.LENGTH_LONG).show()
                return
            }
            val pairedBluetoothDeviceName =
                this.getSharedPreferences(Consts.bcpSectionName, Context.MODE_PRIVATE)
                    .getString(Consts.pairingNameKey, "")
            var position = 0
            var selectPosition = 0
            for (device in pairedBluetoothDevices) {
                var bluetoothDeviceName: String
                bluetoothDeviceName = device.name + " (" + device.address + ")"
                adapter.add(bluetoothDeviceName)
                if (pairedBluetoothDeviceName != null && pairedBluetoothDeviceName.isNotEmpty()) {
                    if (bluetoothDeviceName.compareTo(pairedBluetoothDeviceName) == 0) {
                        selectPosition = position   // 前回接続したデバイスをデフォルト選択
                    } else {
                        val bdAddress = bluetoothDeviceName.substring(
                            bluetoothDeviceName.indexOf("(") + 1, bluetoothDeviceName.indexOf(")")
                        )
                        if (bdAddress == pairedBluetoothDeviceName) {
                            selectPosition = position
                        }
                    }
                }
                position += 1
            }
            binding.bluetoothListView.choiceMode = ListView.CHOICE_MODE_SINGLE
            binding.bluetoothListView.adapter = adapter
            binding.bluetoothListView.selector = PaintDrawable(Color.BLUE)
            binding.bluetoothListView.setItemChecked(selectPosition, true)  // デフォルト行を選択

            /*
            /////////////////////////////////////////////////////////////////////////////////////
            // リストのアイテム（BluetoothDevice）を選択した時の処理
            // １．選択したデバイスを記録
            // ２．スキャン画面へ遷移
            /////////////////////////////////////////////////////////////////////////////////////
            val listClickListener = OnItemClickListener { parent, _, position, _ ->
                val listView = parent as ListView
                val item = listView.getItemAtPosition(position) as String
                selectPosition = position
                binding.bluetoothListView.setItemChecked(selectPosition, true)
                this.getSharedPreferences(Consts.bcpSectionName, Context.MODE_PRIVATE).edit()
                    .putString(Consts.pairingNameKey, item).apply()
            }
             */

            binding.startButton.setOnClickListener {
                val item = binding.bluetoothListView.selectedItem as String
                val intent = Intent(this, ScanToPrintActivity::class.java)
                intent.putExtra(Consts.bluetoothDeviceExtra, item )
            }

        } catch (th: Throwable) {
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // 「前画面に戻る」ボタン押下時の処理
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event!!.action == KeyEvent.ACTION_DOWN) {
            if (event!!.keyCode == KeyEvent.KEYCODE_BACK) {

                val alertBuilder = AlertDialog.Builder(this)
                alertBuilder.setMessage(R.string.alert_AppExit)
                alertBuilder.setCancelable(false)
                alertBuilder.setPositiveButton(R.string.msg_Ok) { _, _ ->
                    exitProcess(RESULT_OK) }
                alertBuilder.setNegativeButton(R.string.msg_No) { _, _ ->
                    // 何もしない
                }
                alertBuilder.show()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

}


/*


        scanButton.setOnClickListener {
            IntentIntegrator(this).apply {
                // カメラ起動をするActivityを指定
                captureActivity = CustomScannerActivity::class.java
            }.initiateScan()
        }

        printButton.setOnClickListener {

            val printText = resultText.text.toString() ?: ""

            if (printText != "") {
                Log.v("MainActivity", "start print")
                val intent = Intent(applicationContext, Blp2dPrintActivity::class.java)
                intent.putExtra("printData", printText)
                startActivity(intent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 読み取った結果の受け取り
        val result = IntentIntegrator.parseActivityResult(resultCode, data)
        if (result.contents != null) {
            // 読み取った結果を表示
            // Toast.makeText(this, result.contents, Toast.LENGTH_LONG).show()
            resultText.text = result.contents
        }
    }

}
*/