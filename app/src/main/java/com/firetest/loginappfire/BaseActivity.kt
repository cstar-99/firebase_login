package com.firetest.loginappfire

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

open class BaseActivity : AppCompatActivity(){
    private lateinit var mProgressDialog: Dialog
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

    }
    fun saveLoginNum(loginNum: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong("loginNum", loginNum)
        editor.apply()
    }
    fun deletePref(){
        sharedPreferences.edit().clear().commit()
    }

    fun getLoginNum(): Long {
        return sharedPreferences.getLong("loginNum", 0)
    }
    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.dialog_progress)
        val textView = mProgressDialog.findViewById<TextView>(R.id.tv_progress_text)
        textView.text = text
        mProgressDialog.setCancelable(false)
        mProgressDialog.show()
    }

    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }
    fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser!!.uid
    }
    fun getCurrentUserName(): String? {
        return FirebaseAuth.getInstance().currentUser!!.displayName
    }

    fun showErrorSnackBar(message: String) {
        val snackBar = Snackbar.make(
            findViewById(android.R.id.content),
            message, Snackbar.LENGTH_LONG
        )
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        snackBar.show()

    }
}