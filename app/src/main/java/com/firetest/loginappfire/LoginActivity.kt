package com.firetest.loginappfire

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import com.firetest.loginappfire.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.initialize

class LoginActivity : BaseActivity() {
    private lateinit var ccm:String
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val auth = FirebaseAuth.getInstance()
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        // Check if user is signed in (non-null) and update UI accordingly
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent =
                Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        ccm = binding.ccp.selectedCountryCodeWithPlus
        binding.loginBtn.setOnClickListener {
            if(binding.etMobileNum.text!!.isNotEmpty()){
                sendotp(binding.etMobileNum)
            }else{
                showErrorSnackBar("Please enter correct mobile number")
            }

        }

    }
    fun sendotp(mobile_no: EditText) {
        showProgressDialog(resources.getString(R.string.Sending_Otp))
        if (mobile_no.text.toString().trim { it <= ' ' }.isNotEmpty()) {
            if (mobile_no.text.toString().trim { it <= ' ' }.length == 10) {

                hideProgressDialog()
                val intent =
                    Intent(this@LoginActivity, VerifyOtpActivity::class.java)
                intent.putExtra("mobile",ccm+mobile_no.text.toString())
                startActivity(intent)
            }
            else {
                hideProgressDialog()
                showErrorSnackBar("Please enter your correct number")
            }
        } else {
            hideProgressDialog()
            showErrorSnackBar("Please enter your details")
        }
    }
}