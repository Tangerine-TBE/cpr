package com.pr.perfectrecovery.utils

import android.util.Log
import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.livedata.StatusLiveData
import kotlin.math.abs

class DataVolatile {
    //电量值：  0-100%
    private var VI_Value = 0

    //距离值：  30-150
    private var L_Value = 0

    //气压值：  0-2000ml
    private var QY_Value = 0

    //蓝牙连接状态：   0-断开 1-连接
    private var BLS_Value = 0

    //USB连接状态: 0-断开 1-连接
    private var ULS_Value = 0

    //通道打开状态 0-关闭 1-打开
    private var TOS_Value = 0

    //连接方式  0-蓝牙 1-连接USB
    private var LKS_Value = 0

    //按压位置正确  0-错误  1-正确
    private var PSR_Value = 0

    //工作方式：00——休眠   01——工作    02——待机
    private var WS_Value = 0

    //按压频率：0-200
    private var PF_Value = 0

    //吹气频率：0-200
    private var CF_Value = 0

    //按压次数
    private var PR_SUM = 0

    //吹气次数
    private var QY_SUM = 0

    //吹气上升或下降标志位
    private var top_flag = 0

    //按压上升或下降标志位
    private var low_flag = 0

    private var Qliang = 0

    private val dataDTO = BaseDataDTO()

    //是否开始数据传输
    private var isStart = false

    private var L_valueSet = mutableListOf<Int>()
    private var QY_valueSet = mutableListOf<Int>()
    private var QY_valueSet2 = mutableListOf<Int>()
    private var pt_valueSet = mutableListOf<Int>()
    private var deviceMAC: String? = null

    /**
     * array 数据列表
     * isClear 清除数据集合
     */
    fun max(isClear: Boolean): Int {
        var maximum = 0
        for (i in QY_valueSet.indices) {
            if (maximum < QY_valueSet[i]) {
                maximum = QY_valueSet[i]
            }
        }
        if (isClear) {
            QY_valueSet.clear()
        }
        return maximum
    }

    /**
     * 获取吹气值和
     */
    fun qyValue(): Int {
        var sum = 0
        for (i in QY_valueSet2.indices) {
            sum += i
        }
        QY_valueSet2.clear()
        return sum
    }

    val mapObject = mutableMapOf<String, BaseDataDTO>()

    /**
     * 解析蓝发送的数据
     *
     * @param data
     */
    @Synchronized
    fun parseString(data: String?): BaseDataDTO {
        //System.out.print(DataFormatUtils.getCrc16(DataFormatUtils.hexStr2Bytes(data)));
        if (data != null && data.length == 40) {
            deviceMAC = "001b${data.substring(24, 28) + data.substring(32, 36)}"
            //模型状态需先判断
            val state = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        28,
                        30
                    )
                )
            )
            if (state and 1 == 1) {
                BLS_Value = 1
            } else {
                BLS_Value = 0
            }
            if (state and 2 == 2) {
                ULS_Value = 1
            } else {
                ULS_Value = 0
            }
            if (state and 4 == 4) {
                TOS_Value = 1
            } else {
                TOS_Value = 0
            }
            if (state and 8 == 8) {
                LKS_Value = 1
            } else {
                LKS_Value = 0
            }
