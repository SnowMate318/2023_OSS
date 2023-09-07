import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.prac.ai.databinding.FragmentAlarmPopupDialogBinding
import com.prac.ai.databinding.FragmentAlarmSettingBinding
import com.prac.ai.models.Alarm

class AlarmPopupDialogFragment : DialogFragment() {
    private lateinit var alarmPopupDialogBinding: FragmentAlarmPopupDialogBinding
    companion object {
        private const val ARG_ALARM = "alarm"
        fun newInstance(alarm: Alarm): AlarmPopupDialogFragment {
            val args = Bundle()
            args.putParcelable(ARG_ALARM, alarm)

            val fragment = AlarmPopupDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        alarmPopupDialogBinding = FragmentAlarmPopupDialogBinding.inflate(layoutInflater,container,false)
        return alarmPopupDialogBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val alarm = arguments?.getParcelable<Alarm>(ARG_ALARM)
        // 이제 alarm 객체를 사용하여 필요한 작업을 수행할 수 있습니다.
    }
}