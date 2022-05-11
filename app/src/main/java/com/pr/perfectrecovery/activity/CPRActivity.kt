package com.pr.perfectrecovery.activity

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import com.clj.fastble.utils.HexUtil
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
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
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.LinkedHashSet

/**
 * CPR页面  蓝夜列表扫描链接
 */
class CPRActivity : BaseActivity() {
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

    //监听USB连接状态
    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == "android.hardware.usb.action.USB_STATE") {
                val connected = intent.extras!!.getBoolean("connected")
                if (connected) {
//                    Toast.makeText(this@CPRActivity, "USB已连接", Toast.LENGTH_SHORT).show()
//                    viewBinding.tvMsg.text = "USB已连接"
                } else {
//                    viewBinding.tvMsg.text = "USB已断开"
//                    Toast.makeText(this@CPRActivity, "USB已断开", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCpractivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        EventBus.getDefault().register(this)
        registerBroadcast()
        initView()
        //初始化蓝牙管理器
        initBluetooth()
        initBle()
        ttl()
        viewBinding.cbBle.isChecked = true
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
                    mDeviceAdapter.addData(bleDevice)
                }
            }
            if (bleDevice != null) {
                openTTL(bleDevice, 0)
            }
            stopRefresh()
            //listAdapter.notifyDataSetChanged()
            //        handler.removeCallbacks(runnable)
            //        handler.postDelayed(runnable, 2000)
        }
    }

    private fun registerBroadcast() {
        val filter = IntentFilter()
        filter.addAction("android.hardware.usb.action.USB_STATE")
        registerReceiver(broadcastReceiver, filter)
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
            intent.putParcelableArrayListExtra(BaseConstant.CONNECT_BLE_DEVICES, connectList)
            startActivity(intent)
        }

        viewBinding.progressCircular.setOnClickListener {
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
                refresh()
            }
        }

        viewBinding.cbBle.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                mDeviceAdapter.setList(null)
                BaseApplication.driver?.CloseDevice()
                searchBle()
                viewBinding.cbUsb.isChecked = !isChecked
                viewBinding.cbBle.isChecked = isChecked
            }
        }
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
                isInitValueMap.clear()
                bindBluetooth()
                isStart = true
            }
            BaseConstant.EVENT_CPR_STOP -> {
                isStart = false
                unBindBluetooth()
                //清空当前map数据
                dataMap.values.forEach { item ->
                    item.dataClear()
                }
            }
            BaseConstant.EVENT_CPR_CLEAR -> {
                //清空当前map数据
                dataMap.values.forEach { item ->
                    item.dataClear()
                }
            }
            BaseConstant.EVENT_CPR_CHECK -> {
                Log.e("CPRActivity", "${event.isCheck}")
                DataVolatile01.setModel(event.isCheck)
            }
            BaseConstant.CLEAR_DEVICE_HISTORY_DATA -> {
                deviceCount = 0
                unBindBluetooth()
                Log.e("hunger_test_clear", " recieve message")
                //清空当前map数据
                dataMap.values.forEach { item ->
                    item.dataClear()
                }
                dataMap.clear()
                isInitValueMap.clear()
                Log.e("hunger_test_clear", " clear done")

            }
            BaseConstant.EVENT_DO_BIND -> {
                isInitValueMap.clear()
                bindBluetooth()
            }
            BaseConstant.EVENT_DO_START -> {
                isStart = true
            }
        }
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
                    openTTL(bleDevice, position)
                } else {
                    if (!BleManager.getInstance().isConnected(bleDevice)) {
                        if (count >= 6) {//处理提示语设备连接过多提示
                            viewBinding.tvMsg.text = "当前版本最多同时支持6台模型"
                            hintHandler.postDelayed(this::setTextNull, 2000)
                        }
                        BleManager.getInstance().cancelScan()
                        connect(bleDevice, position)
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
//        showConnectedDevice()
//        startRefresh()
//        checkPermissions()
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
    private fun connect(bleDevice: BleDevice, position: Int) {
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

//                unBind(bleDevice)
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
                    isItemClickable = true
                }
            })
    }

    /**
     * 去重
     */
    private fun dedupList(list: MutableList<BleDevice>?): MutableList<BleDevice>? {
        val set = LinkedHashSet<BleDevice>()
        if (list != null) {
            set.addAll(list)
            list.clear()
            list.addAll(set)
        }
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
    }

    private val dataMap = mutableMapOf<String, DataVolatile01>()
    private var dataDTO = BaseDataDTO()
    var deviceCount = 0
    private fun bind(bleDevice: BleDevice?) {
        val gatt = BleManager.getInstance().getBluetoothGatt(bleDevice)
        //蓝牙服务列表
        val services = gatt.services
        val bluetoothGattService = services[2]
        val characteristic = bluetoothGattService.characteristics[1]
        BleManager.getInstance().notify(
            bleDevice,
            characteristic.service.uuid.toString(),
            characteristic.uuid.toString(),
            object : BleNotifyCallback() {
                override fun onNotifySuccess() {
                    deviceCount++
                    if (deviceCount == bleList.size) {
                        EventBus.getDefault()
                            .post(MessageEventData(BaseConstant.EVENT_CANCEL_DIALOG, "", null))
                    }
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
                    if (!BaseApplication.driver?.isConnected!! && characteristic != null) {
                        val formatHexString = HexUtil.formatHexString(
                            characteristic.value,
                            false
                        )
                        runOnUiThread { Log.e("CPRActivity", formatHexString) }
                        Log.e("TAG9", "原始数据${formatHexString}")
                        sendMessage(formatHexString)
                    }
                }
            })
    }

    private var isRefreshPower: Boolean = false
    private var isStart = false
    private fun sendMessage(formatHexString: String) {
        if (TextUtils.isEmpty(formatHexString) || formatHexString.length < 18) {
            return
        }
        val deviceMAC =
            "001b${formatHexString.substring(12)}"
        Log.e("TAG", "原始数据${formatHexString}")
        Log.e("TAG", "MAC:${deviceMAC}")
        val dataVolatile = dataMap[deviceMAC]
        if (dataVolatile != null) {
            dataDTO = dataVolatile.parseString(formatHexString)
        } else {
            val mDataVolatile = DataVolatile01()
            mDataVolatile.initPreDistance(formatHexString, deviceMAC)
            dataDTO = mDataVolatile.parseString(formatHexString)
            dataMap[dataDTO.mac] = mDataVolatile
        }
        if (isStart) {
            StatusLiveData.data.postValue(dataDTO)
        }
        //处理连接后电量显示
        if (isRefreshPower) {
            powerHandler.removeCallbacks(powerRunning)
            powerHandler.postDelayed(powerRunning, 10000)
            Handler().postDelayed(this::setPower, 500)
        }
    }

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
                        Log.e("sendMessage", "MAC：${dataItme.deviceMAC}")
                        Log.e("sendMessage", "电量值：${dataItme.VI_Value}")
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
        BleManager.getInstance().disconnectAllDevice()
        BleManager.getInstance().destroy()
    }

    private fun initTTL() {
        if (BaseApplication.driver!!.isConnected) {
            val retval: Int? = BaseApplication.driver?.ResumeUsbPermission()
            if (retval == 0) {
                if (BaseApplication.driver!!.SetConfig(
                        115200, 8, 1, 0, 0
                    )//配置串口波特率，函数说明可参照编程手册
                ) {
                    ToastUtils.showShort("串口设置成功!")
                } else {
                    ToastUtils.showShort("串口设置失败!")
                }
            } else if (retval == -2) {
                ToastUtils.showShort("获取权限失败!")
            }
        }
    }

    private fun ttl() {
        if (!BaseApplication.driver?.UsbFeatureSupported()!!) // 判断系统是否支持USB HOST
        {
            val dialog: androidx.appcompat.app.AlertDialog =
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton(
                        "确认"
                    ) { arg0, arg1 ->
                        {
                            //exitProcess(0)
                        }
                    }.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    private fun openTTL(bleDevice: BleDevice, position: Int) {
        if (!BaseApplication.driver?.isConnected!!) {
            when (BaseApplication.driver?.ResumeUsbList()) {
                -1 -> { // ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
                    ToastUtils.showShort("打开设备失败!")
                    BaseApplication.driver?.CloseDevice()
                }
                0 -> {
                    if (!BaseApplication.driver?.UartInit()!!) { //对串口设备进行初始化操作
                        ToastUtils.showShort("设备初始化失败!")
                        return
                    }
                    ToastUtils.showShort("打开设备成功!")
                    bleDevice.isConnected = true
                    bleDevice.isLoading = false
                    mDeviceAdapter.notifyItemChanged(position, bleDevice)
                    mDeviceAdapter.notifyDataSetChanged()
                    initTTL()
                    ReadThread().start() //开启读线程读取串口接收的数据
                }
                else -> {
                    Toast.makeText(this, "USB未授权限!", Toast.LENGTH_SHORT).show()
//                    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
//                    builder.setIcon(R.mipmap.icon_wm_logo)
//                    builder.setTitle("未授权限")
//                    builder.setMessage("确认退出吗？")
//                    builder.setPositiveButton(
//                        "确定"
//                    ) { dialog, which ->
//                        //System.exit(0)
//                        dialog.dismiss()
//                    }
//                    builder.setNegativeButton(
//                        "返回"
//                    ) { dialog, which ->
//                        dialog.dismiss()
//                    }
//                    builder.show()
                }
            }
        } else {
            //关闭USB TTL
            Toast.makeText(this, "关闭USB串口!", Toast.LENGTH_SHORT).show()
            try {
                ToastUtils.showShort("打开设备成功!")
                bleDevice.isConnected = false
                bleDevice.isLoading = false
                mDeviceAdapter.notifyItemChanged(position, bleDevice)
                mDeviceAdapter.notifyDataSetChanged()
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            BaseApplication.driver?.CloseDevice()
//            totalrecv = 0
        }
    }

    inner class ReadThread : Thread() {
        override fun run() {
            val buffer = ByteArray(64)
            while (true) {
                val msg = Message.obtain()
                if (!BaseApplication.driver?.isConnected!!) {
                    break
                }
                val length: Int = BaseApplication.driver!!.ReadData(buffer, 64)
                if (length > 0) {
                    runOnUiThread {
                        val formatHexString = ConvertUtil.toHexString(buffer, length)
                        Log.i("CPRActivity", "data -- ${formatHexString.trim()}")
                        sendMessage(formatHexString.trim())
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        BleManager.getInstance().disconnectAllDevice()
        BleManager.getInstance().destroy()
    }

}