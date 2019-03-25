package com.weex.commen_module.callhistory

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.provider.CallLog
import com.blankj.utilcode.util.CloseUtils
import com.google.gson.Gson
import java.util.*

/**
 * Created by xuwx on 2019/1/8.
 */
class CallHistoryMaster {

    companion object {
        var instance: CallHistoryMaster? = null
            get() {
                if (instance == null) {
                    synchronized(CallHistoryMaster::class.java)
                    {
                        if (instance == null) {
                            instance = CallHistoryMaster()
                        }
                    }
                }
                return instance
            }
    }

    @SuppressLint("MissingPermission")
    fun getCallHistoryLimitCount(contentResolver: ContentResolver, count: Int): String {
        var cursor: Cursor
        try {
            cursor = if (count == -1) {
                contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, " date DESC ")
            } else {
                contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, " date DESC limit $count")
            }
            return formatCallHistoryData(parseCallHistory(cursor))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return formatCallHistoryData(listOf())
    }

    @SuppressLint("MissingPermission")
    fun getCallHistoryLimitDate(contentResolver: ContentResolver, date: Date): String {
        var cursor: Cursor
        try {
            cursor = contentResolver.query(CallLog.Calls.CONTENT_URI, null, "date > ${date.time}", null, " date DESC ")
            return formatCallHistoryData(parseCallHistory(cursor))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return formatCallHistoryData(listOf())
    }

    /**
     * 将通话记录数据转换成JSON字符串
     *
     * @param models
     * @return
     */
    private fun formatCallHistoryData(models: List<CallHistoryDataModel>): String {
        val infoMap = LinkedHashMap<String, Map<*, *>>()
        val listMap = LinkedHashMap<String, List<*>>()
        listMap["callHistory"] = models
        infoMap["callHistoryMod"] = listMap

        return Gson().toJson(infoMap)
    }

    private fun parseCallHistory(cursor: Cursor): List<CallHistoryDataModel> {
        val callHistoryDataModelList = ArrayList<CallHistoryDataModel>()
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val record = CallHistoryDataModel(cursor.getString(cursor.getColumnIndex("name")),
                            cursor.getString(cursor.getColumnIndex("number")))
                    record.name = cursor.getString(cursor.getColumnIndex("name"))
                    record.number = cursor.getString(cursor.getColumnIndex("number"))
                    record.time = cursor.getLong(cursor.getColumnIndex("date"))
                    record.date = stampToDate(record.time.toString())
                    record.duration = cursor.getLong(cursor.getColumnIndex("duration"))
                    var recordType = ""
                    val type = cursor.getInt(cursor.getColumnIndex("type"))
                    when (type) {
                        1 -> recordType = "incoming"
                        2 -> recordType = "outgoing"
                        3 -> recordType = "missing"
                        4 -> recordType = "hd"
                        5, 6, 7 -> {
                        }
                        8 -> recordType = "wifi"
                        else -> {
                        }
                    }

                    record.type = recordType
                    callHistoryDataModelList.add(record)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            CloseUtils.closeIO(cursor)
        }
        return callHistoryDataModelList
    }

    data class CallHistoryDataModel(var name: String = "", var number: String = "", var time: Long = 0L, var date: String = "", var duration: Long = 0L, var type: String = "")

    private fun stampToDate(stamp: String): String {
        if (stamp.length == 13) {
            val dateFormat = Date(java.lang.Long.valueOf(stamp)!!)
            return dateFormat.toString()
        }
        return "error"
    }

}