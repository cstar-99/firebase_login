package com.firetest.loginappfire

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.firetest.loginappfire.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log
import kotlin.properties.Delegates

class MainActivity : BaseActivity() {
    private lateinit var login:String
    private lateinit var myRef: DatabaseReference
    private lateinit var formattedMonth: String
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val usersReference = firebaseDatabase.getReference("users")
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val calendar: Calendar = Calendar.getInstance()
        calendar.get(Calendar.MONTH) + 1
        val sdf = SimpleDateFormat("MM", Locale.getDefault())
        formattedMonth = sdf.format(calendar.time)

        myRef = usersReference.child(FirebaseAuth.getInstance().uid!!).child(currentYear.toString())

        // Read from the database
        myRef.addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val months = arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
                val loginCounts = ArrayList<Int>()

                for (month in months) {
                    val count = snapshot.child("logins").child(month).getValue(Int::class.java) ?: 0
                    loginCounts.add(count)
                }

                updateBarChart(binding.barChart, months, loginCounts)

                Log.d("TAG", "Value received: ")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("TAG", "Failed to read value.", error.toException())
            }

        })

        if(intent != null){
            login= intent.getStringExtra("login").toString()
            if(login == "true"){
                Log.d("TAG", "updated value log")
                val counter = getLoginNum()
                val myRef2 = usersReference.child(FirebaseAuth.getInstance().uid!!).child(currentYear.toString()).child("logins").child(formattedMonth)

                myRef2.setValue(counter)
            }
        }
        binding.logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            deletePref()
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun updateBarChart(barChart: BarChart, months: Array<String>, loginCounts: List<Int>) {
        val entries: ArrayList<BarEntry> = ArrayList()
        for (i in loginCounts.indices) {
            entries.add(BarEntry(i.toFloat(), loginCounts[i].toFloat()))
        }

        val dataSet = BarDataSet(entries, "Logins")
        val data = BarData(dataSet)

        // Customize chart appearance
        dataSet.color = resources.getColor(R.color.blue2)
        dataSet.valueTextColor = resources.getColor(android.R.color.black)
        barChart.data = data
        barChart.setFitBars(true)
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false

        // Customize x-axis
        val xAxis: XAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(months)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
    }
}