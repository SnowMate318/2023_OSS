package com.prac.ai.fragments

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prac.ai.databinding.FragmentMessengerBinding
import com.prac.ai.databinding.ItemChatbotMessageBinding
import com.prac.ai.databinding.ItemUserMessageBinding
import java.text.SimpleDateFormat
import java.util.*

data class Message(val content: String, val created_at: Timestamp, var user: Boolean){

    constructor() : this("", Timestamp.now(), true)


}
class MessageAdapter(private val messages: MutableList<Message>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    companion object {
        const val VIEW_TYPE_MESSAGE_SENT = 1
        const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.user) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }
    inner class UserMessageViewHolder(private val binding: ItemUserMessageBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(message: Message){
            binding.apply{
                binding.userMessageContent.text = message.content
                val time = message.created_at.toDate()
                binding.messageTime.text = SimpleDateFormat("a h:mm", Locale.KOREAN).format(time)
            }
        }
    }

    inner class ChatbotMessageViewHolder(private val binding: ItemChatbotMessageBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(message: Message){
            binding.apply{
                binding.messageContent.text = message.content
                val time = message.created_at.toDate()
                binding.messageTime.text = SimpleDateFormat("a h:mm", Locale.KOREAN).format(time)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            VIEW_TYPE_MESSAGE_SENT ->{
                val binding = ItemUserMessageBinding.inflate(LayoutInflater.from(parent.context),parent,false)
                UserMessageViewHolder(binding)
            }
            VIEW_TYPE_MESSAGE_RECEIVED ->{
                val binding = ItemChatbotMessageBinding.inflate(LayoutInflater.from(parent.context),parent,false)
                ChatbotMessageViewHolder(binding)
            }
            else -> throw RuntimeException("Error")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as UserMessageViewHolder).bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ChatbotMessageViewHolder).bind(message)
        }
    }


    override fun getItemCount(): Int {
        return messages.size
    }

}

class MessengerFragment : Fragment() {

    private lateinit var registration: ListenerRegistration
    private lateinit var user: FirebaseUser
    private lateinit var db: FirebaseFirestore
    private lateinit var uid:String
    private var messages = mutableListOf<Message>(

    )

    lateinit var fragmentMessengerBinding: FragmentMessengerBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentMessengerBinding = FragmentMessengerBinding.inflate(layoutInflater,container,false)
        return fragmentMessengerBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = Firebase.auth.currentUser!!
        db = Firebase.firestore
        uid = user.uid
        fragmentMessengerBinding.messagesRecyclerView.apply{
            adapter = MessageAdapter(messages)
            layoutManager = LinearLayoutManager(context)
        }
        view.setOnTouchListener { v, event ->
            hideKeyboard()
            false // 이벤트를 더 이상 전파하지 않습니다.
        }
        registration = db.collection("User")
            .document(uid)
            .collection("Message")
            .orderBy("created_at")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }
                for (dc in snapshots!!.documentChanges) {
//                    print(messages)
                    val newMessage = dc.document.toObject(Message::class.java)

                    messages.add(newMessage)
                    print(messages)
                    fragmentMessengerBinding.messagesRecyclerView.adapter?.notifyItemInserted(
                        messages.size - 1
                    )
                    fragmentMessengerBinding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)

                }
            }
        SendButtonClickEvent()
    }
    private fun sendMessage(text: String): HashMap<String, Any> {

            fun getCurrentKoreanTimestamp(): Timestamp {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
                return Timestamp(calendar.time)
            }

        Message(text, getCurrentKoreanTimestamp(), true)
        print(getCurrentKoreanTimestamp())
        return hashMapOf(
            "content" to text,
            "created_at" to getCurrentKoreanTimestamp(),
            "user" to true
        )
        //서버 전송 메소드 추가하기
    }
    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
    private fun SendButtonClickEvent() {
        fragmentMessengerBinding.sendButton.setOnClickListener() {
            val text: String = fragmentMessengerBinding.messageInput.text.toString()
            if (text.isNotEmpty()) {
                db.collection("User").document(uid).collection("Message").add(sendMessage(text))
                Toast.makeText(this.context, uid, Toast.LENGTH_SHORT).show()
                fragmentMessengerBinding.messageInput.text = null
                fragmentMessengerBinding.messagesRecyclerView.smoothScrollToPosition(messages.size)

                hideKeyboard()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        registration.remove()

    }
}