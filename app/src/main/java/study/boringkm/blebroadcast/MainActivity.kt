package study.boringkm.blebroadcast

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import study.boringkm.blebroadcast.ui.theme.BleBroadcastTheme


class MainActivity : ComponentActivity() {

    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val PERMISSION_CODE = 1001
        private const val SCAN_PERIOD = 10000L
    }

    private var mScanning: Boolean = false

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val name = result?.device?.name ?: ""
            val address = result?.device?.address ?: ""

            Log.i("ble_scan", "name: $name, address: $address")
        }

        override fun onBatchScanResults(results: List<ScanResult?>?) {
            super.onBatchScanResults(results)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }
            ?.also {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
                finish()
            }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.i("ble result", it.resultCode.toString())
                val intent = it.data
                Log.i("ble data", intent.toString())
            }

        val scanHandler = Handler(Looper.getMainLooper())

        setContent {
            BleBroadcastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = {
                            // Ensures Bluetooth is available on the device and it is enabled. If not,
                            // displays a dialog requesting user permission to enable Bluetooth.
                            bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
                                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                if (ActivityCompat.checkSelfPermission(
                                        applicationContext,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        ActivityCompat.requestPermissions(
                                            this@MainActivity,
                                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                            PERMISSION_CODE
                                        )
                                    }
                                    Toast.makeText(
                                        applicationContext,
                                        "?????? ????????? ??????????????????",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@apply
                                }
                                activityResultLauncher.launch(enableBtIntent)
                            }
                        }) {
                            Text("???????????? ?????????")
                        }

                        Button(onClick = {
                            scanLeDevice(true, scanHandler)
                        }) {
                            Text("????????????")
                        }

                        Button(onClick = {
                            scanLeDevice(false, scanHandler)
                        }) {
                            Text("?????? ?????????")
                        }
                    }
                }
            }
        }
    }

    private fun scanLeDevice(enable: Boolean, handler: Handler) {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    1002
                )
            }
            return
        }

        val bluetoothLeScanner: BluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                mScanning = false
                Log.i("ble_scan", "stop")
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            mScanning = true
            Log.i("ble_scan", "start")
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            mScanning = false
            Log.i("ble_scan", "stop")
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BleBroadcastTheme {
        Greeting("Android")
    }
}