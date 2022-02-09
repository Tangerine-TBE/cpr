package com.pr.perfectrecovery.utils;


public class TestVolatile {
    // public static int DType=0;
    //电量值：  0-100%
    public static int VI_Value = 0;
    //距离值：  30-150
    public static int L_Value = 0;
    //气压值：  0-2000ml
    public static int QY_Value = 0;
    //蓝牙连接状态：   0-断开 1-连接
    public static int BLS_Value = 0;
    //USB连接状态: 0-断开 1-连接
    public static int ULS_Value = 0;
    //通道打开状态 0-关闭 1-打开
    public static int TOS_Value = 0;
    //连接方式  0-蓝牙 1-连接USB
    public static int LKS_Value = 0;
    //按压位置正确  0-错误  1-正确
    public static int PSR_Value = 0;
    //工作方式：00——休眠   01——工作    02——待机
    public static int WS_Value = 0;
    //按压频率：0-200
    public static int PF_Value = 0;
    //吹气频率：0-200
    public static int CF_Value = 0;
    //按压次数
    public static int PR_SUM = 0;
    //吹气次数
    public static int QY_SUM = 0;
    //按压上升或下降标志位
    public static int low_flag = 0;
    //吹气上升或下降标志位
    public static int top_flag = 0;

    public static int[] L_valueSet;
    public static int[] QY_valueSet;

/*第一部分  上传格式  波特率115200
--------------------------------------------------------------------
 FE 01 XX XX XX XX 01 02 03 04 05 06   07   08   09   0A 0B 0C CH CL
 00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19
---------------------------------------------------------------------
00-01：FE 01<固定值>
02-05：XX XX XX XX <eg：22A00001---软件版本22,类型A,编号00001>
06-08:距离数据<间隔30ms>
09-11:气压数据<间隔30ms>

12:按压频率(上一个按压或弹回持续时间：600/持续时间,2s超时,次数/每秒)
13:吹气频率(吹气持续计时<大于80时>，100代表10s)
14:模型状态0
--------------------------------------------------------------------
bit0:蓝牙状态<1-连接 0-断开>
bit1: USB状态<1-连接 0-断开> (第一步版本硬件暂不支持默认0)
bit2:通道状态<1-打开 0-断开> (低电平有效)    `
bit3:位置状态<1-有效 0-无效> (低电平有效)
bit4:按压不足<1-有效 0-无效> (阈值暂定：120)
bit5:按压过深<1-有效 0-无效> (阈值暂定：60)
bit6:气压不足<1-有效 0-无效> (阈值暂定：400)方便调试先定200,对应数值为50
bit7:气压过大<1-有效 0-无效> (阈值暂定：600)
--------------------------------------------------------------------
15:电量数据<3.0V--0%,>3.6V--100%>
16:模型状态1
--------------------------------------------------------------------
bit0:按压变化<1-->0:完成一次按压，按压回弹时变化>
bit1:吹气变化<1-->0:完成一次吹气，吹气回弹时变化>
bit2:0
bit3:0
bit4:0
bit5:0
bit6:0
bit7:0
--------------------------------------------------------------------
   17:预留
   18:CRC<前面18个数据进行CRC16_Modbus,高8位>
   19:CRC<前面18个数据进行CRC16_Modbus,低8位>

第二部分:仅限USB端通信 波特率115200 ASCII
<1>设置名称
WMFS-XXXXXX   <eg：A00001---类型A,编号00001，蓝牙显示为WMFS-A00001>
成功返回：WMFS-XXXXXX
要求：个数要对上,以WMSF-开始
<2>设置阈值*/

    public static void Anal_Data() {
        String data = "FE01001B1BE5B9162696030000010000000085FE";
        //System.out.print(DataFormatUtils.getCrc16(DataFormatUtils.hexStr2Bytes(data)));
        if (data != null && data.length() == 40) {
            //模型状态需先判断
            int state = DataFormatUtils.byteArrayToInt(DataFormatUtils.hexStr2Bytes("00" + data.substring(28, 30)));
            if ((state & 1) == 1) {
                BLS_Value = 1;
            } else {
                BLS_Value = 0;
            }
            ;
            if ((state & 2) == 2) {
                ULS_Value = 1;
            } else {
                ULS_Value = 0;
            }
            ;
            if ((state & 4) == 4) {
                TOS_Value = 1;
            } else {
                TOS_Value = 0;
            }

            if ((state & 8) == 8) {
                LKS_Value = 1;
            } else {
                LKS_Value = 0;
            }
            ;
            if ((state & 16) == 16) {
                PSR_Value = 1;
            } else {
                PSR_Value = 0;
            }
            ;
            //按压距离
            int L_d1 = DataFormatUtils.byteArrayToInt(DataFormatUtils.hexStr2Bytes("00" + data.substring(12, 14)));
            int L_d2 = DataFormatUtils.byteArrayToInt(DataFormatUtils.hexStr2Bytes("00" + data.substring(14, 16)));
            int L_d3 = DataFormatUtils.byteArrayToInt(DataFormatUtils.hexStr2Bytes("00" + data.substring(16, 18)));
            L_Value = selectValue_P(L_d1, L_d2, L_d3);
            //吹气数据
            int QY_d1 = DataFormatUtils.byteArrayToInt(DataFormatUtils.hexStr2Bytes("00" + data.substring(18, 20)));
            int QY_d2 = DataFormatUtils.byteArrayToInt(DataFormatUtils.hexStr2Bytes("00" + data.substring(20, 22)));
            int QY_d3 = DataFormatUtils.byteArrayToInt(DataFormatUtils.hexStr2Bytes("00" + data.substring(22, 24)));
            //不做气压值的算法处理
            QY_Value = selectValue_QY(QY_d1, QY_d2, QY_d3);
            //频率
            //PF_Value=DataFormatUtils.byteArrayToInt( DataFormatUtils.hexStr2Bytes("00" + data.substring(24, 26)));
            // CF_Value=DataFormatUtils.byteArrayToInt( DataFormatUtils.hexStr2Bytes("00" + data.substring(26, 28)));
            VI_Value = DataFormatUtils.byteArrayToInt(DataFormatUtils.hexStr2Bytes("00" + data.substring(30, 32)));
        }
        System.out.println("电量值：" + VI_Value);
        System.out.println("距离值：" + L_Value);
        System.out.println("气压值：" + QY_Value);
        System.out.println("蓝牙连接值：" + BLS_Value);
        System.out.println("USB连接值：" + ULS_Value);
        System.out.println("通道打开状态值：" + TOS_Value);
        System.out.println("连接方式值：" + LKS_Value);
        System.out.println("按压位置正确值：" + PSR_Value);
        System.out.println("工作方式值：" + WS_Value);
        System.out.println("按压频率值：" + PF_Value);
        System.out.println("吹气频率值：" + CF_Value);

    }

