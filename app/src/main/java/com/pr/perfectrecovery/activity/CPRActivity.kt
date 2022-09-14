package com.pr.perfectrecovery.activity

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.clj.fastble.BleManager
import com.clj.fastble.callback.*
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import com.clj.fastble.utils.HexUtil
import com.hoho.android.usbserial.driver.*
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.pr.perfectrecovery.BaseApplication
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.adapter.DeviceBluetoothAdapter
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.ConfigBean
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.comm.ObserverManager
import com.pr.perfectrecovery.databinding.ActivityCpractivityBinding
import com.pr.perfectrecovery.databinding.ItemBluetoothBinding
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.ConvertUtil
import com.pr.perfectrecovery.utils.DataVolatile01
import com.pr.perfectrecovery.utils.FileUtils
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.util.*

/**
 * CPR页面  蓝夜列表扫描链接
 */
class CPRActivity : BaseActivity(), SerialInputOutputManager.Listener {
    private lateinit var viewBinding: ActivityCpractivityBinding
    private var isRefresh = false
    private val mDeviceAdapter = DeviceBluetoothAdapter()
    private var bleList = mutableListOf<BleDevice>()
    private var connectList = arrayListOf<BleDevice>()
    private var isInitValueMap = mutableMapOf<String, Boolean>()

