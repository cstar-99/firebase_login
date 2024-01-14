package com.firetest.loginappfire

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.firetest.loginappfire.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.log
import kotlin.properties.Delegates

class MainActivity : BaseActivity() {
    private lateinit var login:String
    private var valueL by Delegates.notNull<Long>()
    private lateinit var myRef: DatabaseReference
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

                binding.mainText.text = value.toString()
                Log.d("TAG", "Value is: " + value)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("TAG", "Failed to read value.", error.toException())
            }

        })

        if(intent != null){
            login= intent.getStringExtra("login").toString()
            if(login == "true"){
                val counter = getLoginNum()
                myRef.setValue(counter)
            }
        }
        binding.logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}