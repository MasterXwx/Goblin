package com.weex.commen_module.smshistory

import android.content.ContentResolver
import android.database.Cursor
import android.provider.Telephony
import com.blankj.utilcode.util.CloseUtils
import com.google.gson.Gson
import java.lang.Long
import java.util.*

/**
 * Created by xuwx on 2019/1/8.
 */
class SmsHistoryMaster {

    companion object {
        var instance: SmsHistoryMaster? = null
            get() {
                if (instance == null) {
                    synchronized(SmsHistoryMaster::class.java) {
                        if (instance == null) {
                            instance = SmsHistoryMaster()
                        }
                    }
                }
                return instance
            }
    }

    fun getSmsHistoryLimitCount(contentResolver: ContentResolver, count: Int): String {
        var cursor: Cursor
        try {
            cursor = if (count == -1) {
                contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, " date DESC ")
            } else {
                contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, " date DESC limit $count")
            }
            return formatSmsHistoryData(parseSmsHistory(cursor))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return formatSmsHistoryData(listOf())
    }

    /**
     * 将短信记录数据转换成JSON字符串
     *
     * @param models
     * @return
     */
    private fun formatSmsHistoryData(models: List<SmsHistoryDataModel>): String {
        val infoMap = LinkedHashMap<String, Map<*, *>>()
        val listMap = LinkedHashMap<String, List<*>>()
        listMap["sms"] = models
        infoMap["smsMod"] = listMap
        return Gson().toJson(infoMap)
    }

    /**
     * 获取短信记录
     *
     * @param cursor
     * @return
     */
    private fun parseSmsHistory(cursor: Cursor?): List<SmsHistoryDataModel> {
        val smsDataModelList = ArrayList<SmsHistoryDataModel>()
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val smsDataModel = SmsHistoryDataModel()
                    val time = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                    smsDataModel.time = time
                    smsDataModel.date = stampToDate(time)
                    val number = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                    smsDataModel.number = number
                    val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                    smsDataModel.body = body
                    var type = ""
                    when (Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow("type")))) {
                        1 -> type = "inbox"
                        2 -> type = "sent"
                        3 -> type = "draft"
                        4 -> type = "outbox"
                        5 -> type = "failed"
                        6 -> type = "queued"
                    }
                    smsDataModel.type = type
                    smsDataModelList.add(smsDataModel)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            CloseUtils.closeIO(cursor)
        }
        return smsDataModelList
    }

    data class SmsHistoryDataModel(var time: String = "", var date: String = "", var number: String = "", var body: String = "", var type: String = "")

    private fun stampToDate(stamp: String): String {
        if (stamp.length == 13) {
            val dateFormat = Date(Long.valueOf(stamp)!!)
            return dateFormat.toString()
        }
        return "error"
    }

}