    companion object {
        fun getCustomProber(): UsbSerialProber {
            val customTable = ProbeTable()
            customTable.addProduct(
                0x16d0,
                0x087e,
                CdcAcmSerialDriver::class.java
            ) // e.g. Digispark CDC
            return UsbSerialProber(customTable)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCpractivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        EventBus.getDefault().register(this)
        initView()
        //初始化蓝牙管理器
        initBluetooth()
        initBle()
        mainLooper = Handler(Looper.getMainLooper())
        viewBinding.cbBle.isChecked = true
        registerReceiver(
            broadcastReceiver,
            IntentFilter(INTENT_ACTION_GRANT_USB)
        )
    }


    private fun initView() {
        val jsonString = MMKV.defaultMMKV().decodeString(BaseConstant.MMKV_WM_CONFIGURATION)
        val configBean = GsonUtils.fromJson(jsonString, ConfigBean::class.java)
        DataVolatile01.PR_HIGH_VALUE = configBean.prHigh()
        DataVolatile01.PR_LOW_VALUE = configBean.prLow()
        DataVolatile01.QY_HIGH_VALUE = configBean.tidalVolumeEnd
        DataVolatile01.QY_LOW_VALUE = configBean.tidalVolume
        DataVolatile01.PR_DEFAULT_TIMES = configBean.prCount
        DataVolatile01.QY_DEFAULT_TIMES = configBean.qyCount
        //searchBle()
        //查看是否有蓝牙权限
        checkPermissions()
        viewBinding.bottom.ivBack.setOnClickListener { finish() }
        viewBinding.bottom.tvContinue.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }

        viewBinding.recyclerview.layoutManager = LinearLayoutManager(this)
        // 是否抵用滚动事件监听
        //layoutManager.setChangeSelectInScrolling(false)
        viewBinding.recyclerview.adapter = bluetoothAdapter

        viewBinding.bottom.ivStart.setOnClickListener {
            val intent = Intent(this, TrainingSingleActivity::class.java)
            connectList.clear()
            connectList.addAll(bleList)
            connectList.distinctBy { listOf(it.mac, it.mac) }
            intent.putParcelableArrayListExtra(BaseConstant.CONNECT_BLE_DEVICES, connectList)
            startActivity(intent)
        }

        viewBinding.progressCircular.setOnClickListener {
//            BaseApplication.driver?.ResumeUsbList()
            searchBle()
        }
        viewBinding.recyclerview.adapter = mDeviceAdapter
        mDeviceAdapter.setOnItemClickListener(itemClick)

        viewBinding.cbUsb.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                mDeviceAdapter.setList(null)
                unBindBluetooth()
                viewBinding.cbBle.isChecked = !isChecked
                viewBinding.cbUsb.isChecked = isChecked
                disconnectUsb()
                refresh()
            }
        }

        viewBinding.cbBle.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                disconnectUsb()
                mDeviceAdapter.setList(null)
                BaseApplication.driver?.CloseDevice()
                searchBle()
                viewBinding.cbUsb.isChecked = !isChecked
                viewBinding.cbBle.isChecked = isChecked
            }
        }
    }

    /**
     * 读取蓝牙写入
     */
    private fun bleRead(bleDevice: BleDevice) {
        val gatt = BleManager.getInstance().getBluetoothGatt(mBleDevice)
        val services = gatt.services
        val bluetoothGattService = services[0]
        val characteristic = bluetoothGattService.characteristics[0]
        BleManager.getInstance().read(
            bleDevice,
            characteristic?.service?.uuid.toString(),
            characteristic?.uuid.toString(),
            object : BleReadCallback() {
                override fun onReadSuccess(data: ByteArray) {
                    val formatHexString = HexUtil.formatHexString(data)
                    Log.e("bleConnect", "onReadSuccess: $formatHexString")
                    if (!TextUtils.isEmpty(formatHexString)
                        && formatHexString.substring(formatHexString.length - 2) == "01"
                    ) {
                        //连接成功
                        Log.e("bleConnect", "onReadSuccess: 成功")
                    } else {
                        //连接失败
                        Log.e("bleConnect", "onReadSuccess: 失败")
                    }
                    //runOnUiThread { addText(txt, HexUtil.formatHexString(data, true)) }
                }

                override fun onReadFailure(exception: BleException) {
                    //runOnUiThread { addText(txt, exception.toString()) }
                    //连接异常 == 失败
                    Log.e("bleConnect", "onReadFailure: ${exception.description}")
                }
            })
    }

    /**
     * 写入蓝牙
     * code 03 开始 04 结束
     */
    private val OPEN = "03"
    private val END = "04"
    private val CONNECT_SUCCESS = "01"
    private val CONNECT_FAIL = "02"
    private fun bleWrite(bleDevice: BleDevice, code: String?) {
        if (mBleDevice == null) {
            ToastUtils.showShort("未连接蓝牙，请重新连接。")
        }
        if (TextUtils.isEmpty(bleDevice.mac)) {
            return
        }
        var mac = bleDevice.mac.replace(":", "").lowercase(Locale.getDefault())
        mac = "fefa" + mac.substring(mac.length - 8, mac.length)
        //拼接发送
        if (!TextUtils.isEmpty(code)) {
            mac += code
        }
        Log.e("bleConnect", "bleWrite: $mac")
        Log.e("bleConnect", "hexStringToBytes: ${HexUtil.hexStringToBytes(mac).toString()}")
        //监听当前蓝牙是否写入
        val gatt = BleManager.getInstance().getBluetoothGatt(mBleDevice)
        val services = gatt.services
        val bluetoothGattService = services[2]
        val characteristic = bluetoothGattService.characteristics[0]
        BleManager.getInstance().write(
            mBleDevice,
            characteristic?.service?.uuid.toString(),
            characteristic?.uuid.toString(),
            HexUtil.hexStringToBytes(mac),
            object : BleWriteCallback() {
                override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
                    Log.e("bleConnect", "onWriteSuccess: current${current}-  total $total")
                    setBleDevice(bleDevice)
                    //Handler().postDelayed({ searchBle() }, 3000)
                    bleRead(mBleDevice!!)
                }

                override fun onWriteFailure(exception: BleException) {
                    Log.e("bleConnect", "onWriteFailure: " + exception.description)
                }
            })
    }

    private fun setBleDevice(bleDevice: BleDevice) {
        count++
        //处理已连接的设备靠前
        mDeviceAdapter.remove(bleDevice)
        bleDevice.isConnected = true
        bleDevice.isLoading = false
        bleDevice.count = count
        //添加已连接蓝牙
        if (mDeviceAdapter.data.size == 0) {
            mDeviceAdapter.addData(bleDevice)
        } else {
            if (count - 1 <= mDeviceAdapter.data.size) {
                mDeviceAdapter.addData(count - 1, bleDevice)
            } else {
                mDeviceAdapter.addData(bleDevice)
            }
        }
        //                viewBinding.textView.text = "$count"
        bleList.add(bleDevice)
        bleList = removeDuplicate()
        viewBinding.tvConnections.text = "设备连接数：${bleList.size}"
    }

    private fun removeDuplicate(): MutableList<BleDevice> {
        val set: MutableSet<BleDevice> = LinkedHashSet<BleDevice>()
        set.addAll(bleList)
        bleList.clear()
        bleList.addAll(set)
        return bleList
    }

    private fun searchBle() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG)
                .show()
        } else {
            mDeviceAdapter.setList(null)
            if (!isRefresh) {
                viewBinding.tvMsg.visibility = View.INVISIBLE
                startRefresh()
                checkPermissions()
                isRefresh = true
            } else {
                isRefresh = false
                stopRefresh()
                //取消扫描蓝牙设备
                BleManager.getInstance().cancelScan()
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        when (event.code) {
            BaseConstant.EVENT_CPR_START -> {
                clearMap()
                isInitValueMap.clear()
//                bindBluetooth()
                isStart = true
                if (mBleDevice != null) {
                    bleWrite(mBleDevice!!, OPEN)
                }
//                if (usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted){
                mainLooper!!.post { connectUsb() }
//                }
            }
            BaseConstant.EVENT_CPR_BLE_CLOSE -> {
                if (mBleDevice != null) {
                    bleWrite(mBleDevice!!, END)
                }
                clearMap()
                FileUtils.saveThrowableMessage(sb.toString())
            }
            BaseConstant.EVENT_CPR_STOP -> {
                isStart = false
                //unBindBluetooth()
                disconnectUsb()
                if (mBleDevice != null) {
                    bleWrite(mBleDevice!!, END)
                }
                //清空当前map数据
                clearMap()
            }
            BaseConstant.EVENT_CPR_CLEAR -> {
                //清空当前map数据
                clearMap()
            }
            BaseConstant.CLEAR_DEVICE_HISTORY_DATA -> {
                deviceCount = 0
                unBindBluetooth()
                Log.e("hunger_test_clear", " recieve message")
                //清空当前map数据
                clearMap()
                dataMap.clear()
                isInitValueMap.clear()
                Log.e("hunger_test_clear", " clear done")
            }
            BaseConstant.EVENT_DO_BIND -> {
                isInitValueMap.clear()
                bindBluetooth()
            }
            BaseConstant.EVENT_DO_MULTI_START -> {
                clearMap()
                isMulti = true
                isStart = true
                if (mBleDevice != null) {
                    bleWrite(mBleDevice!!, OPEN)
                }
            }
        }
    }

    private fun clearMap() {
        dataDTO = BaseDataDTO()
        dataMap.values.forEach { item ->
            item.dataClear()
            dataMap.remove(item.deviceMAC)
        }
        dataMap.clear()
    }

    private fun bindBluetooth() {
        unBindBluetooth()
        bleList.forEach {
            bind(it)
        }
    }

    private fun unBindBluetooth() {
        bleList.forEach {
            unBind(it)
        }
    }

    private val itemClick =
        OnItemClickListener { adapter, view, position ->
            if (isItemClickable) {
                viewBinding.tvMsg.visibility = View.INVISIBLE
                val bleDevice = mDeviceAdapter.getItem(position)
                if (bleDevice.getmUsbSerialDriver() != null) {
                    connectUsb()
                } else {
                    if (!BleManager.getInstance().isConnected(bleDevice)) {
                        if (count >= 6) {//处理提示语设备连接过多提示
                            viewBinding.tvMsg.text = "当前版本最多同时支持6台模型"
                            hintHandler.postDelayed(this::setTextNull, 2000)
                        }
                        BleManager.getInstance().cancelScan()
                        val bleBluetooth = BleManager.getInstance().getBleBluetooth(bleDevice)
                        if (mBleDevice != null) {
                            bleWrite(bleDevice, "")
                        } else {
                            connectUsb(bleDevice, position)
                        }
                    } else {
                        BleManager.getInstance().disconnect(bleDevice)
                    }
                }
            }
        }

    private val hintHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    private fun setTextNull() {
        viewBinding.tvMsg.text = ""
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted) {
            //mainLooper!!.post { connectUsb() }
        }
    }

    override fun onPause() {
//        if (connected) {
//            LogUtils.e("disconnected")
//            disconnectUsb()
//        }
//        unregisterReceiver(broadcastReceiver)
        super.onPause()
        sb = StringBuffer()
    }

    private fun initBluetooth() {
        BleManager.getInstance().init(application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setConnectOverTime(5000)
            .operateTimeout = 5000
    }

    private val REQUEST_CODE_OPEN_GPS = 1
    private val REQUEST_CODE_PERMISSION_LOCATION = 2
    private fun checkPermissions() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show()
            return
        }
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val permissionDeniedList: MutableList<String> = ArrayList()
        for (permission in permissions) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission)
            } else {
                permissionDeniedList.add(permission)
            }
        }
        if (permissionDeniedList.isNotEmpty()) {
            val deniedPermissions = permissionDeniedList.toTypedArray()
            ActivityCompat.requestPermissions(
                this,
                deniedPermissions,
                REQUEST_CODE_PERMISSION_LOCATION
            )
        }
    }

    private val bluetoothAdapter = object :
        BaseQuickAdapter<BleDevice, BaseViewHolder>(R.layout.item_bluetooth) {
        override fun convert(holder: BaseViewHolder, item: BleDevice) {
            val bind = ItemBluetoothBinding.bind(holder.itemView)
            bind.tvBluetoothName.text = item.name
            bind.tvBluetoothStatus.text = "未连接"
        }
    }

    private fun onPermissionGranted(permission: String) {
        when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.notifyTitle)
                    .setMessage(R.string.gpsNotifyMsg)
                    .setNegativeButton(
                        R.string.cancel
                    ) { dialog, which -> finish() }
                    .setPositiveButton(
                        R.string.setting
                    ) { dialog, which ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivityForResult(intent, REQUEST_CODE_OPEN_GPS)
                    }
                    .setCancelable(false)
                    .show()
            } else {
                initBle()
                startScan()
            }
        }
    }

    private var isItemClickable = true
    private var count = 0
    private fun connectUsb(bleDevice: BleDevice, position: Int) {
        BleManager.getInstance()
            .setConnectOverTime(5000)
            .setOperateTimeout(5000)
            .connect(bleDevice, object : BleGattCallback() {
                override fun onStartConnect() {
                    isItemClickable = false
                    bleDevice.isLoading = true
                    mDeviceAdapter.remove(bleDevice)
                    mDeviceAdapter.addData(position, bleDevice)
                }

                override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                    bleDevice.isLoading = false
                    bleDevice.isConnected = false
                    mDeviceAdapter.remove(bleDevice)
                    mDeviceAdapter.addData(position, bleDevice)
                    isItemClickable = true
                    mBleDevice = null
                    if (exception.code == BleException.ERROR_CODE_TIMEOUT) {
                        viewBinding.tvMsg.text = "连接超时，请重新连接"
                    } else {
                        viewBinding.tvMsg.text = "连接失败，请重新连接"
                    }
                }

                override fun onConnectSuccess(
                    bleDevice: BleDevice,
                    gatt: BluetoothGatt,
                    status: Int
                ) {
                    count++
                    //处理已连接的设备靠前
                    mDeviceAdapter.remove(bleDevice)
                    bleDevice.isConnected = true
                    bleDevice.isLoading = false
                    bleDevice.count = count
                    //添加已连接蓝牙
                    if (mDeviceAdapter.data.size == 0) {
                        mDeviceAdapter.addData(bleDevice)
                    } else {
                        if (count - 1 <= mDeviceAdapter.data.size) {
                            mDeviceAdapter.addData(count - 1, bleDevice)
                        } else {
                            mDeviceAdapter.addData(bleDevice)
                        }
                    }
//                viewBinding.textView.text = "$count"
                    bleList.add(bleDevice)
                    viewBinding.tvConnections.text = "设备连接数：${count}"
                    bind(bleDevice)
                    mBleDevice = bleDevice
                    isItemClickable = true
                    isRefreshPower = true
                }

                override fun onDisConnected(
                    isActiveDisConnected: Boolean,
                    bleDevice: BleDevice,
                    gatt: BluetoothGatt,
                    status: Int
                ) {
                    count--
                    mBleDevice = null
                    bleList.remove(bleDevice)
                    if (isActiveDisConnected) {
                        ToastUtils.showLong(bleDevice.name + getString(R.string.active_disconnected))
                    } else {
                        ToastUtils.showLong(bleDevice.name + getString(R.string.disconnected))
                        ObserverManager.getInstance().notifyObserver(bleDevice)
                    }
                    EventBus.getDefault()
                        .post(
                            MessageEventData(
                                BaseConstant.DEVICE_DISCONNECTED,
                                bleDevice.mac,
                                null
                            )
                        )
                    viewBinding.tvConnections.text = "设备连接数：${count}"

                    //断开蓝牙连接
                    if (BleManager.getInstance().isConnected(bleDevice)) {
                        BleManager.getInstance().disconnect(bleDevice)
                    }
                    if (count == 0) {
                        EventBus.getDefault()
                            .post(
                                MessageEventData(
                                    BaseConstant.EVENT_CPR_DISCONNENT,
                                    "",
                                    null
                                )
                            )
                    }
                    val newList = mutableListOf<BleDevice>()
                    for (item in mDeviceAdapter.data) {
                        if (item.count > bleDevice.count) {
                            item.count--
                        }
                        newList.add(item)
                    }
                    unBind(bleDevice)
                    mDeviceAdapter.remove(bleDevice)
                    newList.remove(bleDevice)
                    bleDevice.isConnected = false
                    bleDevice.isLoading = false
                    bleDevice.count = 0
                    newList.add(bleDevice)
                    mDeviceAdapter.setList(dedupList(newList))
//                    refresh()
                    isItemClickable = true
                }
            })
    }

    /**
     * 去重
     */
    private fun dedupList(list: MutableList<BleDevice>?): MutableList<BleDevice>? {
        list?.distinctBy { listOf(it.mac, it.mac) }
        return list
    }

    private fun initBle() {
        val uuids: Array<String>?
//        val str_uuid: String = et_uuid.getText().toString()
        val str_uuid: String = ""
        uuids = if (TextUtils.isEmpty(str_uuid)) {
            null
        } else {
            str_uuid.split(",").toTypedArray()
        }
        var serviceUuids: Array<UUID?>? = null
        if (uuids != null && uuids.isNotEmpty()) {
            serviceUuids = arrayOfNulls(uuids.size)
            for (i in uuids.indices) {
                val name = uuids[i]
                val components = name.split("-").toTypedArray()
                if (components.size != 5) {
                    serviceUuids[i] = null
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i])
                }
            }
        }

        val names = arrayOf("WMFS", "D87A")
        val mac: String = ""
        val isAutoConnect: Boolean = false
        val scanRuleConfig = BleScanRuleConfig.Builder()
            .setServiceUuids(serviceUuids) // 只扫描指定的服务的设备，可选
            .setDeviceName(true, *names) // 只扫描指定广播名的设备，可选
            .setDeviceMac(mac) // 只扫描指定mac的设备，可选
            .setAutoConnect(isAutoConnect) // 连接时的autoConnect参数，可选，默认false
            .setScanTimeOut(5000) // 扫描超时时间，可选，默认10秒
            .build()
        BleManager.getInstance().initScanRule(scanRuleConfig)
    }

    private fun startScan() {
//        viewBinding.tvModelNum.visibility = View.INVISIBLE
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                //已连接的蓝牙添加进来
                val deviceList = BleManager.getInstance().allConnectedDevice
                mDeviceAdapter.data.clear()
                mDeviceAdapter.setList(dedupList(deviceList))
            }

            override fun onLeScan(bleDevice: BleDevice) {
                super.onLeScan(bleDevice)
            }

            override fun onScanning(bleDevice: BleDevice) {
                //处理重复蓝牙问题
                if (mDeviceAdapter.data.isNotEmpty()) {
                    mDeviceAdapter.data.forEach { item ->
                        if (bleDevice.mac == item.mac) {
                            mDeviceAdapter.remove(item)
                        }
                    }
                }
                mDeviceAdapter.addData(bleDevice)
                mDeviceAdapter.notifyDataSetChanged()
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
                mDeviceAdapter.data.let {
                    if (it.size == 0) {
                        viewBinding.tvMsg.visibility = View.VISIBLE
                    }
                }
                stopRefresh()
                isRefresh = false
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSION_LOCATION -> if (grantResults.size > 0) {
                var i = 0
                while (i < grantResults.size) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        permissions[i]?.let { onPermissionGranted(it) }
                    }
                    i++
                }
            }
        }
    }

    private fun checkGPSIsOpen(): Boolean {
        val locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
//                initBle()
                startScan()
            }
        }
    }

    /**
     * 开始刷新动画
     */
    private fun startRefresh() {
        viewBinding.tvDesc.setText(R.string.searching_for_available_models)
        viewBinding.progressCircular.indeterminateDrawable =
            resources.getDrawable(R.drawable.progressbar_circle)
        viewBinding.progressCircular.progressDrawable =
            resources.getDrawable(R.drawable.progressbar_circle)
    }

    /**
     * 停止刷新动画
     */
    private fun stopRefresh() {
        viewBinding.tvDesc.setText(R.string.available_models)
        viewBinding.progressCircular.indeterminateDrawable =
            resources.getDrawable(R.drawable.icon_wm_refresh)
        viewBinding.progressCircular.progressDrawable =
            resources.getDrawable(R.drawable.icon_wm_refresh)
    }

    private fun unBind(bleDevice: BleDevice) {
        val gatt = BleManager.getInstance().getBluetoothGatt(bleDevice) ?: return
        //蓝牙服务列表
        val services = gatt.services
        val bluetoothGattService = services[2]
        val characteristic = bluetoothGattService.characteristics[1]
        BleManager.getInstance().stopNotify(
            bleDevice,
            characteristic.service.uuid.toString(),
            characteristic.uuid.toString()
        )
//        mBleDevice = null
    }

    private var mBleDevice: BleDevice? = null

    //    var deviceCount = 0
    private fun bind(bleDevice: BleDevice?) {
        val gatt = BleManager.getInstance().getBluetoothGatt(bleDevice) ?: return
        //蓝牙服务列表
        val services = gatt.services
        val bluetoothGattService = services[2]
        val characteristic = bluetoothGattService.characteristics[1]
        BleManager.getInstance().notify(
            bleDevice,
            characteristic?.service?.uuid.toString(),
            characteristic?.uuid.toString(),
            object : BleNotifyCallback() {
                override fun onNotifySuccess() {
                    mBleDevice = bleDevice
//                    deviceCount++
//                    if (deviceCount == bleList.size) {
                    EventBus.getDefault()
                        .post(MessageEventData(BaseConstant.EVENT_CANCEL_DIALOG, "", null))
//                    }
                    runOnUiThread(Runnable {
                        Log.i("CPRActivity", "notify success")
                    })
                }

                override fun onNotifyFailure(exception: BleException) {
                    runOnUiThread(Runnable {
                        Log.i("CPRActivity", exception.toString())
                    })
                }

                override fun onCharacteristicChanged(data: ByteArray) {
                    /*         val formatHexString = HexUtil.formatHexString(
                                 characteristic.value,
                                 false
                             )*/
                    val formatHexString = HexUtil.formatHexString(
                        data,
                        false
                    )
                    sb.append(formatHexString)
                    sb.append("\n")
                    if (!BaseApplication.driver?.isConnected!! && characteristic != null) {
                        runOnUiThread { Log.e("CPRActivity", formatHexString) }
                        sendMessage(formatHexString)
                    }
                }
            })
    }

    private var isRefreshPower: Boolean = false
    private var isStart = false
    private var isMulti = false
    private var deviceCount: Int = 0
    private var sb = StringBuffer()
    private var dataDTO = BaseDataDTO()
    private val dataMap = mutableMapOf<String, DataVolatile01>()

    private fun sendMessage(formatHexString: String) {
        if (TextUtils.isEmpty(formatHexString) || formatHexString.length < 20) {
            return
        }
        data.clear()
        val num = formatHexString.length.div(20)
        if (formatHexString.length > 20) {
            for (index in 1..num) {
                val oneData = formatHexString.substring(20 * (index - 1), 20 * index)
                setData(oneData)
            }
        } else if (formatHexString.length == 20) {
            setData(formatHexString)
        }

        //处理连接后电量显示
//        if (isRefreshPower) {
//            powerHandler.removeCallbacks(powerRunning)
//            powerHandler.postDelayed(powerRunning, 10000)
//            Handler().postDelayed(this::setPower, 500)
//        }
    }

    private fun setData(data: String) {
        val deviceMAC =
            "001b${data.substring(12)}"
        val dataVolatile = dataMap[deviceMAC]
        if (dataVolatile != null) {
            dataDTO = dataVolatile.baseDataDecode(data)
        } else {
            val newDataVolatile = DataVolatile01()
            newDataVolatile.initPreDistance(data, deviceMAC)
            dataMap[deviceMAC] = newDataVolatile
        }
        Log.e("setData", GsonUtils.toJson(dataDTO))
        if (isStart) {
            StatusLiveData.dataSingle.value = dataDTO
            Log.e("setDatacf", "${dataDTO.cf}")
            //曲线模型数据
            StatusLiveData.dataSingleChart.value = dataDTO
        }
    }

    val data = ArrayList<BaseDataDTO>()

    private val powerHandler = Handler(Looper.getMainLooper())
    private val powerRunning = Runnable {
        setPower()
        powerHandler.postDelayed({
            setPower()
        }, 10000)
    }

    /**
     * 设置列表电量
     */
    private fun setPower() {
        val dataList = mutableListOf<BleDevice>()
        mDeviceAdapter.data.forEachIndexed { index, item ->
            if (item.isConnected) {
                //将mac字符串转换一下去掉 冒号 ：
                val replaceMac = item.mac.replace(":", "").toLowerCase()
                Log.e("sendMessage", "ble MAC：${replaceMac}")
                dataMap.keys.forEach {
                    val dataItme = dataMap[it]
                    if (dataItme != null && replaceMac == dataItme.deviceMAC) {
                        Log.e("sendMessage", "MAC：${dataItme.deviceMAC}  电量值：${dataItme.VI_Value}")
                        item.power = dataItme.VI_Value
//                        return@forEach
                    }
                }
                mDeviceAdapter.notifyItemChanged(index, item)
            }
            dataList.add(item)
        }
        isRefreshPower = false
        mDeviceAdapter.setList(dataList)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        unBindBluetooth()
        disconnectUsb()
        unregisterReceiver(broadcastReceiver)
        BleManager.getInstance().disconnectAllDevice()
        BleManager.getInstance().destroy()
    }

    private val deviceId = 0
    private var portNum: Int = 0
    private var baudRate: Int = 115200
    private val withIoManager = true
    private val INTENT_ACTION_GRANT_USB: String = "android.hardware.usb.action.USB_STATE"

    private enum class UsbPermission {
        Unknown, Requested, Granted, Denied
    }

    private var mainLooper: Handler? = null
