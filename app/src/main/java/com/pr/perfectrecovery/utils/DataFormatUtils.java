package com.pr.perfectrecovery.utils;

public class DataFormatUtils {

    public static byte hexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    public static byte[] hexStr2Bytes(String src) {
        int m = 0, n = 0;
        int l = src.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            char ch1 = src.charAt(2 * i);
            switch (ch1) {
                case ('a'):
                    m = 10;
                    break;
                case ('b'):
                    m = 11;
                    break;
                case ('c'):
                    m = 12;
                    break;
                case ('d'):
                    m = 13;
                    break;
                case ('e'):
                    m = 14;
                    break;
                case ('f'):
                    m = 15;
                    break;
                default:
                    m = Integer.parseInt("" + ch1);
                    break;
            }
            char ch2 = src.charAt(2 * i + 1);
            switch (ch2) {
                case ('a'):
                    n = 10;
                    break;
                case ('b'):
                    n = 11;
                    break;
                case ('c'):
                    n = 12;
                    break;
                case ('d'):
                    n = 13;
                    break;
                case ('e'):
                    n = 14;
                    break;
                case ('f'):
                    n = 15;
                    break;
                default:
                    n = Integer.parseInt("" + ch2);
                    break;
            }
            int value = m * 16 + n;
            if (value > 127) {
                value -= 256;
                byte[] intToBytes = DataFormatUtils.intToBytes(value);
                // String intToHexString = DataFormatUtils.intToHexString(value);
                ret[i] = intToBytes[0];
            } else {
                byte[] intToBytes = DataFormatUtils.intToBytes(value);
                ret[i] = intToBytes[0];
            }
        }
        return ret;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
    /*整数转换成单个字节数组*/

