package com.prac.ai.fragments

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.app.Activity
import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.MediaStore
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.prac.ai.CaptureActivity
import com.prac.ai.LoginActivity
import com.prac.ai.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import com.prac.ai.databinding.FragmentMedicationBinding
import com.prac.ai.databinding.ItemCardBinding
import com.prac.ai.databinding.ItemTextBinding
import java.io.ByteArrayInputStream

data class Medications(val name: String, var take: String?, var current_medi: String?)
data class Hospital(val name: String, val medications: MutableList<Medications>)
data class Medication(val hospital_name: String, val medi_name: String, val one_day: Int, val enable:Boolean, val end_date: Timestamp){
    constructor() : this("", "",0, true,Timestamp.now())
}
class CardAdapter(private val hospitals: List<Hospital>): RecyclerView.Adapter<CardAdapter.CardView>(){
    inner class CardView(private val binding: ItemCardBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(position: Int){
            binding.CardTitle.text = hospitals[position].name
            binding.RecycleText.apply{
                adapter = TextAdapter(hospitals[position].medications)
                layoutManager = LinearLayoutManager(binding.RecycleText.context, LinearLayoutManager.VERTICAL,false)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardView {
        val view = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardView(view)
    }

    override fun onBindViewHolder(holder: CardView, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return hospitals.size
    }
}

class TextAdapter(private val medications: List<Medications>): RecyclerView.Adapter<TextAdapter.TextView>(){
    inner class TextView(private val binding: ItemTextBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(position: Int){
            binding.medicineName.text = medications[position].name
            binding.medicineVol.text = medications[position].current_medi?:""
            binding.medicineTake.text = medications[position].take?:R.string.not_entered.toString()

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextView {
        val view = ItemTextBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return TextView(view)
    }

    override fun onBindViewHolder(holder: TextView, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return medications.size
    }
}

class MedicationFragment : Fragment() {
    private var user: FirebaseUser? = null
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var uid: String? = null
    private var storage: FirebaseStorage? = null
    private var isFabOpen = false
    lateinit var medicationBinding: FragmentMedicationBinding
    // 약물 목록 생성

    val hospitals = mutableListOf<Hospital>(

    )

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        medicationBinding = FragmentMedicationBinding.inflate(layoutInflater, container, false)
        return medicationBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = Firebase.auth.currentUser!!
        auth = Firebase.auth
        db = Firebase.firestore
        uid = user!!.uid
        storage = Firebase.storage
        medicationBinding.recyclerView.apply {
            adapter = CardAdapter(hospitals)
            layoutManager = LinearLayoutManager(context)
        }
        db!!.collection("User").document(uid!!).collection("Medication")
            .get()
            .addOnSuccessListener { result ->
                val hospitalMap = mutableMapOf<String, MutableList<Medications>>()

                for (document in result) {
                    val newMedication2 = document.toObject(Medication::class.java)
                    val take = "1일 ${newMedication2.one_day ?: 0}회 복용"
                    val current_medi = if (newMedication2.enable) "복용중" else ""
                    val newMedication = Medications(newMedication2.medi_name, take, current_medi)

                    if (newMedication2.hospital_name != null) {
                        if (!hospitalMap.containsKey(newMedication2.hospital_name)) {
                            hospitalMap[newMedication2.hospital_name] = mutableListOf()
                        }
                        hospitalMap[newMedication2.hospital_name]!!.add(newMedication)
                    }
                }
                hospitals.clear() // 기존의 hospitals 데이터를 지웁니다.
                for ((key, value) in hospitalMap) {
                    hospitals.add(Hospital(key, value))
                }
                (medicationBinding.recyclerView.adapter as? CardAdapter)?.notifyDataSetChanged()


            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
        setFABClickEvent()
    }


    private fun setFABClickEvent() {
        // 플로팅 버튼 클릭시 애니메이션 동작 기능
        medicationBinding.fabAddMedication.setOnClickListener {
            toggleFab()
        }

        // 플로팅 버튼 클릭 이벤트 - 캡처
//        medicationBinding.fabEdit.setOnClickListener {
//
//        }

        // 플로팅 버튼 클릭 이벤트 - 공유
        medicationBinding.fabCapture.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    private fun toggleFab() {
        Toast.makeText(this.context, "메인 버튼 클릭!", Toast.LENGTH_SHORT).show()
        // 플로팅 액션 버튼 닫기 - 열려있는 플로팅 버튼 집어넣는 애니메이션
        if (isFabOpen) {
//            ObjectAnimator.ofFloat(medicationBinding.fabEdit, "translationY", 0f).apply { start() }
            ObjectAnimator.ofFloat(medicationBinding.fabCapture, "translationY", 0f)
                .apply { start() }
            ObjectAnimator.ofFloat(medicationBinding.fabAddMedication, View.ROTATION, 45f, 0f)
                .apply { start() }
        } else { // 플로팅 액션 버튼 열기 - 닫혀있는 플로팅 버튼 꺼내는 애니메이션
//            ObjectAnimator.ofFloat(medicationBinding.fabEdit, "translationY", -360f).apply { start() }
            ObjectAnimator.ofFloat(medicationBinding.fabCapture, "translationY", -180f)
                .apply { start() }
            ObjectAnimator.ofFloat(medicationBinding.fabAddMedication, View.ROTATION, 0f, 45f)
                .apply { start() }
            medicationBinding.fabCapture.isClickable = true
        }

        isFabOpen = !isFabOpen


    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(context, CaptureActivity::class.java)
        this.startActivity(takePictureIntent)
    }
}