//    private val controlLines: ControlLines? = null

    private var usbIoManager: SerialInputOutputManager? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var usbPermission: UsbPermission = UsbPermission.Unknown
    private var connected = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (INTENT_ACTION_GRANT_USB == intent.action) {
                usbPermission = if (intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED,
                        false
                    )
                ) UsbPermission.Granted else UsbPermission.Denied
                connectUsb()
            }
        }
    }

    private fun refresh() {
        if (viewBinding.cbUsb.isChecked) {
            val usbManager = getSystemService(USB_SERVICE) as UsbManager
            val usbDefaultProber: UsbSerialProber = UsbSerialProber.getDefaultProber()
            val usbCustomProber: UsbSerialProber = getCustomProber()
            //        if (usbManager.deviceList.values == null || usbManager.deviceList.values.isEmpty()) {
            //            viewBinding.ctUsb.isChecked = false
            //        }
            var bleDevice: BleDevice? = null
            for (device in usbManager.deviceList.values) {
                val driver: UsbSerialDriver = usbDefaultProber.probeDevice(device)
                //ToastUtils.showShort(driver::class.java.simpleName.replace("SerialDriver", ""))
                mDeviceAdapter.setList(null)
                for (port in driver.ports.indices) {
                    bleDevice = BleDevice(driver)
                    mDeviceAdapter.remove(bleDevice)
                    mDeviceAdapter.addData(bleDevice)
                }
            }
            if (bleDevice != null) {
                //connectUsb()
            }
            stopRefresh()
        }
    }

    /*
     * Serial + UI
     */
    private fun connectUsb() {
        var device: UsbDevice? = null
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        for (v in usbManager.deviceList.values) if (v.deviceId > deviceId) device = v
        if (device == null) {
            LogUtils.e("connection failed: device not found")
            return
        }
        var driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        if (driver == null) {
            driver = getCustomProber().probeDevice(device)
        }
        if (driver == null) {
            LogUtils.e("connection failed: no driver for device")
            return
        }
        if (driver.ports.size < portNum) {
            LogUtils.e("connection failed: not enough ports at device")
            return
        }
        usbSerialPort = driver.ports[portNum]
        val usbConnection = usbManager.openDevice(driver.device)
        if (usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(
                driver.device
            )
        ) {
            usbPermission = UsbPermission.Requested
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val usbPermissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(INTENT_ACTION_GRANT_USB),
                flags
            )
            usbManager.requestPermission(driver.device, usbPermissionIntent)
            return
        }
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.device))
                LogUtils.e("connection failed: permission denied")
            else
                LogUtils.e(
                    "connection failed: open failed"
                )
            return
        }
        try {
            usbSerialPort!!.open(usbConnection)
            usbSerialPort!!.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE)
            usbSerialPort!!.readEndpoint
            if (withIoManager) {
                usbIoManager = SerialInputOutputManager(usbSerialPort, this)
                usbIoManager!!.start()
            }
            LogUtils.e("connected")
            connected = true
            //controlLines.start()
        } catch (e: java.lang.Exception) {
            LogUtils.e("connection failed: " + e.message)
            disconnectUsb()
        }
    }

    private fun disconnectUsb() {
        connected = false
        //controlLines.stop()
        if (usbIoManager != null) {
            usbIoManager!!.listener = null
            usbIoManager!!.stop()
        }
        usbIoManager = null
        try {
            if (usbSerialPort != null)
                usbSerialPort!!.close()
        } catch (ignored: IOException) {
            ignored.printStackTrace()
        }
        usbSerialPort = null
    }

    /*
     * Serial
     */
    override fun onNewData(data: ByteArray?) {
        mainLooper!!.post {
            if (data != null && data.isNotEmpty()) {
                val formatHexString = ConvertUtil.toHexString(data, data.size)
                Log.i("CPRActivity", "data -- ${formatHexString.trim()}")
                sendMessage(formatHexString.trim())
            }
        }
    }

    override fun onRunError(e: Exception) {
        mainLooper!!.post {
            LogUtils.e("connection lost: " + e.message)
            disconnectUsb()
        }
    }


    override fun finish() {
        super.finish()
        BleManager.getInstance().disconnectAllDevice()
        BleManager.getInstance().destroy()
    }
}