//            if (state and 16 == 16) {
//                PSR_Value = 1
//            } else {
//                PSR_Value = 0
//            }
            if (state and 8 == 8) {
                PSR_Value = 1
            } else {
                PSR_Value = 0
            }
            //按压距离
            val L_d1 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        12,
                        14
                    )
                )
            )
            val L_d2 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        14,
                        16
                    )
                )
            )
            val L_d3 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        16,
                        18
                    )
                )
            )
            L_Value = selectValue_P(L_d1, L_d2, L_d3)
            //清空频率
            pt(L_Value)
            //吹气数据
            val QY_d1 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        18,
                        20
                    )
                )
            )
            val QY_d2 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        20,
                        22
                    )
                )
            )
            val QY_d3 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        22,
                        24
                    )
                )
            )

            //不做气压值的算法处理
            QY_Value = selectValue_QY(QY_d1, QY_d2, QY_d3)

            //频率
            // var pfvalue=DataFormatUtils.byteArrayToInt( DataFormatUtils.hexStr2Bytes("00" + data.substring(24, 26)));
            // Log.e("TAG9", "按压频率：$pfvalue")
            // CF_Value=DataFormatUtils.byteArrayToInt( DataFormatUtils.hexStr2Bytes("00" + data.substring(26, 28)));
            VI_Value = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        30,
                        32
                    )
                )
            )
        }
        val stringBuffer = StringBuffer()
        stringBuffer.append("电量值：").append(VI_Value)
        stringBuffer.append("距离值：").append(L_Value)
        stringBuffer.append("气压值：").append(QY_Value)
        stringBuffer.append("蓝牙连接值：").append(BLS_Value)
        stringBuffer.append("USB连接值：").append(ULS_Value)
        stringBuffer.append("通道打开状态值：").append(TOS_Value)
        stringBuffer.append("连接方式值：").append(LKS_Value)
        stringBuffer.append("按压位置正确值：").append(PSR_Value)
        stringBuffer.append("工作方式值：").append(WS_Value)
        stringBuffer.append("按压频率值：").append(PF_Value)
        stringBuffer.append("吹气频率值：").append(CF_Value)

        val dataDTO = BaseDataDTO()
        dataDTO.mac = deviceMAC.toString()
        dataDTO.prSum = PR_SUM
        dataDTO.qySum = QY_SUM
        dataDTO.electricity = VI_Value
        dataDTO.distance = L_Value
        dataDTO.bpValue = QY_Value
        dataDTO.blsType = BLS_Value
        dataDTO.usbConnectType = ULS_Value
        dataDTO.aisleType = TOS_Value
        dataDTO.connectType = LKS_Value
        dataDTO.psrType = PSR_Value
        dataDTO.workType = WS_Value
        dataDTO.cf = CF_Value
        dataDTO.pf = PF_Value
        dataDTO.isStart = isStart
        dataDTO.err_pr_high = ERR_PR_HIGH
        dataDTO.err_pr_low = ERR_PR_LOW
        dataDTO.err_qy_close = ERR_QY_CLOSE
        dataDTO.err_pr_posi = ERR_PR_POSI
        dataDTO.err_qy_dead = ERR_QY_DEAD
        dataDTO.err_qy_high = ERR_QY_HIGH
        dataDTO.err_qy_low = ERR_QY_LOW
        dataDTO.err_pr_unback = ERR_PR_UNBACK
        dataDTO.pr_depth_sum = PR_DEPTH_SUM  //按压深度总和(mm)
        dataDTO.pr_time_sum = PR_TIME_SUM    // 按压时间总和（ms）
        dataDTO.qy_volume_sum = QY_VOLUME_SUM  //吹气量总和
        dataDTO.qy_time_sum = QY_TIME_SUM     //吹气时间总和
        dataDTO.pr_seqright_total = PR_SEQRIGHT_TOTAL //按压频率正常的次数
        dataDTO.qy_serright_total = QY_SERRIGHT_TOTAL //吹气频率正确的次数
