package com.prac.ai.models

import android.os.Parcelable
import com.prac.ai.AlarmChange
import kotlinx.parcelize.Parcelize

@Parcelize
data class Alarm(
    var hour: Int,
    var minute: Int,
    var alarm_name: String,
    var alarm_title: String,
    var alarm_content: String,
    var enable: Boolean,
    val alarm_num: Int
) : Parcelable
