package com.pr.perfectrecovery.utils

import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.utils.TestVolatile.top_flag
import kotlin.math.abs

object DataVolatile {
    //电量值：  0-100%
    var VI_Value = 0

    //距离值：  30-150
    var L_Value = 0

    //气压值：  0-2000ml
    var QY_Value = 0

    //蓝牙连接状态：   0-断开 1-连接
    var BLS_Value = 0

    //USB连接状态: 0-断开 1-连接
    var ULS_Value = 0

    //通道打开状态 0-关闭 1-打开
    var TOS_Value = 0

    //连接方式  0-蓝牙 1-连接USB
    var LKS_Value = 0

    //按压位置正确  0-错误  1-正确
    var PSR_Value = 0

    //工作方式：00——休眠   01——工作    02——待机
    var WS_Value = 0

    //按压频率：0-200
    var PF_Value = 0

    //吹气频率：0-200
    var CF_Value = 0

    //按压次数
    var PR_SUM = 0

    //吹气次数
    var QY_SUM = 0

    //按压上升或下降标志位
    var low_flag = 0

    val dataDTO = BaseDataDTO()

    var Qliang = 0

    var L_valueSet = intArrayOf(1)
    var QY_valueSet = mutableListOf<Int>()
    var pt_valueSet = mutableListOf<Int>()

    fun max(array: List<Int>): Int {
        var maximum = Int.MIN_VALUE
        for (i in array.indices) {
            if (maximum < array[i]) {
                maximum = array[i]
            }
        }
        QY_valueSet.clear()
        return maximum
    }

    /**
     * 获取吹气值和
     */
    fun qyValue(array: List<Int>): Int {
        var sum = 0
        for (i in array.indices) {
            sum += i
        }

        QY_valueSet.clear()
        return sum
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val data = "fe06040a0b01052d5303030405010723090ab261" //1、先将接收到的数据转调用工具类的方法换成字符串
        //System.out.print(DataFormatUtils.getCrc16(DataFormatUtils.hexStr2Bytes(data)));
        parseString(data)
    }

    /**
     * 解析蓝发送的数据
     *
     * @param data
     */
    fun parseString(data: String?): BaseDataDTO {
        //System.out.print(DataFormatUtils.getCrc16(DataFormatUtils.hexStr2Bytes(data)));
        if (data != null && data.length == 40) {
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
            if (state and 16 == 16) {
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
            //PF_Value=DataFormatUtils.byteArrayToInt( DataFormatUtils.hexStr2Bytes("00" + data.substring(24, 26)));
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
        dataDTO.ERR_PR_HIGH = ERR_PR_HIGH
        dataDTO.ERR_PR_LOW = ERR_PR_LOW
        dataDTO.ERR_PR_POSI = ERR_PR_POSI
        dataDTO.ERR_QY_CLOSE = ERR_QY_CLOSE
        dataDTO.ERR_QY_DEAD = ERR_QY_DEAD
        dataDTO.ERR_QY_HIGH = ERR_QY_HIGH
        dataDTO.ERR_QY_LOW = ERR_QY_LOW

        return dataDTO
    }

    fun setCF_Value() {
        CF_Value = 0
    }

    var preTimePress: Long = 0

    /*
* 获取初始位置，每次连接成功后调用一次初始化方法
* */
    var preDistance: Long = 180
    fun initPreDistance(data: String?) {
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
            preDistance = ((L_d1 + L_d2 + L_d3) / 3).toLong()
        }
    }

    /*
    * 根据按压三次相邻的距离值找到有效值。
    * */
    fun selectValue_P(L_d1: Int, L_d2: Int, L_d3: Int): Int {
        var value = 0
        if (PR_SUM == 0 && abs(preDistance - L_d1) < 10 && abs(preDistance - L_d2) < 10 && abs(
                preDistance - L_d3
            ) < 10
        ) {
            return preDistance.toInt()
        }
        // int low_flag=0;
        if (L_d1 >= L_d2) {
            if (L_d2 >= L_d3) {
                value = L_d3
                low_flag = 0
            } else {
                value = L_d2
                //preTimePress = System.currentTimeMillis()    //获取开始时间
                low_flag = 1
                PR_SUM++
                Err_PrTotal(value)
                val changTimePress = System.currentTimeMillis()
                if (PR_SUM > 1) {
                    val time = changTimePress - preTimePress
                    PF_Value = (60000 / time).toInt()
                    if(PF_Value>180){
                        PF_Value=180;
                    }else if(PF_Value<60){
                        PF_Value=60;
                    }
                }
                preTimePress = changTimePress
            }
        } else if (L_d2 <= L_d3) {
            if (low_flag == 0) {
                low_flag = 1
                PR_SUM++
                Err_PrTotal(L_d3)
                val changTimePress = System.currentTimeMillis()
                if (PR_SUM > 1) {
                    val time = changTimePress - preTimePress
                    PF_Value = (60000 / time).toInt()
                    if(PF_Value>180){
                        PF_Value=180;
                    }else if(PF_Value<60){
                        PF_Value=60;
                    }
                }
                preTimePress = changTimePress
            }
            value = L_d3
        } else {
            value = L_d2
        }
        return value
    }

    //按压错误-按压不足
    var ERR_PR_LOW = 0

    //按压错误-按压过大
    var ERR_PR_HIGH = 0

    //按压错误-按压位置错误
    var ERR_PR_POSI = 0

    private fun Err_PrTotal(l: Int) {
        if (PSR_Value == 0) {
            ERR_PR_POSI++
        } else {
            if (l < 45) {
                ERR_PR_HIGH++
            } else if (l > 60) {
                ERR_PR_LOW++
            }
        }
    }

    //吹气错误-气压不足
    var ERR_QY_LOW = 0

    //吹气错误-气压过大
    var ERR_QY_HIGH = 0

    //吹气错误-气压进胃
    var ERR_QY_DEAD = 0

    //吹气错误-气道未打开错误
    var ERR_QY_CLOSE = 0

    fun ERR_QyTotal(value: Int) {
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

    var preTimeQY: Long = 0

    /*
 * 根据吹气三次相邻的气压值找到有效值。
 * */
    fun selectValue_QY(QY_d1: Int, QY_d2: Int, QY_d3: Int): Int {
        var value = 0
        if (QY_d1 > 0 || QY_d2 > 0 || QY_d3 > 0) {
            top_flag = 1
            Qliang = (QY_d1 + QY_d2 + QY_d3) * 30
        }
        if (QY_d1 == 0 && QY_d2 == 0 && QY_d3 == 0) {

            if (top_flag == 1) {
                ERR_QyTotal(max(QY_valueSet));//每次筛选最大吹气值，去做错误次数的判断
                val changTimePress = System.currentTimeMillis()
                ++QY_SUM
                top_flag = 0
                Qliang = 0
                if (QY_SUM > 1) {
                    val time = changTimePress - preTimeQY
                    CF_Value = (60000 / time).toInt()
                }
                preTimeQY = changTimePress
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
            QY_valueSet.add(value)
        }
        return value
    }
    //判断按压是否停止
    private const val count = 20
    private fun pt(p: Int): Boolean {
        if (p > 175) {
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

    fun getData(): BaseDataDTO? {
        return dataDTO
    }
}