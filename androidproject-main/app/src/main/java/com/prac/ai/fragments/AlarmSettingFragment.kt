package com.prac.ai.fragments

import android.os.Bundle
import com.prac.ai.NotificationReceiver
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prac.ai.databinding.FragmentAlarmSettingBinding
import com.prac.ai.databinding.ItemAlarmBinding
import java.text.SimpleDateFormat
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*
import com.prac.ai.fragments.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prac.ai.models.Alarm
import com.prac.ai.AlarmChange
import com.prac.ai.R

class AlarmAdapter(private val alarms: List<Alarm>, private val listener: OnAlarmClickListener) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>()  {

    interface OnAlarmClickListener {
        fun onAlarmClick(alarm: Alarm, position: Int)
    }
    inner class AlarmViewHolder(private val binding: ItemAlarmBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(alarm: Alarm) {
            binding.alarmTime.text = formatTime(alarm.hour, alarm.minute)
            binding.alarmName.text = alarm.alarm_name
            binding.root.setOnClickListener {
                listener?.onAlarmClick(alarm, bindingAdapterPosition)  // position을 함께 전달
            }

            binding.alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
                alarm.enable = isChecked
            }
        }

        private fun formatTime(hour: Int, minute: Int): String {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            val sdf = SimpleDateFormat("a h:mm", Locale.KOREAN)
            return sdf.format(calendar.time)
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(alarms[position])
    }

    override fun getItemCount(): Int = alarms.size
}

class AlarmSettingFragment : Fragment() {

    private var user: FirebaseUser? = null
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var uid:String? = null
    private var alarms = mutableListOf<Alarm>()

    private lateinit var binding: FragmentAlarmSettingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlarmSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = Firebase.auth.currentUser!!
        auth = Firebase.auth
        db = Firebase.firestore
        uid = user!!.uid
        db!!.collection("User").document(uid!!).get()
            .addOnSuccessListener { document ->
                val max = document["max_medi"]?.toString()?.toIntOrNull() ?: return@addOnSuccessListener

                // If the data doesn't match, populate the default alarms. Otherwise, fetch from Firestore.
                db!!.collection("User").document(uid!!).collection("Alarm").get()
                    .addOnSuccessListener { alarmDocuments ->
                        if (alarmDocuments.size() != max) {
                            alarms = getDefaultAlarms(max)
                        } else {
                            for (doc in alarmDocuments) {
                                alarms.add(doc.toObject(Alarm::class.java))
                            }
                        }

                        setupRecyclerView()
                        scheduleAlarms()
                    }
            }

