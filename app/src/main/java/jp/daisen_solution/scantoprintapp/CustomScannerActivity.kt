package jp.daisen_solution.scantoprintapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.journeyapps.barcodescanner.CaptureManager
import kotlinx.android.synthetic.main.activity_custom_scanner.*
import kotlinx.android.synthetic.main.custom_qr_code_scanner.*

class CustomScannerActivity : AppCompatActivity() {

    private lateinit var capture: CaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_scanner)

        // CaptureManager使用してカメラ画面の起動を行う
        capture = CaptureManager(this, barcodeView).apply {
            initializeFromIntent(intent, savedInstanceState)
        }
        capture.decode()

        // フラッシュのON/OFFをするListenerの設定
        flashSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                barcodeView.setTorchOn()
            } else {
                barcodeView.setTorchOff()
            }
        }
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
        flashSwitch.isChecked = false
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }
}