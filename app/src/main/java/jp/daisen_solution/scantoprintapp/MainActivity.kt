package jp.daisen_solution.scantoprintapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.PaintDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import jp.daisen_solution.scantoprintapp.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val con = this.applicationContext
        try {
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


            // リストのアイテム（BluetoothDevice）を選択した時の処理
            // １．選択したデバイスを記録
            // ２．スキャン画面へ遷移
            val clickListener = OnItemClickListener { parent, _, position, _ ->
                val listView = parent as ListView
                val item = listView.getItemAtPosition(position) as String

                this.getSharedPreferences(Consts.bcpSectionName, Context.MODE_PRIVATE).edit()
                    .putString(Consts.pairingNameKey, item).apply()

                IntentIntegrator(this).apply {
                    // カメラ起動をするActivityを指定
                    captureActivity = ScanToPrintActivity::class.java
                }.initiateScan()


            }


        }
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