package com.prac.ai

import android.Manifest
import com.prac.ai.databinding.ActivityMainBinding
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var GoogleSignInClient: GoogleSignInClient
    private var user: FirebaseUser? = null
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var uid:String? = null
    lateinit var binding: ActivityMainBinding;
    var account: GoogleSignInAccount? = null
    var googleFirstName = ""
    var googleLastName = ""
    var googleEmail = ""
    var googleProfilePicURL = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("106228442872-fsdg14luol2lc9hvhq4sb8r4dk6045vq.apps.googleusercontent.com")
                .requestEmail()
                .build()

        GoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.loginbutton.setOnClickListener {
            signIn()
        }

        val permission1 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA)
        val permission2 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (permission1 == PackageManager.PERMISSION_DENIED || permission2 == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1000)
            }
        }else{
            if (permission1 == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(
                        arrayOf(Manifest.permission.CAMERA),
                        1000)
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Get the results for READ_PHONE_STATE permission
        if (requestCode == 1000) {
            var checkResult = true

            // Check if all permissions are granted
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false
                    break
                }
            }

            // Finish the app if permissions are not granted
            if (checkResult) {

            } else {
                finish()
            }
        }
    }
    private fun signIn() {
        val signInIntent = GoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        GoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                // Update your UI here
            }
    }

    private fun revokeAccess() {
        GoogleSignInClient.revokeAccess()
            .addOnCompleteListener(this) {
                // Update your UI here
            }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account!!.id)

                googleFirstName = account?.givenName ?: ""
                Log.i("Google First Name", googleFirstName)

                googleLastName = account?.familyName ?: ""
                Log.i("Google Last Name", googleLastName)

                googleEmail = account?.email ?: ""
                Log.i("Google Email", googleEmail)
//
                googleProfilePicURL = account?.photoUrl.toString()
                Log.i("Google Profile Pic URL", googleProfilePicURL)
//
//                val googleIdToken = account?.idToken ?: ""
//                Log.i("Google ID Token", googleIdToken)
//
//                val googleId = account?.id ?: ""
//                Log.i("Google ID", googleId)


                user = Firebase.auth.currentUser!!
                auth = Firebase.auth
                db = Firebase.firestore
                uid = user!!.uid
                var max:Int = 0;
                val docRefUser = db!!.collection("User").document(uid!!)
                val mediCollection = docRefUser.collection("Medication").whereEqualTo("enable", true)
                var regMedi = 0

                docRefUser.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                        max = (document.get("max_oneday") as? Int) ?: 0
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
                mediCollection.get()
                    .addOnSuccessListener { documents ->
                        var curMedi = 0
                        var currentMax = 0
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
                        val currentDate = calendar.time

                        for (document in documents) {
                            val endDate = document.get("end_date") as? com.google.firebase.Timestamp
                            if (endDate != null && endDate.toDate().before(currentDate)) {
                                // end_date가 현재 시간보다 과거라면 enable 필드를 false로 업데이트
                                document.reference.update("enable", false)
                            } else if (endDate != null) {
                                curMedi = curMedi + 1
                                if(max>5) currentMax = 5
                                else if(currentMax<max) currentMax = max
                            }
                        }
                        if(curMedi!=regMedi){
                            docRefUser.update("max_medi",currentMax)
                            docRefUser.update("cur_medi", curMedi)
                            changeAlarm()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents: ", exception)
                    }



                var myIntent: Intent
                myIntent = Intent(this, LoginActivity::class.java)
                myIntent.putExtra("openAlarmSettingFragment", false)
                myIntent.putExtra("google_first_name", googleFirstName)
                myIntent.putExtra("google_last_name", googleLastName)
//                myIntent.putExtra("google_id", googleId)
                myIntent.putExtra("google_email", googleEmail)

                myIntent.putExtra("google_profile_pic_url", googleProfilePicURL)
//                myIntent.putExtra("google_id_token", googleIdToken)
                this.startActivity(myIntent)
                firebaseAuthWithGoogle(account!!.idToken!!)
            } catch (e: ApiException) {
                Log.e(
                    "failed code=", e.statusCode.toString()
                )
            }
        }
    }

    private fun changeAlarm() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("알림 변경")
        builder.setMessage("복용약이 추가되었습니다. 알림설정을 변경하시겠습니까?")

        // "예" 버튼 클릭 시의 리스너
        builder.setPositiveButton("예") { dialog, which ->
            // 여기에 "예"를 클릭했을 때 수행할 작업을 작성합니다.
            val intent = Intent(this, DrawerActivity::class.java)
            intent.putExtra("openAlarmSettingFragment", false)
            intent.putExtra("google_first_name", googleFirstName)
            intent.putExtra("google_last_name", googleLastName)
//                myIntent.putExtra("google_id", googleId)
            intent.putExtra("google_email", googleEmail)

            intent.putExtra("google_profile_pic_url", googleProfilePicURL)
//                myIntent.putExtra("google_id_token", googleIdToken)
            this.startActivity(intent)
        }

        // "아니오" 버튼 클릭 시의 리스너
        builder.setNegativeButton("아니오") { dialog, which ->
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth!!.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth!!.currentUser
                    updateUI(user)

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        Log.i("taehyun", "updateUI")
    }



    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

}