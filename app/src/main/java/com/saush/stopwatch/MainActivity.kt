package com.saush.stopwatch

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

const val CHANNEL_ID = "timer_notification"

class MainActivity : AppCompatActivity() {
    var seconds = 0
    var limit = 0
    var isRunning = false
    var shouldBeNotified = false
    var wasNotified = false
    val handler = Handler(Looper.getMainLooper())

    private lateinit var runnable: Runnable
    lateinit var runnableHelp: Runnable
    lateinit var timeView: TextView
    lateinit var progressBar: ProgressBar
    private lateinit var settingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        runnable = object: Runnable {
            override fun run() {
                val minutes = (seconds % 3600) / 60
                val secs = seconds % 60
                val formattedMin = if (minutes > 9) "$minutes" else "0$minutes"
                val formattedSec = if (secs > 9) "$secs" else "0$secs"
                val formattedTime = "$formattedMin:$formattedSec"
                timeView.text = formattedTime

                if (isRunning) {
                    if (limit != 0 && seconds > limit) timeView.setTextColor(Color.RED)
                    else timeView.setTextColor(Color.BLACK)
                    seconds++
                }
                handler.postDelayed(this, 1000)
            }
        }
        runnableHelp = object: Runnable {
            override fun run() {
                if (isRunning) {
                    if (limit != 0 && seconds > limit && !wasNotified && shouldBeNotified) {
                        wasNotified = true
                        sendNotification()
                    }
                    progressBar.indeterminateTintList = ColorStateList.valueOf(getRandomColor())

                    handler.postDelayed(this, 1000)
                }
            }
        }
        timeView = findViewById(R.id.textView)
        progressBar = findViewById(R.id.progressBar)
        settingsButton = findViewById(R.id.settingsButton)

        onClickNotifications()
    }

    fun onClickStart(view: View) {
        if (!isRunning) {
            isRunning = true
            runTimer()
            progressBar.visibility = View.VISIBLE
            settingsButton.isEnabled = false
        }
    }

    fun onClickReset(view: View) {
        isRunning = false
        seconds = 0
        wasNotified = false
        timeView.text = "00:00"
        timeView.setTextColor(Color.GRAY)
        handler.removeCallbacks(runnable)
        progressBar.visibility = View.INVISIBLE
        settingsButton.isEnabled = true
    }

    fun onClickSettings(view: View) {
        val builder = AlertDialog.Builder(this)
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_NUMBER

        builder.setTitle(R.string.upperLimit)
        builder.setView(editText)
        builder.setPositiveButton("OK") { _, _ ->
            limit = try {
                editText.text.toString().toInt()
            } catch (e: Exception) {
                0
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->  dialog.cancel() }
        builder.show()
    }

    private fun onClickNotifications() {
        val notButton: Button = findViewById(R.id.notificationsButton)
        val items = arrayOf("Enable notifications", "Disable notifications")
        var selectedItem = ""

        notButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setSingleChoiceItems(items, -1) { _, i ->
                    selectedItem = items[i]
                    shouldBeNotified = when (selectedItem) {
                        "Enable notifications" -> true
                        "Disable notifications" -> false
                        else -> shouldBeNotified
                    }
                }
                .setPositiveButton(android.R.string.ok) { _, _: Int ->
                    Toast.makeText(this,
                        when (selectedItem) {
                            "Enable notifications" -> "Notifications enabled"
                            "Disable notifications" -> "Notifications disabled"
                            else -> "Something go wrong..."
                       }, Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun runTimer() {
        handler.post(runnable)
        handler.post(runnableHelp)
    }

    private fun sendNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Time exceeded"
            val descriptionText = "Time is out!"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pIntent = PendingIntent
            .getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val nBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Notification")
            .setContentText("Time exceeded")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pIntent)

        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, nBuilder.build())
    }

    private fun getRandomColor(): Int {
        val a = (0..255).random()
        val b = (0..255).random()
        val c = (0..255).random()
        val d = (0..255).random()
        return Color.argb(a, b, c, d)
    }
}