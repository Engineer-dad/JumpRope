package co.hisfot.appjumbrope

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity() {

    val BT_REQUEST_ENABLE = 1
    val BT_MESSAGE_READ = 2
    val BT_CONNECTING_STATUS = 3
    val BT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    var mPairedDevices: Set<BluetoothDevice> = mBluetoothAdapter.bondedDevices

    var mBtProtocol : BtProtocol = BtProtocol( "" )
  //  var mListPairedDevices: List<String> =  ArrayList()
    var mBluetoothHandler = object : Handler() {
      override fun handleMessage(msg: Message) {
          if (msg.what == BT_MESSAGE_READ) {
              var readMessage: String? = null
              try {
//                  var objCopy: ByteArray? = null
//                  objCopy = ByteArray(msg.arg1);
//                  System.arraycopy((msg.obj as ByteArray), 0,objCopy, 0, msg.arg1 )
//                  if(mBtProtocol.addByte(objCopy)){
//                      if(mBtProtocol.jumpCnt == "0")  tvReceiveData.text = "준비"
//                      else tvReceiveData.text =mBtProtocol.jumpCnt
//                  }

                  val cnt:Int = (msg.obj as ByteArray)[0].toInt()
                  if(cnt == 0)  tvReceiveData.text = "준비"
                  else tvReceiveData.text = cnt.toString();
//                  println(mBtProtocol.addByte(objCopy))
//                  println(mBtProtocol.jumpCnt)

              } catch (e: UnsupportedEncodingException) {
                  e.printStackTrace()
              }

          }
      }
  }
    var mThreadConnectedBluetooth: ConnectedBluetoothThread ? = null
    var mBluetoothDevice: BluetoothDevice? = null
    var mBluetoothSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        btnConnect.setOnClickListener {
            if(mBluetoothSocket == null){
                listPairedDevices()
            }
            else{
                tvReceiveData.text = "준비";
            }
        }
//        btnSendData.setOnClickListener {
//            if(mThreadConnectedBluetooth !=null ){
//                mThreadConnectedBluetooth?.write(tvSendData.text.toString());
//                tvSendData.text.clear();
//            }
//        }
    }


    fun bluetoothOn(){
        if(mBluetoothAdapter == null){
            Toast.makeText(applicationContext, "블루투스를 지원하지 않는 기기입니다", Toast.LENGTH_LONG).show();
        }
        else{
            if(mBluetoothAdapter.isEnabled){
                Toast.makeText(applicationContext, "블루투스가 이미 활성화 되어 있습니다", Toast.LENGTH_LONG).show();

            }
            else{
                Toast.makeText(applicationContext, "블루투스가 활성화 되어 있지 않습니다", Toast.LENGTH_LONG).show();
                var intentBluetoothEnable : Intent =  Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE)

            }
        }
    }

    fun bluetoothOff() {
        if (mBluetoothAdapter.isEnabled) {
            mBluetoothAdapter.disable()
            Toast.makeText(applicationContext, "블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(applicationContext, "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun listPairedDevices() {
        if (mBluetoothAdapter.isEnabled) {
            //mPairedDevices = mBluetoothAdapter.bondedDevices
            if ( !mPairedDevices.isEmpty()) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("장치 선택")
                var mListPairedDevices = mutableListOf<String>()
                for (device in mPairedDevices) {
                    mListPairedDevices.add(device.name)
                    //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                val items: Array<CharSequence> = mListPairedDevices.toTypedArray();
                //mListPairedDevices.toArray(arrayOfNulls<CharSequence>(mListPairedDevices.size()))
                builder.setItems(items, DialogInterface.OnClickListener { dialog, item -> connectSelectedDevice (items[item].toString()) })
                val alert: AlertDialog = builder.create()
                alert.show()
            } else {
                Toast.makeText(applicationContext, "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(applicationContext, "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun connectSelectedDevice(selectedDeviceName: String) {
        for (tempDevice in mPairedDevices) {
            if (selectedDeviceName == tempDevice.name) {
                mBluetoothDevice = tempDevice
                break
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice?.createRfcommSocketToServiceRecord(BT_UUID)
            mBluetoothSocket?.connect()
            mThreadConnectedBluetooth = mBluetoothSocket?.let { ConnectedBluetoothThread(it) }
            mThreadConnectedBluetooth?.start()
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget()
            btnConnect.text = "초기화"
            tvReceiveData.text = "준비"
        } catch (e: IOException) {
            Toast.makeText(applicationContext, "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
        }
    }


    inner class ConnectedBluetoothThread(private val socket: BluetoothSocket) : Thread() {
        private var mmSocket: BluetoothSocket? = null
        private var mmInStream: InputStream? = null
        private var mmOutStream: OutputStream? = null

        init {
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
              tmpIn = socket.getInputStream()
             tmpOut = socket.getOutputStream()
            } catch (e: IOException) {
              Toast.makeText(applicationContext, "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = mmInStream?.available() ?: 0;
                    if (bytes != 0) {
                        //SystemClock.sleep(100)
                        bytes = mmInStream?.available() ?: 0
                        bytes = mmInStream?.read(buffer, 0, bytes) ?: 0
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(str: String) {
            val bytes = str.toByteArray()
            try {
                mmOutStream?.write(bytes)
            } catch (e: IOException) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            }
        }


    }
}