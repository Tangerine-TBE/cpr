package com.pr.perfectrecovery.utils

import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.pr.perfectrecovery.BaseApplication
import java.io.*


object FileUtils {
    var logFilePath: String =
        Environment.getExternalStorageDirectory().path + File.separator.toString() + "cprCrash" + File.separator + BaseApplication.instance()!!.packageName + File.separator.toString() + "crashLog"

    fun saveThrowableMessage(errorMessage: String) {
        if (TextUtils.isEmpty(errorMessage)) {
            return
        }
        val file = File(logFilePath)
        Log.e("TAG", "saveThrowableMessage: $logFilePath")
        if (!file.exists()) {
            val mkdirs = file.mkdirs()
            if (mkdirs) {
                writeStringToFile(errorMessage, file)
            }
        } else {
            writeStringToFile(errorMessage, file)
        }
    }

    private fun writeStringToFile(errorMessage: String, file: File) {
        Thread {
            var outputStream: FileOutputStream? = null
            try {
                val inputStream = ByteArrayInputStream(errorMessage.toByteArray())
                outputStream = FileOutputStream(
                    File(
                        file,
                        System.currentTimeMillis().toString() + ".txt"
                    )
                )
                var len = 0
                val bytes = ByteArray(1024)
                while (inputStream.read(bytes).also { len = it } != -1) {
                    outputStream.write(bytes, 0, len)
                }
                outputStream.flush()
            } catch (e: IOException) {
                Log.e("程序出异常了", "写入本地文件成功：" + file.absolutePath)
                ToastUtils.showShort("CPR日志写入异常")
                e.printStackTrace()
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }
}