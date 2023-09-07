package com.prac.ai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.prac.ai.databinding.ActivityAlarmChangeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prac.ai.models.Alarm

class AlarmChange : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmChangeBinding
    private lateinit var user: FirebaseUser
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var uid:String
    private var alarm: Alarm? = null
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        val alarm_num = intent.getIntExtra("ALARM_INDEX", -1)+1
         alarm = intent.extras?.getParcelable<Alarm>("ALARM_DATA")
            ?: Alarm(0, 0, "알람", "알람", "약 먹을 시간입니다.", false, alarm_num)
        super.onCreate(savedInstanceState)
        user = Firebase.auth.currentUser!!
        auth = Firebase.auth
        db = Firebase.firestore
        uid = user.uid
        binding = ActivityAlarmChangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        // TimePicker의 리스너 설정
        binding.timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
        }

        // "적용" 버튼 클릭 리스너 설정
        binding.navbutton.setOnClickListener {

            val alarmTitle = binding.EditTextTitle.text.toString()
            val alarmContent = binding.editTextAlarmContent.text.toString()
            alarm!!.alarm_title = alarmTitle
            alarm!!.alarm_content = alarmContent
            alarm!!.hour = selectedHour
            alarm!!.minute = selectedMinute
            val MediRef = db.collection("User")
                .document(uid)
                .collection("Alarm")
                .whereEqualTo("alarm_num", 1)

            // 결과 가져오기
            MediRef.get().addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    // 각 문서 처리
                    val docRef = db.collection("User").document(uid).collection("Alarm").document(document.id)

                    val updatedData = hashMapOf(
                        "alarm_title" to alarmTitle,
                        "alarm_content" to alarmContent,
                        "hour" to selectedHour,
                        "minute" to selectedMinute
                    )

                    docRef.update(updatedData as Map<String, Any>).addOnSuccessListener {
                        // 업데이트 성공 시 처리
                        finish()
                    }.addOnFailureListener { e ->
                        // 업데이트 실패 시 처리
                        finish()
                    }
                }
            }.addOnFailureListener { exception ->
                // 오류 처리
                finish()
            }
        }
    }
}