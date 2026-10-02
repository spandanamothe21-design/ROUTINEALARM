package com.routine.alarm
import android.app.*
import android.content.*
import android.media.*
import android.os.*
import android.speech.tts.*

class AlarmService : Service(), TextToSpeech.OnInitListener {
  private var tts: TextToSpeech? = null
  private var mp: MediaPlayer? = null
  private var name = ""; private var round = 0; private var ready = false
  private val h = Handler(Looper.getMainLooper())
  override fun onBind(i: Intent?): IBinder? = null
  override fun onInit(s: Int) { ready = s == TextToSpeech.SUCCESS
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(u: String?) {}
      override fun onError(u: String?) { h.post { next() } }
      override fun onDone(u: String?) { h.postDelayed({ next() }, 1500) } }) }
  override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
    if (i?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
    name = i?.getStringExtra("name") ?: "your task"; round = 0
    val nm = getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(NotificationChannel("al", "Alarms", NotificationManager.IMPORTANCE_HIGH))
    val stop = PendingIntent.getService(this, 1, Intent(this, AlarmService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
    val n = Notification.Builder(this, "al").setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle("Time to: $name").setContentText("Tap Stop to dismiss")
      .addAction(Notification.Action.Builder(null, "STOP", stop).build()).setOngoing(true).build()
    startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    tts = TextToSpeech(this, this); music(); return START_NOT_STICKY
  }
  private fun music() {
    try {
      mp?.release()
      mp = MediaPlayer().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
        setDataSource(this@AlarmService, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        isLooping = true; prepare(); start() }
    } catch (e: Exception) {}
    h.postDelayed({ speak() }, 8000)
  }
  private fun speak() {
    mp?.stop()
    if (!ready) { h.postDelayed({ speak() }, 700); return }
    val a = Bundle().apply { putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM) }
    tts?.speak("Time to $name. $name.", TextToSpeech.QUEUE_FLUSH, a, "u")
  }
  private fun next() { if (++round >= 3) stopSelf() else music() }
  override fun onDestroy() { h.removeCallbacksAndMessages(null); mp?.release(); tts?.shutdown(); super.onDestroy() }
}
