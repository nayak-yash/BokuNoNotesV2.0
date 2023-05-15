package com.bokuno.notes.ui.fragments.notes

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat.is24HourFormat
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bokuno.notes.*
import com.bokuno.notes.databinding.FragmentNoteBinding
import com.bokuno.notes.models.Note
import com.bokuno.notes.models.HelpRequest
import com.bokuno.notes.notif.channelID
import com.bokuno.notes.notif.messageExtra
import com.bokuno.notes.notif.notificationID
import com.bokuno.notes.notif.titleExtra
import com.bokuno.notes.utils.Constants
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class NoteFragment : Fragment() {

    private var hour: Int = 0
    private var minute: Int = 0
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel by viewModels<NotesViewModel>()
    private var editnote: Note? = null
    private var isScheduled: Boolean = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var location : String?=null
    @Inject
    lateinit var mAuth: FirebaseAuth

    private lateinit var timePickerDialog: TimePickerDialog

    companion object{
        const val PERMISSION_ID=101
        var TAG = "xyz"
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInitialData()
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(activity!!)
        bindObserver()
    }

    private fun bindObserver() {
        binding.ivLocation.setOnClickListener{
            getCurrentLocation()
        }

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivAlarm.setOnClickListener {
            if(!isScheduled){
                openTimePicker()
            }
            else{
                isScheduled = false
            }
        }
        binding.ivSubmit.setOnClickListener{
            val title=binding.etTitle.text.toString().trim()
            val note=binding.etNote.text.toString().trim()
            if(title.isNotEmpty() && note.isNotEmpty()){
                if(editnote != null){
                    editnote!!.title = title
                    editnote!!.text = note
                    noteViewModel.updateNote(editnote!!)
                }
                else{
                    noteViewModel.addNote(Note(
                        title = title, location = location, text = note, createdAt = System.currentTimeMillis(), userId = mAuth.currentUser?.uid.toString()
                    ))
                }

                if(isScheduled){
                    createNotificationChannel()
                    scheduleNotification(title,note)
                }
                findNavController().popBackStack()
            }
            else if(title.isEmpty()){
                Toast.makeText(activity,"Fill the title",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(activity,"Fill the note",Toast.LENGTH_SHORT).show()
            }
        }


        var job: Job? = null
        binding.ivHelp.setOnClickListener {
                job?.cancel()
                job = MainScope().launch {
                    delay(Constants.SEARCH_NOTES_TIME_DELAY)
                    val help = binding.etChatAI.text.toString()
                    if(help.isNotEmpty()){
                        val helpRequest = HelpRequest(prompt = help)
                        if(noteViewModel.sendMessage(helpRequest)){
                            binding.etChatAI.text.clear()
                        }
                        else{
                            Toast.makeText(activity,"Network Failure",Toast.LENGTH_SHORT).show()
                        }
                    }
                    else{
                        Toast.makeText(activity,"Please fill the query!!",Toast.LENGTH_SHORT).show()
                    }
                }
        }
        noteViewModel.response.observe(viewLifecycleOwner){
            it?.let{
                binding.etNote.text = binding.etNote.text.append('\n').append(it)
            }
        }
    }

    private fun openTimePicker() {
        val isSystem24Hour = is24HourFormat(requireContext())
        val timePickerDialog = TimePickerDialog(
            activity,
            { view, hourOfDay, minuteOfDay ->
                hour = hourOfDay
                minute = minuteOfDay
                isScheduled = true
            },
            hour,
            minute,
            isSystem24Hour
        )
        timePickerDialog.show()
    }

    private fun setInitialData() {
        val jsonNote = arguments?.getString("note")
        if (jsonNote != null) {
            editnote = Gson().fromJson<Note>(jsonNote, Note::class.java)
            editnote?.let {
                binding.etTitle.setText(it.title)
                binding.etNote.setText(it.text)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun scheduleNotification(title : String, note : String) {
        val intent = Intent(context, Notification::class.java)
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, note)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
        val dateFormat = android.text.format.DateFormat.getLongDateFormat(activity)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(activity)

        AlertDialog.Builder(activity)
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
        val calendar: Calendar = Calendar.getInstance()
        if (Build.VERSION.SDK_INT >= 23) {
            calendar.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
                0
            )
        }
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
        val notificationManager = activity!!.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
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
                Toast.makeText(activity, "Enable location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            ActivityCompat.requestPermissions(activity!!, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSION_ID
            )
        }
    }

    private fun calculateAddress(location: Location) {
        var subLocalityName = ""
        var cityName = ""
        var geoCoder = Geocoder(activity!!, Locale.getDefault())
        var address = geoCoder.getFromLocation(location.latitude, location.longitude, 2)
        subLocalityName = address?.get(0)!!.subLocality
        cityName = address?.get(0)!!.locality
        this.location = "$subLocalityName $cityName"
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
        if(ActivityCompat.checkSelfPermission(activity!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(activity!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    private fun isLocationEnabled():Boolean{
        val locationManager=activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "Permission Granted", Toast.LENGTH_SHORT).show()
                    getCurrentLocation()
                } else {
                    Toast.makeText(activity, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}