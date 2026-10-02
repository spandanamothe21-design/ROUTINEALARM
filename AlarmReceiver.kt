package com.routine.alarm
import android.content.*

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(c: Context, i: Intent) {
    val tasks = Store.load(c)
    if (i.action == Intent.ACTION_BOOT_COMPLETED) { tasks.forEach { Store.schedule(c, it) }; return }
    val t = tasks.firstOrNull { it[0] == i.getStringExtra("id") } ?: return
    Store.schedule(c, t) // repeat tomorrow
    c.startForegroundService(Intent(c, AlarmService::class.java).putExtra("name", t[1]))
  }
}
