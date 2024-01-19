package com.firetest.loginappfire

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.properties.Delegates

class VerifyOtpActivity : BaseActivity() {
    lateinit var timer: TextView
    private lateinit var myRef: DatabaseReference
    lateinit var code_by_system: String
    lateinit var resend: TextView
    private lateinit var auth:FirebaseAuth
    lateinit var otp: TextView
    var newuser by Delegates.notNull<Boolean>()
    lateinit var verify: TextView
    lateinit var credential: PhoneAuthCredential
    var Number_entered_by_user: String? = null
    private lateinit var formattedMonth: String
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val usersReference = firebaseDatabase.getReference("users")
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

        val calendar: Calendar = Calendar.getInstance()
        calendar.get(Calendar.MONTH) + 1
        val sdf = SimpleDateFormat("MM", Locale.getDefault())
        formattedMonth = sdf.format(calendar.time)


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
            0,
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
//                val code = phoneAuthCredential.smsCode
//                code?.let { finish_everything(it) }
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
                newuser = task.result!!.additionalUserInfo!!
                    .isNewUser
                val user: FirebaseUser? = auth.currentUser
                user?.uid?.let { uid ->
                    // Check if the user node exists
                    checkUserNode(uid)
                }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsBroadcastReceiver, intentFilter,RECEIVER_EXPORTED)
        }else{
            registerReceiver(smsBroadcastReceiver, intentFilter)
        }
    }

    private fun fetchVerificationCode(message: String): String {
        return Regex("(\\d{6})").find(message)?.value ?: ""
    }

    companion object {
        const val TAG = "SMS_USER_CONSENT"

        const val REQ_USER_CONSENT = 100
    }

    private fun checkUserNode(uid: String) {
        val userNodeReference: DatabaseReference = usersReference.child(uid)

        userNodeReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // User node doesn't exist, create the structure
                    createUserNodeStructure(userNodeReference)
                }else{
                    finalCall()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    private fun createUserNodeStructure(userNodeReference: DatabaseReference) {

        val initialLogins: MutableMap<String, Any> = HashMap()
        for (month in 1..12) {
            // Initialize each month with 0 logins
            initialLogins[month.toString().padStart(2, '0')] = 0
        }

        val userStructure: MutableMap<String, Any> = HashMap()
        userStructure["logins"] = initialLogins

        // Set the initial structure in the database
        userNodeReference.child(currentYear.toString()).setValue(userStructure)

        finalCall()
    }

    private fun finalCall() {
        Log.d("TAG", "final called")
        if (newuser) {
            Log.d("TAG", "final called new")
            val log = getLoginNum()
            saveLoginNum(log+1)

            val intent =
                Intent(this@VerifyOtpActivity, MainActivity::class.java)
            intent.putExtra("login","true")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            hideProgressDialog()
            finish()
        }else{
            Log.d("TAG", "final called reg")
            myRef = usersReference.child(FirebaseAuth.getInstance().uid!!).child(currentYear.toString()).child("logins").child(formattedMonth)


            // Read from the database
            myRef.addListenerForSingleValueEvent(object: ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    val value = snapshot.value
                    if (value is Long) {
                        saveLoginNum(value+1)
                    }
                    Log.d("TAG", "Value is: " + value)
                    val intent =
                        Intent(this@VerifyOtpActivity, MainActivity::class.java)
                    intent.putExtra("login","true")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    hideProgressDialog()
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("TAG", "Failed to read value.", error.toException())
                }

            })

        }



    }

}