package com.firetest.loginappfire

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.firetest.loginappfire.databinding.ActivityVerifyOtpBinding
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
import java.util.concurrent.TimeUnit

class VerifyOtpActivity : BaseActivity() {
    lateinit var timer: TextView
    private lateinit var myRef: DatabaseReference
    lateinit var code_by_system: String
    lateinit var resend: TextView
    private lateinit var auth:FirebaseAuth
    lateinit var otp: TextView
    lateinit var verify: TextView
    lateinit var credential: PhoneAuthCredential
    var Number_entered_by_user: String? = null
    lateinit var smsBroadcastReceiver: SmsBroadcastReceiver
    private lateinit var binding: ActivityVerifyOtpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        otp = findViewById(R.id.pinview)
        timer = findViewById(R.id.timertext)
        verify = findViewById(R.id.btn_verifyotp)
        resend = findViewById(R.id.resendotp_verifyotp)
        val mobile = intent.getStringExtra("mobile")
        Number_entered_by_user = mobile.toString()
        binding.mobileNumberText.text = mobile

        verify.setOnClickListener {
            try {
                check_code()
            }catch (e: Exception){
                showErrorSnackBar("please wait")
            }
        }
        resend.setOnClickListener {
            resend_otp(Number_entered_by_user!!)
            counttimer()
        }
        send_code_to_user(Number_entered_by_user!!)

    }
    private fun counttimer() {
        startSmsUserConsent()
        object : CountDownTimer(70000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                timer.text = resources.getString(R.string.resend_code) +" "+millisUntilFinished / 1000 + "sec"
            }

            override fun onFinish() {
                timer.visibility = View.GONE
                resend.visibility = View.VISIBLE
            }
        }.start()
    }
    private fun resend_otp(number_entered_by_user: String) {
        send_code_to_user(number_entered_by_user)
    }

    private fun check_code() {
        val user_entered_otp = otp.text.toString()
        if (user_entered_otp.isEmpty() || user_entered_otp.length < 6) {
            showErrorSnackBar("wrong OTP")
            return
        }
        finish_everything(user_entered_otp)
    }

    private fun send_code_to_user(number_entered_by_user: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            number_entered_by_user,
            60,
            TimeUnit.SECONDS,
            this,
            mCallback
        )
        counttimer()
    }

    private val mCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(s: String, forceResendingToken: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(s, forceResendingToken)
                code_by_system = s
            }

            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                val code = phoneAuthCredential.smsCode
                code?.let { finish_everything(it) }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@VerifyOtpActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }

    private fun finish_everything(code: String) {
        otp.setText(code)
        credential = PhoneAuthProvider.getCredential(code_by_system, code)
        sign_in(credential)
    }

    private fun sign_in(credential: PhoneAuthCredential) {
        showProgressDialog("Please wait Logging In")
        auth.signInWithCredential(credential).addOnCompleteListener(this@VerifyOtpActivity
        ) { task ->

            if (task.isSuccessful) {
                val newuser = task.result!!.additionalUserInfo!!
                    .isNewUser
                if (newuser) {
                    val log = getLoginNum()
                    saveLoginNum(log+1)
                }else{
                    myRef = FirebaseAuth.getInstance().uid?.let {
                        FirebaseDatabase.getInstance().reference.child(
                            it
                        ).child("loginNum")
                    }!!
                    // Read from the database
                    myRef.addValueEventListener(object: ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            val value = snapshot.value
                            if (value is Long) {
                                saveLoginNum(value+1)
                            }
                            Log.d("TAG", "Value is: " + value)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.w("TAG", "Failed to read value.", error.toException())
                        }

                    })

                }

                val intent =
                    Intent(this@VerifyOtpActivity, MainActivity::class.java)
                intent.putExtra("login","true")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                hideProgressDialog()
                finish()

            } else {
                Toast.makeText(this@VerifyOtpActivity, task.exception!!.message, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        registerToSmsBroadcastReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(smsBroadcastReceiver)
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_USER_CONSENT -> {
                if ((resultCode == Activity.RESULT_OK) && (data != null)) {
                    //That gives all message to us. We need to get the code from inside with regex
                    val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                    val code = message?.let { fetchVerificationCode(it) }

                    otp.setText(code)
                }
            }
        }
    }

    private fun startSmsUserConsent() {
        SmsRetriever.getClient(this).also {
            //We can add user phone number or leave it blank
            it.startSmsUserConsent(null)
                .addOnSuccessListener {
                    Log.d(TAG, "LISTENING_SUCCESS")
                }
                .addOnFailureListener {
                    Log.d(TAG, "LISTENING_FAILURE")
                }
        }
    }

    private fun registerToSmsBroadcastReceiver() {
        smsBroadcastReceiver = SmsBroadcastReceiver().also {
            it.smsBroadcastReceiverListener = object : SmsBroadcastReceiver.SmsBroadcastReceiverListener {
                override fun onSuccess(intent: Intent?) {
                    intent?.let { context -> startActivityForResult(context, REQ_USER_CONSENT) }
                }

                override fun onFailure() {
                }
            }
        }

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        registerReceiver(smsBroadcastReceiver, intentFilter)
    }

    private fun fetchVerificationCode(message: String): String {
        return Regex("(\\d{6})").find(message)?.value ?: ""
    }

    companion object {
        const val TAG = "SMS_USER_CONSENT"

        const val REQ_USER_CONSENT = 100
    }

}