    /**
     * 将int数值转换为占两个字节的byte数组，本方法适用于(高位在后，低位在前)的顺序。  和bytesToInt2（）配套使用
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 将int数值转换为占两个字节的byte数组，本方法适用于(高位在后，低位在前)的顺序。  和bytesToInt2（）配套使用
     */
    public static byte[] intToBytes2(int value) {
        byte[] src = new byte[2];
        src[0] = (byte) ((value >> 8) & 0xFF);
        src[1] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8));
        return value;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = (int) (((src[offset] & 0xFF) << 8)
                | (src[offset + 1] & 0xFF));
        return value;
    }

    public static String intToHexString(int i) {
        String str = Integer.toHexString(i);
        /* System.out.println(str);*/
        int n = str.length();
        String[] ss = {"0000", "000", "00", "0", ""};
        str = ss[n] + str;
        return str;
    }

    /*@Test
    public void myTest(){
        System.out.println(intToHexString(1));
        System.out.println(intToHexString(201));
        System.out.println(intToHexString(3333));
        System.out.println(intToHexString(10000));
    }*/
    public static String getCrc16(byte[] arr_buff) {
        int len = arr_buff.length;
        //预置 1 个 16 位的寄存器为十六进制FFFF, 称此寄存器为 CRC寄存器。  
        int crc = 0xFFFF;
        int i, j;
        for (i = 0; i < len; i++) {
            //把第一个 8 位二进制数据 与 16 位的 CRC寄存器的低 8 位相异或, 把结果放于 CRC寄存器  
            crc = ((crc & 0xFF00) | (crc & 0x00FF) ^ (arr_buff[i] & 0xFF));
            for (j = 0; j < 8; j++) {
                //把 CRC 寄存器的内容右移一位( 朝低位)用 0 填补最高位, 并检查右移后的移出位  
                if ((crc & 0x0001) > 0) {
                    //如果移出位为 1, CRC寄存器与多项式A001进行异或  
                    crc = crc >> 1;
                    crc = crc ^ 0xA001;
                } else
                    //如果移出位为 0,再次右移一位  
                    crc = crc >> 1;
            }
        }
        String str = Integer.toHexString(crc);
        if (str.length() == 3) {
            str = "0" + str;
        }
        /*    System.out.println(str);*/
        String str1 = str.substring(0, 2);
        String str2 = str.substring(2);
        return str2 + str1;
    }

    /*Modbus读取寄存器*/
    public static String modbusReadRegister(int slaveId, int start, int readLenth) {
        String id = Integer.toHexString(slaveId);
        if (id.length() == 1) {
            id = "0" + id;
        }
        String code = id + "03" + intToHexString(start) + intToHexString(readLenth);
        byte[] hexStr2Bytes = hexStr2Bytes(code);
        String crc16 = getCrc16(hexStr2Bytes);
        return code + crc16;
    }

    /*modbus写单个寄存器*/
    public static String modbusWriteSingleRegister(int slaveId, int reg, int value) {
        String id = Integer.toHexString(slaveId);
        if (id.length() == 1) {
            id = "0" + id;
        }
        String code = id + "06" + intToHexString(reg) + intToHexString(value);
        byte[] hexStr2Bytes = hexStr2Bytes(code);
        String crc16 = getCrc16(hexStr2Bytes);
        return code + crc16;
    }

    public static String modbusTCPWriteSingleRegister(int slaveId, int reg, int value) {
        String id = Integer.toHexString(slaveId);
        if (id.length() == 1) {
            id = "0" + id;
        }
        String code = "000100000006" + id + "06" + intToHexString(reg) + intToHexString(value);
        byte[] hexStr2Bytes = hexStr2Bytes(code);
        return code;
    }

    public static byte[] float2byte(float f) {

        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);

        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }

        // 翻转数组
        int len = b.length;
        // 建立一个与源数组元素类型相同的数组
        byte[] dest = new byte[len];
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len);
        byte temp;
        // 将顺位第i个与倒数第i个交换
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }

        return dest;

    }


    public static float hexStrToFloat_1234(String hex) {// 解析4个字节中的数据，按照IEEE754的标准
        //String newStr=hex.substring(4)+hex.substring(0,4);
        String newStr = hex;
        // String s = Float.toString(DataFormatUtils.bytesToFloat(aByte));
        // System.out.println(newStr);
        byte[] bytes = DataFormatUtils.hexStr2Bytes(newStr);
        int index = 1;
        String s = "";
        if ((bytes[0] & 0xff) >= 128) {
            int i = bytes[0] & 0x7f;
            index = -1;
            s = Integer.toHexString(i);
            //System.out.println(s);
            newStr = s + newStr.substring(2);
        }
        //System.out.println(newStr);
        float result = index * Float.intBitsToFloat(Integer.valueOf(newStr, 16));
        if ((result + "").equals("NaN")) {
            result = 0;
        }
        return result;
    }

    public static float hexStrToFloat_3412(String hex) {// 解析4个字节中的数据，按照IEEE754的标准,倒序
        String newStr = hex.substring(4) + hex.substring(0, 4);
        byte[] bytes = DataFormatUtils.hexStr2Bytes(newStr);
        int index = 1;
        String s = "";
        if ((bytes[0] & 0xff) >= 128) {
            int i = bytes[0] & 0x7f;
            index = -1;
            s = Integer.toHexString(i);
            //System.out.println(s);
            newStr = s + newStr.substring(2);
        }
        return index * Float.intBitsToFloat(Integer.valueOf(newStr, 16));
    }

    public static byte[] floatToBytes(float a) {
        byte[] data = new byte[4];
        if (a == 0) {
            for (int i = 0; i < 4; i++) {
                data[i] = 0x00;
            }
            return data;
        }
        Integer[] intdata = {0, 0, 0, 0};
        a = Math.abs(a);
        // 首先将浮点数转化为二进制浮点数
        float floatpart = a % 1;
        int intpart = (int) (a / 1);

        System.out.println(intpart + " " + floatpart);
        // 将整数部分化为2进制,并转化为string类型
        String intString = "";
        String floatString = "";
        String result = "";
        String subResult = "";
        int zhishu = 0;
        if (intpart == 0) {
            intString += "0";
        }
        while (intpart != 0) {
            intString = intpart % 2 + intString;
            intpart = intpart / 2;
        }
        while (floatpart != 0) {
            floatpart *= 2;
            if (floatpart >= 1) {
                floatString += "1";
                floatpart -= 1;
            } else {
                floatString += "0";
            }

        }

        result = intString + floatString;
        System.out.println(intString + "." + floatString);
        intpart = (int) (a / 1);
        if (intpart > 0) {// 整数部分肯定有1，且以1开头..这样的话，小数点左移
            zhishu = intString.length() - 1;
        } else {// 整数位为0，右移
            for (int i = 0; i < floatString.length(); i++) {
                zhishu--;
                if (floatString.charAt(i) == '1') {
                    break;
                }
            }
            // while(floatString.charAt(index)){}
        }
        // 对指数进行移码操作

        System.out.println("result==" + result + " zhishu==" + zhishu);
        if (zhishu >= 0) {
            subResult = result.substring(intString.length() - zhishu);
        } else {
            subResult = floatString.substring(-zhishu);
        }
        System.out.println("subResult==" + subResult);
        zhishu += 127;
        if (subResult.length() <= 7) {// 若长度

            for (int i = 0; i < 7; i++) {
                if (i < subResult.length()) {
                    intdata[1] = intdata[1] * 2 + subResult.charAt(i) - '0';
                } else {
                    intdata[1] *= 2;
                }

            }

            if (zhishu % 2 == 1) {// 如果质数是奇数，则需要在这个最前面加上一个‘1’
                intdata[1] += 128;
            }
            data[1] = intdata[1].byteValue();
        } else if (subResult.length() <= 15) {// 长度在（7,15）以内
            int i = 0;
            for (i = 0; i < 7; i++) {// 计算0-7位，最后加上第一位
                intdata[1] = intdata[1] * 2 + subResult.charAt(i) - '0';
            }
            if (zhishu % 2 == 1) {// 如果质数是奇数，则需要在这个最前面加上一个‘1’
                intdata[1] += 128;
            }
            data[1] = intdata[1].byteValue();

            for (i = 7; i < 15; i++) {// 计算8-15位
                if (i < subResult.length()) {
                    intdata[2] = intdata[2] * 2 + subResult.charAt(i) - '0';
                } else {
                    intdata[2] *= 2;
                }

            }
            data[2] = intdata[2].byteValue();
        } else {// 长度大于15
            int i = 0;
            for (i = 0; i < 7; i++) {// 计算0-7位，最后加上第一位
                intdata[1] = intdata[1] * 2 + subResult.charAt(i) - '0';
            }
            if (zhishu % 2 == 1) {// 如果质数是奇数，则需要在这个最前面加上一个‘1’
                intdata[1] += 128;
            }
            data[1] = intdata[1].byteValue();

            for (i = 7; i < 15; i++) {// 计算8-15位
                intdata[2] = intdata[2] * 2 + subResult.charAt(i) - '0';
            }
            data[2] = intdata[2].byteValue();

            for (i = 15; i < 23; i++) {// 计算8-15位
                if (i < subResult.length()) {
                    intdata[3] = intdata[3] * 2 + subResult.charAt(i) - '0';
                } else {
                    intdata[3] *= 2;
                }

            }
            data[3] = intdata[3].byteValue();
        }

        intdata[0] = zhishu / 2;
        if (a < 0) {
            intdata[0] += 128;
        }
        data[0] = intdata[0].byteValue();
        byte[] data2 = new byte[4];// 将数据转移，目的是倒换顺序
        for (int i = 0; i < 4; i++) {
            data2[i] = data[3 - i];
        }
        return data2;
    }

    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    public static int byteArrayToInt(byte[] b) {
        byte[] a = new byte[4];
        int i = a.length - 1, j = b.length - 1;
        for (; i >= 0; i--, j--) {//从b的尾部(即int值的低位)开始copy数据
            if (j >= 0)
                a[i] = b[j];
            else
                a[i] = 0;//如果b.length不足4,则将高位补0
        }
        int v0 = (a[0] & 0xff) << 24;//&0xff将byte值无差异转成int,避免Java自动类型提升后,会保留高位的符号位
        int v1 = (a[1] & 0xff) << 16;
        int v2 = (a[2] & 0xff) << 8;
        int v3 = (a[3] & 0xff);
        return v0 + v1 + v2 + v3;
    }


}
