package com.prac.ai

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.prac.ai.databinding.ActivityLoginBinding
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import com.prac.ai.models.Alarm
data class User(val name:String?, val gender:String?, val birthDate:String?, val age:Int?, val significant:String?)
class LoginActivity:AppCompatActivity() {

    lateinit var loginbinding: ActivityLoginBinding
    private val user = Firebase.auth.currentUser
    private val db = Firebase.firestore
    var uid:String? = null
    var myinfo:User? = null
    var hasdata:Boolean = false
    lateinit var docRef: DocumentReference
    val alarms = mutableListOf(
    Alarm(0,0,"시간1", "시간1 안부","약은 잘 드셨나요?",false, 1),
    Alarm(0,10,"시간2","시간2 안부","약은 잘 드셨나요?",false,2),
    Alarm(0,20,"시간3", "시간3 안부","약은 잘 드셨나요?",false,3),
    Alarm(0,30,"시간4", "시간4 안부","약은 잘 드셨나요?",false,4),
    Alarm(0,40,"시간5", "시간5 안부","약은 잘 드셨나요?",false,5)
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginbinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(loginbinding.root)
        if(user!=null) {
            val uid = user.uid
            docRef = db.collection("User").document(uid)
            val alarmRef = docRef.collection("Alarm")
            docRef.get().addOnCompleteListener{ task ->
            if(task.isSuccessful) {
                val document = task.result
                if (!document.exists()) {
                    val userData = hashMapOf(
                        "name" to user.displayName,
                        "email" to user.email,
                        "reg_medi" to 0,
                        "max_oneday" to 0,

                        )
                    docRef.set(userData)
                        .addOnSuccessListener {
                            println("User document added successfully.")
                        }
                        .addOnFailureListener { e ->
                            println("Error adding user document: $e")
                        }

                }
            }
            }
            for (alarm in alarms) {
                alarmRef.add(alarm)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }
            }
        }


//        docRef.get()
//            .addOnSuccessListener { document ->
//                if (document != null) {
//                    myinfo = document.toObject(User::class.java)
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.d(TAG, "get failed with ", exception)
//            }

        val googleFirstName = intent.getStringExtra("google_first_name")
        val googleLastName = intent.getStringExtra("google_last_name")
        val googleEmail = intent.getStringExtra("google_email")
        val googleProfilePicURL = intent.getStringExtra("google_profile_pic_url")
        val name = "${googleLastName}${googleFirstName}"
        //    val googleId = intent.getStringExtra("google_id")
        //    val googleEmail = intent.getStringExtra("google_email")
        //
        //    val googleAccessToken = intent.getStringExtra("google_id_token")

        val googleIdTextView = loginbinding.textViewName
        googleIdTextView.text = name
        var gender = "남자"
        val datePicker = loginbinding.birthDatePicker
        val selectedDate = Calendar.getInstance()
        val day = datePicker.dayOfMonth
        val month = datePicker.month
        val year = datePicker.year
        selectedDate.set(year,month,day)
        val stringDate = "${year}-${month + 1}-${day}"
        val radioGroup =loginbinding.group
        val radioMan = loginbinding.radioMan
        val radioWoman = loginbinding.radioWoman


        loginbinding.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            loginbinding.root.requestFocus()
            false
        }

        if(myinfo==null) radioMan.isChecked = true
        else if(myinfo!!.name == null || myinfo!!.gender == null || myinfo!!.birthDate == null || myinfo!!.age == null) radioMan.isChecked = true
        else if(myinfo!!.gender == "남자") {
            radioMan.isChecked = true
            hasdata = true
        }
        else {
            radioWoman.isChecked = true
            hasdata = true
        }

        if(hasdata){
            val dateString = myinfo!!.birthDate.toString()

            val dateParts = dateString.split("-")

            val tmp_year: Int = dateParts[0].toInt()
            val tmp_month: Int = dateParts[1].toInt() -1
            val tmp_day: Int = dateParts[2].toInt()
            datePicker.updateDate(tmp_year, tmp_month, tmp_day)
        }
        else datePicker.updateDate(1950, 0, 1)

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioMan -> {
                    gender = "남자"
                    radioMan.isChecked = true
                }
                R.id.radioWoman -> {
                    gender = "여자"
                    radioWoman.isChecked=true
                }
            }
        }


        loginbinding.navbutton.setOnClickListener(){
            val DrawerIntent = Intent(this, DrawerActivity::class.java)
            DrawerIntent.putExtra("google_first_name", googleFirstName)
            DrawerIntent.putExtra("google_last_name", googleLastName)
            DrawerIntent.putExtra("google_email", googleEmail)
            DrawerIntent.putExtra("google_profile", googleProfilePicURL)
            this.startActivity(DrawerIntent)
            val age = calculateAge(year,month,day)
            val significant:String = loginbinding.editTextSignificant.text.toString()?:"없음"
                val user = User(name, gender, stringDate, age, significant)
                if(uid!=null)db.collection("User").document(uid!!).set(updateUser(user), SetOptions.merge())
                //서버에 user 저장
                if(hasdata) {
                    if(myinfo!!.significant!=null){
                        loginbinding.editTextSignificant.setText(myinfo!!.significant)
                    }
                    else loginbinding.editTextSignificant.text = null
                }
                else loginbinding.editTextSignificant.text = null
        }


    }
    fun calculateAge(year: Int, month: Int, day: Int): Int {
        val today = Calendar.getInstance()
        val birthDate = Calendar.getInstance()
        birthDate.set(year, month, day)
        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }
    private fun updateUser(user:User): HashMap<String, Any?> {
        return hashMapOf(
            "name" to user.name,
            "gender" to user.gender,
            "birth_date" to user.birthDate,
            "age" to user.age,
            "significant" to user.significant
        )
    }
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}