//        deviceMAC?.let { mapObject.put(it, dataDTO) }
        dataDTO.preDistance = preDistance.toInt()
        if (QY_SUM != qy) {
            qy = QY_SUM
            QY_MAX_VOLUME_SUM += max(true)
        }
        dataDTO.QY_valueSet = QY_valueSet
        dataDTO.qy_max_volume_sum = QY_MAX_VOLUME_SUM
        dataDTO.PR_HIGH_VALUE = PR_HIGH_VALUE
        dataDTO.PR_LOW_VALUE = PR_LOW_VALUE
        return dataDTO
    }

    private var qy = 0
    fun dataClear() {
        isStart = false
        //电量值：  0-100%
        VI_Value = 0
        //距离值：  30-150
        L_Value = 0
        //气压值：  0-2000ml
        QY_Value = 0
        //蓝牙连接状态：   0-断开 1-连接
        BLS_Value = 0
        //按压频率：0-200
        PF_Value = 0
        //吹气频率：0-200
        CF_Value = 0
        //按压次数
        PR_SUM = 0
        //吹气次数
        QY_SUM = 0
        ERR_PR_UNBACK = 0
        ERR_PR_HIGH = 0
        ERR_PR_LOW = 0
        ERR_PR_POSI = 0
        ERR_QY_CLOSE = 0
        ERR_QY_DEAD = 0
        ERR_QY_HIGH = 0
        ERR_QY_LOW = 0
        PR_DEPTH_SUM = 0  //按压深度总和(mm)
        PR_TIME_SUM = 0    // 按压时间总和（ms）
        QY_VOLUME_SUM = 0  //吹气量总和
        QY_TIME_SUM = 0     //吹气时间总和
        QY_MAX_VOLUME_SUM = 0//吹气每次最大值总和
        PR_SEQRIGHT_TOTAL = 0; //按压频率正常的次数
        QY_SERRIGHT_TOTAL = 0; //吹气频率正确的次数
        L_valueSet.clear()
        QY_valueSet.clear()
        QY_valueSet2.clear()
        pt_valueSet.clear()
    }
    /*
* 获取初始位置，每次连接成功后调用一次初始化方法
* */

    var preDistance: Long = 180

    var preDistanceMap = mutableMapOf<String, Long>()

    @Synchronized
    fun initPreDistance(data: String?, macAddress: String) {
        // long value=180;
        if (data != null && data.length == 40) {
            //按压距离
            val L_d1 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        12,
                        14
                    )
                )
            )
            val L_d2 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        14,
                        16
                    )
                )
            )
            val L_d3 = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        16,
                        18
                    )
                )
            )
            preDistance = ((L_d1 + L_d2 + L_d3) / 3).toLong();
            //preDistance=150
            // preDistance=L_d1.toLong();
            macAddress.let {
                preDistanceMap[macAddress] = preDistance
            }
        }
    }

    private var UNBACK_FLAG = 0
    private var ERR_FLAG = 0
    private var PR_DOTTIMSE_NUMBER = 0
    private var PR_RUN_FLAG = 0
    private var MIN_FLAG = 0;

    private var PR_DEPTH_SUM = 0  //按压深度总和(mm)
    private var PR_TIME_SUM = 0    // 按压时间总和（ms）
    private var QY_VOLUME_SUM = 0  //吹气量总和
    private var QY_MAX_VOLUME_SUM = 0 //每次吹气峰值总和
    private var QY_TIME_SUM = 0     //吹气时间总和
    private var PR_SEQRIGHT_TOTAL = 0; //按压频率正常的次数
    private var QY_SERRIGHT_TOTAL = 0; //吹气频率正确的次数

    /*
    * 根据按压三次相邻的距离值找到有效值。
    * */
    private fun selectValue_P(L_d1: Int, L_d2: Int, L_d3: Int): Int {
        var value = 0
        var index = 0
        Log.e("TAG8", "$L_d1  $L_d2  $L_d3")
        if (PR_RUN_FLAG == 1) {
            L_valueSet.add(index, L_d1)
            L_valueSet.add(index + 1, L_d2)
            L_valueSet.add(index + 2, L_d3)
            index += 3
        }
        if (abs(preDistance - L_d1) < 10 && abs(preDistance - L_d2) < 10 && abs(preDistance - L_d3) < 10
        ) {
            low_flag = 0
            UNBACK_FLAG = 0
            return preDistance.toInt()
        }
        // int low_flag=0;
        if (L_d1 >= L_d2) {
            //  PR_DOTTIMSE_NUMBER+=3
            if (L_d2 >= L_d3) {
                if (L_d3 <= 30) {
                    return L_d3.toInt()
                }
                value = L_d3
                low_flag = 0
                if (UNBACK_FLAG == 1) {
                    ERR_PR_UNBACK++
                    UNBACK_FLAG = 0
                    Log.e("TAG7", "未回弹")
                    ERR_FLAG = 1;
                }
            } else {
                if (low_flag == 0) {//防止在上升到最高点出现抖动导致次数误增加
                    low_flag = 1
                    PR_SUM++
                    //  Log.e("TAG5", "$PR_SUM")
                    if (ERR_FLAG == 0) {
                        Err_PrTotal(L_d2)
                    } else {
                        ERR_FLAG = 0;
                    }
                    PR_RUN_FLAG = 1;
                    //  Log.e("TAG8", "距离点数$PR_DOTTIMSE_NUMBER")
                    if (PR_SUM > 1) {
                        if (L_valueSet.size > 30) {
                            PF_Value = 0;
                            L_valueSet.clear()
                        } else {
                            if (MIN_FLAG == 1) {
                                PR_DOTTIMSE_NUMBER = L_valueSet.size
                            } else if (MIN_FLAG == 2) {
                                PR_DOTTIMSE_NUMBER = L_valueSet.size + 2
                            }
                            for (item in L_valueSet) {
                                Log.e("TAG8", "距离数 ${item}")
                            }
                            Log.e("TAG8", "距离点数$PR_DOTTIMSE_NUMBER")
                            PR_TIME_SUM += (PR_DOTTIMSE_NUMBER * 40).toInt()
                            PF_Value = (PF_Value + (60000 / (PR_DOTTIMSE_NUMBER * 40)).toInt()) / 2
                            if (PF_Value in 100..120) {
                                PR_SEQRIGHT_TOTAL++
                            }
                            PR_DOTTIMSE_NUMBER = 0;
                            index = 0
                            L_valueSet.clear()
                        }
                        PR_DEPTH_SUM += (preDistance - L_d2 + 5).toInt()

                    }
                    // Log.e("TAG4", "$L_d2")
                    /*val changTimePress = System.currentTimeMillis()
                    if (PR_SUM > 1) {
                        val time = changTimePress - preTimePress+40
                        PF_Value = (PF_Value.toInt()+(60000 / time).toInt())/2
                        if (PF_Value > 150) {
                            PF_Value = 150;
                        } else if (PF_Value < 60) {
                            PF_Value = 60;
                        }
                      Log.e("TAG6", "PF值：$PF_Value")
                    }
                    preTimePress = changTimePress*/
                }
                MIN_FLAG = 1
                value = L_d2
            }
        } else if (L_d2 < L_d3) {
            // PR_DOTTIMSE_NUMBER+=3
            if (low_flag == 0) {
                low_flag = 1
                PR_SUM++

                // Log.e("TAG5", "$PR_SUM")
                if (ERR_FLAG == 0) {
                    Err_PrTotal(L_d1)
                } else {
                    ERR_FLAG = 0;
                }
                PR_RUN_FLAG = 1;
                //Log.e("TAG8", "距离点数$PR_DOTTIMSE_NUMBER")
                if (PR_SUM > 1) {
                    if (L_valueSet.size > 30) {
                        PF_Value = 0;
                        L_valueSet.clear()
                    } else {
                        if (MIN_FLAG == 1) {
                            PR_DOTTIMSE_NUMBER = L_valueSet.size - 2
                        } else if (MIN_FLAG == 2) {
                            PR_DOTTIMSE_NUMBER = L_valueSet.size
                        }
                        for (item in L_valueSet) {
                            Log.e("TAG8", "距离数 ${item}")
                        }
                        Log.e("TAG8", "距离点数$PR_DOTTIMSE_NUMBER")
                        PR_TIME_SUM += (PR_DOTTIMSE_NUMBER * 40).toInt()
                        PF_Value = (PF_Value + (60000 / (PR_DOTTIMSE_NUMBER * 40)).toInt()) / 2
                        if (PF_Value in 100..120) {
                            PR_SEQRIGHT_TOTAL++
                        }
                        PR_DOTTIMSE_NUMBER = 0;
                        index = 0
                        L_valueSet.clear()
                    }
                    PR_DEPTH_SUM += (preDistance - L_d1 + 5).toInt()
                }
                MIN_FLAG = 2
                // Log.e("TAG6", "$L_d1")
                /* val changTimePress = System.currentTimeMillis()
                 if (PR_SUM > 1) {
                     PR_RUN_FLAG=1;
                     val time = changTimePress - preTimePress+70
                     PF_Value = (PF_Value.toInt()+(60000 / time).toInt())/2
                     if (PF_Value > 150) {
                         PF_Value = 150;
                     } else if (PF_Value < 60) {
                         PF_Value = 60;
                     }
                      Log.e("TAG6", "PF值：$PF_Value")
                 }
                 preTimePress = changTimePress*/

                value = L_d1
            } else {
                // PR_DOTTIMSE_NUMBER+=3
                Log.e("TAG7", "初始位置$preDistance")
                if (abs(preDistance - L_d3) < 12) {
                    UNBACK_FLAG = 0
                    low_flag = 0
                    //Log.e("TAG7", "初始位置$preDistance")
                    Log.e("TAG7", "回到初始位置，复位$L_d3")
                } else {
                    UNBACK_FLAG = 1
                    Log.e("TAG7", "未回弹$L_d3")
                }
                value = L_d3
            }

        } else {
            // PR_DOTTIMSE_NUMBER+=3
            Log.e("TAG7", "初始位置$preDistance")
            if (abs(preDistance - L_d2) < 12) {
                UNBACK_FLAG = 0
                Log.e("TAG7", "回到初始位置，复位未回弹$L_d2")
                low_flag = 0
            } else {
                UNBACK_FLAG = 1
                Log.e("TAG7", "未回弹$L_d2")
            }
            value = L_d2
        }

        return value
    }

    //按压错误-按压未回弹
    private var ERR_PR_UNBACK = 0

    //按压错误-按压不足
    private var ERR_PR_LOW = 0

    //按压错误-按压过大
    private var ERR_PR_HIGH = 0

    //按压错误-按压位置错误
    private var ERR_PR_POSI = 0

    /**
     * 初始化按压区间值
     */
    var PR_LOW_VALUE = 45
    var PR_HIGH_VALUE = 60

    private fun Err_PrTotal(l: Int) {

        if (PSR_Value == 0) {
            ERR_PR_POSI++
        } else {
            var value = abs(preDistance - l)
            if (value < PR_LOW_VALUE) {
                ERR_PR_LOW++
                //   Log.e("TAG1", "按压不足")
                //    Log.e("TAG1", "$value")
            } else if (value > PR_HIGH_VALUE) {
                ERR_PR_HIGH++
                //    Log.e("TAG2", "按压过深")
                //   Log.e("TAG2", "$value")
            }
            // Log.e("TAG3", "$value")
        }
    }

    //吹气错误-气压不足
    private var ERR_QY_LOW = 0

    //吹气错误-气压过大
    private var ERR_QY_HIGH = 0

    //吹气错误-气压进胃
    private var ERR_QY_DEAD = 0

    //吹气错误-气道未打开错误
    private var ERR_QY_CLOSE = 0

    private fun ERR_QyTotal(value: Int) {
        if (TOS_Value == 0) {
            ERR_QY_CLOSE++
        } else {
            if (value in 1..39) {
                ERR_QY_LOW++
            } else if (value in 81..120) {
                ERR_QY_HIGH++
            } else if (value > 120) {
                ERR_QY_DEAD++
            }
        }
    }

    private var preTimeQY: Long = 0

    /*
 * 根据吹气三次相邻的气压值找到有效值。
 * */
    private fun selectValue_QY(QY_d1: Int, QY_d2: Int, QY_d3: Int): Int {
        var value = 0
        if (QY_d1 > 0 || QY_d2 > 0 || QY_d3 > 0) {
            top_flag = 1
            Qliang = (QY_d1 + QY_d2 + QY_d3) * 30
            QY_VOLUME_SUM += Qliang
        }
        if (QY_d1 == 0 && QY_d2 == 0 && QY_d3 == 0) {
            if (top_flag == 1) {
                ERR_QyTotal(max(false))//每次筛选最大吹气值，去做错误次数的判断
                val changTimePress = System.currentTimeMillis()
                ++QY_SUM
                top_flag = 0
                Qliang = 0
                if (QY_SUM > 1) {
                    val time = changTimePress - preTimeQY
                    QY_TIME_SUM += time.toInt()
                    CF_Value = (60000 / time).toInt()
                    if (CF_Value in 6..8) {
                        QY_SERRIGHT_TOTAL++
                    }
                }
                preTimeQY = changTimePress
                Log.e("TAG10", "吹气的时间累加和$QY_TIME_SUM")
            }

        }
        value = if (QY_d1 <= QY_d2) {
            if (QY_d2 <= QY_d3) {
                QY_d3
            } else {
                //  top_flag=1;
                QY_d2
            }
        } else {
            if (QY_d2 >= QY_d3) {
                QY_d3
            } else {
                QY_d2
            }
        }
        if (value > 0) {
            QY_valueSet2.add(value)
            QY_valueSet.add(value)
        }
        return value
    }

    //判断按压是否停止
    private val count = 20
    private fun pt(p: Int): Boolean {
        if (p > (preDistance - 5)) {
            if (pt_valueSet.size == count) pt_valueSet.removeFirst()
            pt_valueSet.add(p)
            if (pt_valueSet.size == count) {
                pt_valueSet.clear()
                PF_Value = 0
                return true
            }
        } else {
            pt_valueSet.clear()
        }
        return false
    }
}