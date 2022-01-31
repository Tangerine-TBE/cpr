package com.pr.perfectrecovery.activity

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.pr.perfectrecovery.BaseApplication
import com.pr.perfectrecovery.R
import com.pr.perfectrecovery.adapter.DeviceBluetoothAdapter
import com.pr.perfectrecovery.base.BaseActivity
import com.pr.perfectrecovery.base.BaseConstant
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.bean.MessageEventData
import com.pr.perfectrecovery.comm.ObserverManager
import com.pr.perfectrecovery.databinding.ActivityCpractivityBinding
import com.pr.perfectrecovery.databinding.ItemBluetoothBinding
import com.pr.perfectrecovery.livedata.StatusLiveData
import com.pr.perfectrecovery.utils.ConvertUtil
import com.pr.perfectrecovery.utils.DataVolatile
import com.pr.perfectrecovery.view.itemlayout.PagerConfig
import com.pr.perfectrecovery.view.itemlayout.PagerGridLayoutManager
import com.pr.perfectrecovery.view.itemlayout.PagerGridSnapHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList

/**
 * CPR页面  蓝夜列表扫描链接
 */
class CPRActivity : BaseActivity() {
    private lateinit var viewBinding: ActivityCpractivityBinding
    private var isRefresh = false
    private val mDeviceAdapter = DeviceBluetoothAdapter()
    private var bleList = mutableListOf<BleDevice>()
    private var connectList = arrayListOf<BleDevice>()
    private var isStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCpractivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        EventBus.getDefault().register(this)
        initView()
        //初始化蓝牙管理器
        initBluetooth()
    }

    private fun initView() {
        //查看是否有蓝牙权限
        checkPermissions()
        viewBinding.bottom.ivBack.setOnClickListener { finish() }
        viewBinding.bottom.tvContinue.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }

        viewBinding.btnOpen.setOnClickListener {
            //ttl()
            openTTL()
        }

        viewBinding.tvLog.movementMethod = ScrollingMovementMethod.getInstance()

        viewBinding.recyclerview.layoutManager = LinearLayoutManager(this)
        // 是否抵用滚动事件监听
        //layoutManager.setChangeSelectInScrolling(false)
        viewBinding.recyclerview.adapter = bluetoothAdapter

        viewBinding.bottom.ivStart.setOnClickListener {
            val intent = Intent(this, TrainingSingleActivity::class.java)
            intent.putParcelableArrayListExtra("blueTooth", connectList)
            startActivity(intent)
        }

        viewBinding.progressCircular.setOnClickListener {
            if (!isRefresh) {
                viewBinding.tvModelNum.visibility = View.INVISIBLE
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
        viewBinding.recyclerview.adapter = mDeviceAdapter
        mDeviceAdapter.setOnItemClickListener(itemClick)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onEvent(event: MessageEventData) {
        if (event.code == BaseConstant.EVENT_CPR_START) {
            //循环次数
            bindBluetooth()
        } else if (event.code == BaseConstant.EVENT_CPR_STOP) {
            unBindBluetooth()
        }
    }

    private fun bindBluetooth() {
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
            if (isItemClick) {
                val bleDevice = mDeviceAdapter.getItem(position)
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    if (count >= 6) {//处理提示语设备连接过多提示
                        viewBinding.tvModelNum.text = "当前版本最多同时支持6台模型"
                        hintHandler.postDelayed(this::setTextNull, 2000)
                    }
                    BleManager.getInstance().cancelScan()
                    connect(bleDevice, position)
                } else {
                    BleManager.getInstance().disconnect(bleDevice)
                }
            }
        }

    private val hintHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    private fun setTextNull() {
        viewBinding.tvModelNum.text = ""
    }

    override fun onResume() {
        super.onResume()
        showConnectedDevice()
//        startRefresh()
//        checkPermissions()
    }

    private fun initBluetooth() {
        BleManager.getInstance().init(application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setConnectOverTime(10000).operateTimeout = 5000

    }

    private val REQUEST_CODE_OPEN_GPS = 1
    private val REQUEST_CODE_PERMISSION_LOCATION = 2
    private fun checkPermissions() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
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

    private var pageSize = 0

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
                setScanRule()
                startScan()
            }
        }
    }

    private fun showConnectedDevice() {
        val deviceList = BleManager.getInstance().allConnectedDevice
        bleList = deviceList
        mDeviceAdapter.setList(deviceList)
    }

    private var isItemClick = true
    private var count = 0
    private fun connect(bleDevice: BleDevice, position: Int) {
        BleManager.getInstance()
            .setConnectOverTime(5000)
            .setOperateTimeout(5000)
            .connect(bleDevice, object : BleGattCallback() {
                override fun onStartConnect() {
                    isItemClick = false
                    bleDevice.isLoading = true
                    mDeviceAdapter.remove(bleDevice)
                    mDeviceAdapter.addData(position, bleDevice)
                }

                override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                    bleDevice.isLoading = false
                    mDeviceAdapter.remove(bleDevice)
                    mDeviceAdapter.addData(position, bleDevice)
                    isItemClick = true
                    if (exception.code == BleException.ERROR_CODE_TIMEOUT) {
                        viewBinding.tvModelNum.text = "连接超时，请重新连接"
                    } else {
                        viewBinding.tvModelNum.text = "连接失败，请重新连接"
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
                    bleDevice.isLoading = false
                    bleDevice.count = count
                    if (mDeviceAdapter.data.size == 0) {
                        mDeviceAdapter.addData(bleDevice)
                    } else {
                        mDeviceAdapter.addData(count - 1, bleDevice)
                    }
//                viewBinding.textView.text = "$count"
                    bleList.add(bleDevice)
                    viewBinding.tvConnections.text = "设备连接数：${count}"
                    //bind(bleDevice)
                    isItemClick = true
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
                    viewBinding.tvConnections.text = "设备连接数：${count}"
                    //断开蓝牙连接
                    if (BleManager.getInstance().isConnected(bleDevice)) {
                        BleManager.getInstance().disconnect(bleDevice)
                    }
//                unBind(bleDevice)
                    mDeviceAdapter.remove(bleDevice)
                    bleDevice.isLoading = false
                    bleDevice.count = 0
                    mDeviceAdapter.addData(bleDevice)
                    isItemClick = true
                }
            })
    }

    private fun setScanRule() {
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

//        val str_name: String = et_name.getText().toString()
        var names = arrayOf("WMFS", "D87A")
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
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                //已连接的蓝牙添加进来
                val deviceList = BleManager.getInstance().allConnectedDevice
                mDeviceAdapter.setList(deviceList)

            }

            override fun onLeScan(bleDevice: BleDevice) {
                super.onLeScan(bleDevice)
            }

            override fun onScanning(bleDevice: BleDevice) {
                mDeviceAdapter.addData(bleDevice)
                mDeviceAdapter.notifyDataSetChanged()
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
                mDeviceAdapter.data.let {
                    if (it.size == 0) {
                        viewBinding.tvModelNum.visibility = View.VISIBLE

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
                setScanRule()
                startScan()
            }
        }
    }

    /**
     * 开始刷新动画
     */
    private fun startRefresh() {
        viewBinding.progressCircular.indeterminateDrawable =
            resources.getDrawable(R.drawable.progressbar_circle)
        viewBinding.progressCircular.progressDrawable =
            resources.getDrawable(R.drawable.progressbar_circle)
    }

    /**
     * 停止刷新动画
     */
    private fun stopRefresh() {
        viewBinding.progressCircular.indeterminateDrawable =
            resources.getDrawable(R.drawable.icon_wm_refresh)
        viewBinding.progressCircular.progressDrawable =
            resources.getDrawable(R.drawable.icon_wm_refresh)
    }

    private fun unBind(bleDevice: BleDevice) {
        val gatt = BleManager.getInstance().getBluetoothGatt(bleDevice)
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
                    val dataDTO = DataVolatile.parseString(
                        HexUtil.formatHexString(
                            characteristic.value,
                            false
                        )
                    )
                    runOnUiThread { Log.i("CPRActivity", "${bleDevice?.name}") }
                    //发送数据
//                    EventBus.getDefault()
//                        .post(MessageEventData(BaseConstant.EVENT_SINGLE_DATA_REFRESH, "", dataDTO))
                    StatusLiveData.data.postValue(dataDTO)
                    runOnUiThread(Runnable {
                        Log.i(
                            "CPRActivity", ConvertUtil.hexToString(
                                HexUtil.formatHexString(
                                    characteristic.value,
                                    false
                                )
                            )
                        )
                        addText(viewBinding.tvLog, "deviceName：${bleDevice?.name}")
                        addText(viewBinding.tvLog, "MAC：${bleDevice?.mac}")
                        addText(viewBinding.tvLog, "电量值：" + DataVolatile.VI_Value)
                        addText(viewBinding.tvLog, "距离值：" + DataVolatile.L_Value)
                        addText(viewBinding.tvLog, "气压值：" + DataVolatile.QY_Value)
                        addText(viewBinding.tvLog, "蓝牙连接值：" + DataVolatile.BLS_Value)
                        addText(viewBinding.tvLog, "USB连接值：" + DataVolatile.ULS_Value)
                        addText(viewBinding.tvLog, "通道打开状态值：" + DataVolatile.TOS_Value)
                        addText(viewBinding.tvLog, "连接方式值：" + DataVolatile.LKS_Value)
                        addText(viewBinding.tvLog, "按压位置正确值：" + DataVolatile.PSR_Value)
                        addText(viewBinding.tvLog, "工作方式值：" + DataVolatile.WS_Value)
                        addText(viewBinding.tvLog, "按压频率值：" + DataVolatile.PF_Value)
                        addText(viewBinding.tvLog, "吹气频率值：" + DataVolatile.CF_Value)
                    })
                }
            })
    }

    private fun addText(textView: TextView, content: String) {
        textView.append(content)
        textView.append("\n")
        val offset = textView.lineCount * textView.lineHeight
        if (offset > textView.height) {
            textView.scrollTo(0, offset - textView.height)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun initTTL() {
        if (!BaseApplication.driver!!.isConnected) {
            val retval: Int? = BaseApplication.driver?.ResumeUsbPermission()
            if (retval == 0) {
                if (BaseApplication.driver!!.SetConfig(
                        9600, 8, 1, 0, 0
                    )//配置串口波特率，函数说明可参照编程手册
                ) {
                    Toast.makeText(
                        this, "串口设置成功!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this, "串口设置失败!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (retval == -2) {
                Toast.makeText(
                    this, "获取权限失败!",
                    Toast.LENGTH_SHORT
                ).show()
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
                    ) { arg0, arg1 -> System.exit(0) }.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    private fun openTTL() {
        if (!isOpen) {
            val retval = BaseApplication.driver?.ResumeUsbList()
            when (retval) {
                -1 -> { // ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
                    Toast.makeText(
                        this, "打开设备失败!",
                        Toast.LENGTH_SHORT
                    ).show()
                    BaseApplication.driver?.CloseDevice()
                }
                0 -> {
                    if (!BaseApplication.driver?.UartInit()!!) { //对串口设备进行初始化操作
                        Toast.makeText(
                            this, "设备初始化失败!",
                            Toast.LENGTH_SHORT
                        ).show()
                        Toast.makeText(
                            this, "打开" +
                                    "设备失败!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    Toast.makeText(
                        this, "打开设备成功!",
                        Toast.LENGTH_SHORT
                    ).show()
                    isOpen = true
                    readThread().start() //开启读线程读取串口接收的数据
                }
                else -> {
                    val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                    builder.setIcon(R.mipmap.icon_wm_logo)
                    builder.setTitle("未授权限")
                    builder.setMessage("确认退出吗？")
                    builder.setPositiveButton(
                        "确定"
                    ) { dialog, which ->
                        //System.exit(0)
                        dialog.dismiss()
                    }
                    builder.setNegativeButton(
                        "返回"
                    ) { dialog, which ->
                        dialog.dismiss()
                    }
                    builder.show()
                }
            }
        } else {
            //关闭USB TTL
            isOpen = false
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            BaseApplication.driver?.CloseDevice()
//            totalrecv = 0
        }
    }

    private var isOpen = false

    inner class readThread : Thread() {
        override fun run() {
            val buffer = ByteArray(64)
            while (true) {
                val msg = Message.obtain()
                if (!isOpen) {
                    break
                }
                val length: Int = BaseApplication.driver!!.ReadData(buffer, 64)
                if (length > 0) {
                    //if (md_switch == 1) {
                    runOnUiThread(Runnable {
                        Log.i(
                            "CPRActivity", ConvertUtil.hexToString(
                                HexUtil.formatHexString(
                                    buffer,
                                    false
                                )
                            )
                        )
                        addText(viewBinding.tvLog, "deviceTTL：调试")
                        addText(viewBinding.tvLog, "电量值：" + DataVolatile.VI_Value)
                        addText(viewBinding.tvLog, "距离值：" + DataVolatile.L_Value)
                        addText(viewBinding.tvLog, "气压值：" + DataVolatile.QY_Value)
                        addText(viewBinding.tvLog, "蓝牙连接值：" + DataVolatile.BLS_Value)
                        addText(viewBinding.tvLog, "USB连接值：" + DataVolatile.ULS_Value)
                        addText(viewBinding.tvLog, "通道打开状态值：" + DataVolatile.TOS_Value)
                        addText(viewBinding.tvLog, "连接方式值：" + DataVolatile.LKS_Value)
                        addText(viewBinding.tvLog, "按压位置正确值：" + DataVolatile.PSR_Value)
                        addText(viewBinding.tvLog, "工作方式值：" + DataVolatile.WS_Value)
                        addText(viewBinding.tvLog, "按压频率值：" + DataVolatile.PF_Value)
                        addText(viewBinding.tvLog, "吹气频率值：" + DataVolatile.CF_Value)
                    })
//                    } else {
//                        val recv = String(buffer, 0, length)
//                        msg.obj = recv
//                    }
                    //handler.sendMessage(msg)
                }
            }
        }
    }

}