    public static long preTimePress = 0;

    /*
     * 根据按压三次相邻的距离值找到有效值。
     * */
    public static int selectValue_P(int L_d1, int L_d2, int L_d3) {
        int value = 0;
        // int low_flag=0;
        if (L_d1 >= L_d2) {
            if (L_d2 >= L_d3) {
                value = L_d3;
                low_flag = 0;
            } else {
                value = L_d2;
                // preTimePress = System.currentTimeMillis();    //获取开始时间
                low_flag = 1;
                PR_SUM++;
                Err_PrTotal(value);
                long changTimePress = System.currentTimeMillis();
                if (PR_SUM > 1) {
                    long time = changTimePress - preTimePress;
                    PF_Value = (int) (60000 / time);
                }
                preTimePress = changTimePress;
            }
        } else if (L_d2 <= L_d3) {
            if (low_flag == 0) {
                low_flag = 1;
                PR_SUM++;
                Err_PrTotal(value);
                long changTimePress = System.currentTimeMillis();
                if (PR_SUM > 1) {
                    long time = changTimePress - preTimePress;
                    PF_Value = (int) (60000 / time);
                }
                preTimePress = changTimePress;
            }
            value = L_d3;
        } else {
            value = L_d2;
        }
        return value;
    }

    //按压错误-按压不足
    public static int ERR_PR_LOW = 0;
    //按压错误-按压过大
    public static int ERR_PR_HIGH = 0;
    //按压错误-按压位置错误
    public static int ERR_PR_POSI = 0;

    public static void Err_PrTotal(int l) {
        if (PSR_Value == 0) {
            ERR_PR_POSI++;
        } else {
            if (l < 120) {
                ERR_PR_HIGH++;
            } else if (l > 130) {
                ERR_PR_LOW++;
            }
        }
    }

    //吹气错误-气压不足
    public static int ERR_QY_LOW = 0;
    //吹气错误-气压过大
    public static int ERR_QY_HIGH = 0;
    //吹气错误-气压进胃
    public static int ERR_QY_DEAD = 0;
    //吹气错误-气道未打开错误
    public static int ERR_QY_CLOSE = 0;

    public static void ERR_QyTotal(int value) {
        if (TOS_Value == 0) {
            ERR_PR_POSI++;
        } else {
            if (value > 0 && value < 40) {
                ERR_QY_LOW++;
            } else if (value > 80 && value <= 120) {
                ERR_QY_HIGH++;
            } else if (value > 120) {
                ERR_QY_DEAD++;
            }
        }
    }

    public static long preTimeQY = 0;

    /*
     * 根据吹气三次相邻的气压值找到有效值。
     * */
    public static int selectValue_QY(int QY_d1, int QY_d2, int QY_d3) {
        int value = 0;
        int top_flag = 0;
        if (QY_d1 <= QY_d2) {
            if (QY_d2 <= QY_d3) {
                top_flag = 0;
                value = QY_d3;
            } else {
                top_flag = 1;
                value = QY_d2;
                QY_SUM++;
                ERR_QyTotal(value);
                long changTimePress = System.currentTimeMillis();
                if (QY_SUM > 1) {
                    long time = changTimePress - preTimeQY;
                    CF_Value = (int) (60000 / time);
                }
                preTimeQY = changTimePress;
            }
        } else {
            if (QY_d2 <= QY_d3) {
                if (top_flag == 0) {
                    top_flag = 1;
                    QY_SUM++;
                    ERR_QyTotal(QY_d3);
                    long changTimePress = System.currentTimeMillis();
                    if (QY_SUM > 1) {
                        long time = changTimePress - preTimeQY;
                        CF_Value = (int) (60000 / time);
                    }
                    preTimeQY = changTimePress;
                }
                value = QY_d3;
            } else {
                value = QY_d2;
            }
        }
        return value;
    }

    public static void main(String[] args) {
        int a = selectValue_P(15, 180, 160);
        System.out.println(a);
    }
}
