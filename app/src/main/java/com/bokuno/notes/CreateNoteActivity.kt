package com.bokuno.notes

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.bokuno.notes.daos.NoteDao
import com.bokuno.notes.databinding.ActivityCreateNoteBinding
import com.google.android.gms.location.*
import java.util.*

@Suppress("DEPRECATION")
class CreateNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateNoteBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var location : String?=null
    companion object{
        const val PERMISSION_ID=101
        var TAG = "xyz"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityCreateNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
        binding.tvDetectLocation.setOnClickListener{
            getCurrentLocation()
        }
        binding.scheduleSwitch.setOnCheckedChangeListener{compoundButton,isChecked ->
            if(isChecked){
                binding.timePicker.visibility=View.VISIBLE
                binding.datePicker.visibility= View.VISIBLE
            }
            else{
                binding.timePicker.visibility=View.GONE
                binding.datePicker.visibility= View.GONE
            }
        }

        binding.btnAdd.setOnClickListener{
            val noteDao=NoteDao()
            val title=binding.etTitle.text.toString().trim()
            val note=binding.etNote.text.toString().trim()
            if(binding.scheduleSwitch.isChecked){
                createNotificationChannel()
                scheduleNotification(title,note)
            }
            if(title.isNotEmpty() && note.isNotEmpty()){
                noteDao.addNote(title,note,location)
                finish()
            }
            else if(title.isEmpty()){
                Toast.makeText(this,"Fill the title",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"Fill the note",Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun scheduleNotification(title : String, note : String) {
        val intent = Intent(applicationContext, Notification::class.java)
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, note)

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = getTime()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
        showAlert(time, title, note)
    }

    private fun showAlert(time: Long, title: String, message: String)
    {
        val date = Date(time)
        val dateFormat = android.text.format.DateFormat.getLongDateFormat(applicationContext)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(applicationContext)

        AlertDialog.Builder(this)
            .setTitle("Notification Scheduled")
            .setMessage(
                "Title: " + title +
                        "\nMessage: " + message +
                        "\nAt: " + dateFormat.format(date) + " " + timeFormat.format(date))
            .setPositiveButton("Okay"){_,_ ->}
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getTime(): Long
    {
        val minute = binding.timePicker.minute
        val hour = binding.timePicker.hour
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute)
        return calendar.timeInMillis
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel()
    {
        val name = "Notif Channel"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getCurrentLocation() {
        if (checkPermission()) {
            if (isLocationEnabled()) {
                val task = fusedLocationProviderClient.lastLocation
                task.addOnSuccessListener {
                    if (it != null) {
                        calculateAddress(it)
                    }
                    else{
                        getNewCurrentLocation()
                    }
                }
            } else {
                Toast.makeText(this, "Enable location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION), PERMISSION_ID)
        }
    }

    private fun calculateAddress(location: Location) {
        var subLocalityName = ""
        var cityName = ""
        var geoCoder = Geocoder(this, Locale.getDefault())
        var address = geoCoder.getFromLocation(location.latitude, location.longitude, 2)
        subLocalityName = address?.get(0)!!.subLocality
        cityName = address?.get(0)!!.locality
        this.location = "$subLocalityName $cityName"
        binding.etLocation.setText(this.location)
    }

    private fun getNewCurrentLocation() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        if(checkPermission()) {
            fusedLocationProviderClient!!.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
            )
        }
    }
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation!!
            calculateAddress(mLastLocation)
        }
    }

    private fun checkPermission(): Boolean {
        if(ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun isLocationEnabled():Boolean{
        val locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    getCurrentLocation()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}