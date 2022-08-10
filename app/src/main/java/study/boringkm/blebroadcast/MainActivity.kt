package study.boringkm.blebroadcast

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
        private const val REQUEST_CODE = 999
        private const val PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }
            ?.also {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
                finish()
            }

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.i("ble result", it.resultCode.toString())
            val intent = it.data
            Log.i("ble data", intent.toString())
        }

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
                                        ActivityCompat.requestPermissions(this@MainActivity,
                                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_CODE)
                                    }
                                    Toast.makeText(applicationContext, "다시 활성화 요청해주세요", Toast.LENGTH_SHORT).show()
                                    return@apply
                                }
                                activityResultLauncher.launch(enableBtIntent)
                            }
                        }) {
                            Text("블루투스 활성화")
                        }

                        Button(onClick = {
                            startActivity(Intent(this@MainActivity, DeviceScanActivity::class.java))
                        }) {
                            Text("스캔하기")
                        }
                    }
                }
            }
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