        binding.applyButton.setOnClickListener {
            updateAlarmsInFirestore()
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, MessengerFragment())  // fragment_container는 프래그먼트가 들어가는 레이아웃의 ID입니다. 실제 레이아웃의 ID로 변경해야 합니다.
            transaction.addToBackStack(null)  // 이전 프래그먼트로 돌아갈 수 있게 스택에 추가합니다.
            transaction.commit()
        }
    }



    private fun getDefaultAlarms(max: Int): MutableList<Alarm> {
        return when (max) {
            0 -> mutableListOf(
                Alarm(0,0,"시간1", "시간1 안부","약은 잘 드셨나요?",false, 1),
                Alarm(0,10,"시간2","시간2 안부","약은 잘 드셨나요?",false,2),
                Alarm(0,20,"시간3", "시간3 안부","약은 잘 드셨나요?",false,3),
                Alarm(0,30,"시간4", "시간4 안부","약은 잘 드셨나요?",false,4),
                Alarm(0,40,"시간5", "시간5 안부","약은 잘 드셨나요?",false,5)
            )
            1 -> mutableListOf(
                Alarm(13,0,"점심", "점심안부","약은 잘 드셨나요?",true, 1),
                Alarm(0,0,"시간2","시간2 안부","약은 잘 드셨나요?",false,2),
                Alarm(0,10,"시간3", "시간3 안부","약은 잘 드셨나요?",false,3),
                Alarm(0,20,"시간4", "시간4 안부","약은 잘 드셨나요?",false,4),
                Alarm(0,30,"시간5", "시간5 안부","약은 잘 드셨나요?",false,5)
            )
            2 -> mutableListOf(
                Alarm(8,0,"아침","아침안부","약은 잘 드셨나요?",true,1),
                Alarm(20,0,"저녁", "저녁안부","약은 잘 드셨나요?",true,2),
                Alarm(0,0,"시간3", "시간3 안부","약은 잘 드셨나요?",false,3),
                Alarm(0,10,"시간4", "시간4 안부","약은 잘 드셨나요?",false,4),
                Alarm(0,20,"시간5", "시간5 안부","약은 잘 드셨나요?",false,5)
            )
            3 -> mutableListOf(
                Alarm(8,0,"아침","아침안부","약은 잘 드셨나요?",true,1),
                Alarm(13,0,"점심", "점심안부","약은 잘 드셨나요?",true,2),
                Alarm(20,0,"저녁", "저녁안부","약은 잘 드셨나요?",true,3),
                Alarm(0,0,"시간4", "시간4 안부","약은 잘 드셨나요?",false,4),
                Alarm(0,10,"시간5", "시간5 안부","약은 잘 드셨나요?",false,5)
            )
            4 -> mutableListOf(
                Alarm(1,0,"시간1","시간1 안부","약은 잘 드셨나요?",true,1),
                Alarm(7,0,"시간2","시간2 안부","약은 잘 드셨나요?",true,2),
                Alarm(13,0,"시간3", "시간3 안부","약은 잘 드셨나요?",true,3),
                Alarm(19,0,"시간4", "시간4 안부","약은 잘 드셨나요?",true,4),
                Alarm(0,0,"시간5", "시간5 안부","약은 잘 드셨나요?",false,5)
            )
            5 -> mutableListOf(
                Alarm(4,48,"시간1","시간1 안부","약은 잘 드셨나요?",true,1),
                Alarm(9,36,"시간2","시간2 안부","약은 잘 드셨나요?",true,2),
                Alarm(14,24,"시간3", "시간3 안부","약은 잘 드셨나요?",true,3),
                Alarm(19,12,"시간4", "시간4 안부","약은 잘 드셨나요?",true,4),
                Alarm(0,0,"시간5", "시간5 안부","약은 잘 드셨나요?",true,5)
            )
            else -> mutableListOf()
        }
    }

    private fun setupRecyclerView() {
        binding.alarmRecyclerView.apply {
            adapter = AlarmAdapter(alarms, object : AlarmAdapter.OnAlarmClickListener {
                override fun onAlarmClick(alarm: Alarm, position: Int) {
                    // TODO: 알람 설정 화면으로 이동하는 코드를 작성하세요.
                    val intent = Intent(context, AlarmChange::class.java)
                    intent.putExtra("ALARM_DATA", alarm)
                    intent.putExtra("ALARM_INDEX", position)
                    startActivity(intent)
                }
            })
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }
    private fun scheduleAlarms() {
        alarms.forEachIndexed { index, alarm ->
            if (alarm.enable) {
                scheduleNotification(index, alarm)
            }
        }
    }

    private fun scheduleNotification(notificationId: Int, alarm: Alarm) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
        }

        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("NOTIFICATION_TITLE", alarm.alarm_title)
            putExtra("NOTIFICATION_CONTENT", alarm.alarm_content)
        }

        val pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager?.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
    }

    private fun updateAlarmsInFirestore() {
        uid?.let { id ->
            val userAlarmCollection = db!!.collection("User").document(id).collection("Alarm")

            userAlarmCollection.get()
                .addOnSuccessListener { documents ->
                    val deleteTasks = documents.map { it.reference.delete() }

                    Tasks.whenAllSuccess<Void>(deleteTasks)
                        .addOnSuccessListener {
                            alarms.forEach { userAlarmCollection.add(it) }
                        }
                }
        }
    }
}
