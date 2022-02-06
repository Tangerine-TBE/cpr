package com.pr.perfectrecovery.utils

import com.pr.perfectrecovery.bean.BaseDataDTO
import com.pr.perfectrecovery.utils.TestVolatile.top_flag

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

    var PT_value = false

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
//            PF_Value = DataFormatUtils.byteArrayToInt(
//                DataFormatUtils.hexStr2Bytes(
//                    "00" + data.substring(
//                        24,
//                        26
//                    )
//                )
//            );
//
//            CF_Value = DataFormatUtils.byteArrayToInt(
//                DataFormatUtils.hexStr2Bytes(
//                    "00" + data.substring(
//                        26,
//                        28
//                    )
//                )
//            );

            //清空频率
//            PT_value = pt(L_Value)

            //模型状态
            val state = DataFormatUtils.byteArrayToInt(
                DataFormatUtils.hexStr2Bytes(
                    "00" + data.substring(
                        28,
                        30
                    )
                )
            )
            BLS_Value = if (state and 1 == 1) {
                1
            } else {
                0
            }
            ULS_Value = if (state and 2 == 2) {
                1
            } else {
                0
            }
            TOS_Value = if (state and 4 == 4) {
                1
            } else {
                0
            }
            LKS_Value = if (state and 8 == 8) {
                1
            } else {
                0
            }
            PSR_Value = if (state and 16 == 16) {
                1
            } else {
                0
            }
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
        dataDTO.pressInterrupt = PT_value
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
        return dataDTO
    }

    fun setCF_Value() {
        CF_Value = 0;
    }

    var preTimePress: Long = 0

    /*根据有效距离值计算按压累加次数。
    * */
    fun cal_PreSum(a: Int): Int {
        var sum = 0
        //  L_valueSet[0]=0;
        if (L_valueSet[0] == 0) {
            L_valueSet[0] = a
            val preTimePress = System.currentTimeMillis() //获取开始时间
        } else {
            if (a >= 170 && a - L_valueSet[0] > 10) {
                sum++
                val changTimePress = System.currentTimeMillis()
                val time = changTimePress - preTimePress
                PF_Value = (60000 / time).toInt()
                if(PF_Value>=180) PF_Value=180
                preTimePress = changTimePress
            }
            L_valueSet[0] = a
        }
        return sum
    }

    fun selectValue_P(L_d1: Int, L_d2: Int, L_d3: Int): Int {
        var value = 0
        // int low_flag=0;
        //防止抖动
        if (L_d1 >= L_d2 && L_d1 - L_d2 >= 5) {
            if (L_d2 >= L_d3) {
                value = L_d3
                low_flag = 0
            } else {
                value = L_d2
                // preTimePress = System.currentTimeMillis();    //获取开始时间
                low_flag = 1
                PR_SUM++
                val changTimePress = System.currentTimeMillis()
                if (PR_SUM > 1) {
                    val time = changTimePress - preTimePress
                    PF_Value = (60000 / time).toInt()
                    if(PF_Value>=180) PF_Value=180
                }
                preTimePress = changTimePress
            }
        } else if (L_d2 <= L_d3 && L_d3 - L_d2 >= 5) {
            if (low_flag == 0) {
                low_flag = 1
                PR_SUM++
                val changTimePress = System.currentTimeMillis()
                if (PR_SUM > 1) {
                    val time = changTimePress - preTimePress
                    PF_Value = (60000 / time).toInt()
                    if(PF_Value>=180) PF_Value=180
                }
                preTimePress = changTimePress
            }
            value = L_d3
        } else {
            value = L_d2
        }
        return value
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
        QY_valueSet.add(value)
        return value
    }

    //判断按压是否停止
    private val count = 20
    private fun pt(p: Int): Boolean {
        if (pt_valueSet.size == count) pt_valueSet.removeFirst()
        pt_valueSet.add(p)
        if (pt_valueSet.size == count) {
            val listArray = ArrayList<Int>()
            pt_valueSet.forEachIndexed { index, i ->
                listArray.add(pt_valueSet[index])
            }
            if (listArray.size == count) {
                listArray.clear()
                PF_Value = 0
            }
            return listArray.size == count
        }
        return false
    }


    fun getData(): BaseDataDTO? {
        return dataDTO
    }
}