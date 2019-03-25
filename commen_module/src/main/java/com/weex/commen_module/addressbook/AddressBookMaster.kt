package com.weex.commen_module.addressbook

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import com.blankj.utilcode.util.CloseUtils
import java.util.*

/**
 * Created by xuwx on 2019/1/9.
 */
class AddressBookMaster {

    companion object {
        var instance: AddressBookMaster? = null
            get() {
                if (instance == null) {
                    synchronized(AddressBookMaster::class.java)
                    {
                        if (instance == null) {
                            instance = AddressBookMaster()
                        }
                    }
                }
                return instance
            }
    }

    fun getAddressBook(contentResolver: ContentResolver): List<Contact> {
        var cursor: Cursor
        var contactList = mutableListOf<Contact>()
        try {
            cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
            if (cursor == null) return listOf()
            if (cursor.moveToFirst()) {
                var idColumn = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val displayNameColumn = cursor
                        .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                do {
                    var contactId = cursor.getString(idColumn)
                    var displayName = cursor.getString(displayNameColumn)

                    var phoneCount = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                    if (phoneCount > 0) {
                        var phoneCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                        + "=$contactId", null, null)
                        if (phoneCursor.moveToFirst()) {
                            do {
                                var contact = Contact().apply {
                                    name = displayName
                                    phone = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                }
                                contactList.add(contact)
                            } while (phoneCursor.moveToNext())
                        }
                        CloseUtils.closeIO(phoneCursor)
                    }
                } while (cursor.moveToNext())
            }
            CloseUtils.closeIO(cursor)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var distinctContactSet = TreeSet<Contact>(Comparator<Contact> { o1, o2 -> if (o1.phone == o2.phone) 0 else 1 })
        distinctContactSet.addAll(contactList)
        return distinctContactSet.toList()
    }

    data class Contact(var name: String = "", var phone: String = "")
}