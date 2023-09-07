package com.prac.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.prac.ai.databinding.ActivityMessengerBinding
import com.prac.ai.fragments.*
import com.prac.ai.models.Alarm

data class Alarm(
    var hour: Int,
    var minute: Int,
    var alarmName: String,
    var alarmTitle: String,
    val alarmContent: String,
    var isEnable: Boolean,
    val alarm_num: Int
)
class DrawerActivity : AppCompatActivity() {



    var alarms = mutableListOf<Alarm>(
        Alarm(8, 0, "아침", "아침안부", "약은 잘 드셨나요?", true, 1),
        Alarm(13, 0, "점심", "점심안부", "점심은 잘 드셨나요?", true, 2),
        Alarm(20, 0, "저녁", "저녁안부", "저녁은 잘 드셨나요?", true, 3),
    )
    val size = alarms.size

    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var context: Context
    private lateinit var drawerbinding: ActivityMessengerBinding
    private lateinit var GoogleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        val shouldOpenAlarmSettingFragment = intent.getBooleanExtra("openAlarmSettingFragment", false)
        if (shouldOpenAlarmSettingFragment) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, AlarmSettingFragment()).commit()
        }
        drawerbinding= ActivityMessengerBinding.inflate(layoutInflater)

        val googleFirstName = intent.getStringExtra("google_first_name")
        val googleLastName = intent.getStringExtra("google_last_name")
        val googleEmail = intent.getStringExtra("google_email")
        val googleProfilePicURL = intent.getStringExtra("google_profile_pic_url")
        val name = "${googleLastName}${googleFirstName}"

        val navigationBinding: NavigationView = drawerbinding.navigationView
        val menu = navigationBinding.menu
        val headerView = navigationBinding.getHeaderView(0)

        val infoButton: Button = headerView.findViewById(R.id.infoButton)
        val moreButton: Button = headerView.findViewById(R.id.moreButton)
        val textView: TextView = headerView.findViewById(R.id.textView)
        val textView2: TextView = headerView.findViewById(R.id.textView2)
        val imageView: ImageView = headerView.findViewById(R.id.imageView)

        textView.text = name
        textView2.text = googleEmail

        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("106228442872-fsdg14luol2lc9hvhq4sb8r4dk6045vq.apps.googleusercontent.com")
                .requestEmail()
                .build()

        GoogleSignInClient = GoogleSignIn.getClient(this, gso)

        infoButton.setOnClickListener {
            // 버튼이 클릭되었을 때 수행할 작업
            var myIntent: Intent
            myIntent = Intent(this, LoginActivity::class.java)
            myIntent.putExtra("google_first_name", googleFirstName)
            myIntent.putExtra("google_last_name", googleLastName)
//                myIntent.putExtra("google_id", googleId)
            myIntent.putExtra("google_email", googleEmail)
            myIntent.putExtra("google_profile", googleProfilePicURL)
//                myIntent.putExtra("google_id_token", googleIdToken)
            this.startActivity(myIntent)
            Toast.makeText(context, "$title: 정보를 수정합니다.", Toast.LENGTH_SHORT).show()
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }
        moreButton.setOnClickListener {
            // 버튼이 클릭되었을 때 수행할 작업
            Toast.makeText(context, "$title: 복용 약 정보를 조회합니다.", Toast.LENGTH_SHORT).show()
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, MedicationFragment()).commit()
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
            MessengerFragment()
        ).commit()
        setContentView(drawerbinding.root)


        // 모든 알람 메뉴 아이템을 먼저 숨김
        for (i in 1..5) {
            menu.findItem(resources.getIdentifier("alarm$i", "id", packageName)).isVisible = false
        }

        // alarms 리스트의 사이즈에 맞게 메뉴 아이템을 보이게 함
        for (i in 1..size) {
            val index = i - 1
            var time = ""
            if(alarms[index].hour>=10) time += alarms[index].hour.toString()
            else time += "0"+alarms[index].hour.toString()
            time+=":"
            if(alarms[index].minute>=10) time += alarms[index].minute.toString()
            else time += "0"+alarms[index].minute.toString()

            menu.findItem(resources.getIdentifier("alarm$i", "id", packageName)).isVisible = true
            menu.findItem(resources.getIdentifier("alarm$i", "id", packageName)).title = "${alarms[index].alarm_name}    ${time}"
            val switchCompat = menu.findItem(resources.getIdentifier("alarm$i", "id", packageName)).actionView as SwitchCompat
            switchCompat.isChecked = alarms[index].enable
            switchCompat.setOnCheckedChangeListener { _, isChecked ->
                alarms[index].enable = isChecked
            }
        }


        val toolbar: Toolbar = drawerbinding.toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false) // 기존 title 지우기
        actionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 만들기
        actionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24) // 뒤로가기 버튼 이미지 지정

        mDrawerLayout = findViewById(R.id.drawer_layout)
        context = this

        val navigationView: NavigationView = drawerbinding.navigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->

            mDrawerLayout.closeDrawers()

            val id = menuItem.itemId
            val title = menuItem.title.toString()

            val shouldCloseDrawer = when (id) {
//                R.id.info -> {
//                    menuItem.isChecked = true
//                    Toast.makeText(context, "$title: 상세 정보를 확인합니다.", Toast.LENGTH_SHORT).show()
//                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
//                        InfoFragment()
//                    ).commit()
//                    true
//                }
                R.id.messenger -> {
                    menuItem.isChecked = true
                    Toast.makeText(context, "$title: 메신저 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
                        MessengerFragment()
                    ).commit()
                    true
                }
                R.id.alarm -> {
                    menuItem.isChecked = true
                    Toast.makeText(context, "$title: 알람을 설정합니다.", Toast.LENGTH_SHORT).show()
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_container, AlarmSettingFragment()).commit()
                    true
                }
                R.id.logout -> {
                    Toast.makeText(context, "$title: 로그아웃 시도중", Toast.LENGTH_SHORT).show()
                    signOut()
                    true
                }
                else -> {
                    false
                }

            }
            if(shouldCloseDrawer){
                mDrawerLayout.closeDrawer(GravityCompat.START)
            }

            shouldCloseDrawer
        }
    }

    private fun signOut() {
        GoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                this.startActivity(Intent(this, MainActivity::class.java))
                mDrawerLayout.closeDrawer(GravityCompat.START)
            }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { // 왼쪽 상단 버튼 눌렀을 때
                mDrawerLayout.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}