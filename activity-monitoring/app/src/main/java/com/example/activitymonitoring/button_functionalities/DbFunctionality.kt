package com.example.activitymonitoring.button_functionalities

import android.app.AlertDialog
import android.os.Environment
import android.view.View
import android.widget.TextView
import com.example.activitymonitoring.MainActivity
import com.example.activitymonitoring.R
import com.example.activitymonitoring.defines.StringDefines
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class DbFunctionality {
    fun writeToFile() {
        val mainRef = MainActivity().getInstance()
        val textStatus = mainRef.findViewById<TextView>(R.id.textviewWritingStatus)

        if(mainRef.recordingStatus) {
            textStatus.text = StringDefines().STRING_CANNOT_WRITE_RECORDING
            textStatus.setBackgroundResource(R.color.red)
            return
        }

        textStatus.text = StringDefines().STRING_START_WRITING_TO_FILE
        textStatus.setBackgroundResource(R.color.yellow)
        val timestamp = Calendar.getInstance().timeInMillis

        GlobalScope.launch {
            if(!mainRef.dbAcc.storeToFile(mainRef.databAcc, timestamp)) {
                textStatus.post {
                    textStatus.text = StringDefines().STRING_WRITING_TO_FILE_FAILED_ACC
                    textStatus.setBackgroundResource(R.color.red)
                }
            } else {
                textStatus.post {
                    textStatus.text = StringDefines().STRING_SUCCESSFULLY_WRITTEN
                    textStatus.setBackgroundResource(R.color.green)
                }
            }
        }
    }

    fun deleteDbEntries(view: View) {
        val builder = AlertDialog.Builder(view.context)
        val mainRef = MainActivity().getInstance()

        builder.setTitle("Database Deletion?")
        builder.setMessage("Do you really want to delete all Database entries?")

        builder.setPositiveButton("YES") { dialog, _ ->
            dialog.dismiss()
            val textStatus = mainRef.findViewById<TextView>(R.id.textViewDeletionStatus)

            if(mainRef.recordingStatus) {
                textStatus.text = StringDefines().STRING_CANNOT_DELETE_RECORDING
                textStatus.setBackgroundResource(R.color.red)
                return@setPositiveButton
            }

            textStatus.text = StringDefines().STRING_START_DELETING_DATA
            textStatus.setBackgroundResource(R.color.yellow)

            GlobalScope.launch {
                if(!mainRef.dbAcc.deleteDBEntries(mainRef.databAcc)) {
                    textStatus.post {
                        textStatus.text = StringDefines().STRING_FAILED_DELETION_ACC
                        textStatus.setBackgroundResource(R.color.red)
                    }
                } else {
                    textStatus.post {
                        textStatus.text = StringDefines().STRING_SUCCESSFUL_DELETION
                        textStatus.setBackgroundResource(R.color.green)
                    }
                }
            }
        }

        builder.setNegativeButton(
            "NO"
        ) { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    fun isExternalStorageReadOnly(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState) {
            return true
        }
        return false
    }

    fun isExternalStorageAvailable(): Boolean {
        val extStorageState = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == extStorageState) {
            return true
        }
        return false
    }
}