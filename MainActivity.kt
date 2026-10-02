package com.routine.alarm
import android.app.*
import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.util.Calendar

object Store {
  private fun p(c: Context) = c.getSharedPreferences("rt", 0)
  fun load(c: Context): MutableList<List<String>> =
    p(c).getString("t", "")!!.split("\n").filter { it.isNotBlank() }.map { it.split("|") }.toMutableList()
  fun save(c: Context, l: List<List<String>>) =
    p(c).edit().putString("t", l.joinToString("\n") { it.joinToString("|") }).apply()
  private fun pi(c: Context, t: List<String>): PendingIntent =
    PendingIntent.getBroadcast(c, t[0].toInt(), Intent(c, AlarmReceiver::class.java).putExtra("id", t[0]),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  fun schedule(c: Context, t: List<String>) {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, t[2].toInt()); set(Calendar.MINUTE, t[3].toInt()); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }
    val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
      .setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, show), pi(c, t))
  }
  fun cancel(c: Context, t: List<String>) =
    (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi(c, t))
}

class MainActivity : Activity() {
  var h = 7; var m = 0
  lateinit var list: LinearLayout
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 100, 40, 40) }
    root.addView(TextView(this).apply { text = "⏰ Daily Routine Alarm"; textSize = 26f })
    val name = EditText(this).apply { hint = "Task name (e.g. Drink water)" }
    val tb = Button(this)
    fun lbl() { tb.text = "Time: %02d:%02d  (tap to change)".format(h, m) }; lbl()
    tb.setOnClickListener { TimePickerDialog(this, { _, hh, mm -> h = hh; m = mm; lbl() }, h, m, false).show() }
    val add = Button(this).apply { text = "+ Add task" }
    val test = Button(this).apply { text = "▶ Test alarm now" }
    list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    listOf(name, tb, add, test, list).forEach { root.addView(it) }
    add.setOnClickListener {
      val n = name.text.toString().replace("|", " ").replace("\n", " ").trim()
      if (n.isEmpty()) { Toast.makeText(this, "Enter a task name", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
      val t = listOf((System.currentTimeMillis() % 1000000000L).toString(), n, h.toString(), m.toString())
      val l = Store.load(this); l.add(t); Store.save(this, l); Store.schedule(this, t); name.setText(""); render()
    }
    test.setOnClickListener {
      val n = name.text.toString().ifBlank { "Drink a glass of water" }
      startForegroundService(Intent(this, AlarmService::class.java).putExtra("name", n))
    }
    setContentView(ScrollView(this).apply { addView(root) }); render()
  }
  fun render() {
    list.removeAllViews()
    Store.load(this).sortedWith(compareBy({ it[2].toInt() }, { it[3].toInt() })).forEach { t ->
      val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, 20, 0, 20) }
      row.addView(TextView(this).apply { text = "%02d:%02d  %s".format(t[2].toInt(), t[3].toInt(), t[1]); textSize = 18f
        layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
      row.addView(Button(this).apply { text = "✕"; setOnClickListener {
        Store.cancel(context, t); Store.save(context, Store.load(context).filter { it[0] != t[0] }); render() } })
      list.addView(row)
    }
  }
}
