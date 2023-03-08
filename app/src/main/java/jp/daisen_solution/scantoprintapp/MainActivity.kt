package jp.daisen_solution.scantoprintapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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