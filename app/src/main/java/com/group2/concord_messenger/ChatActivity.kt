package com.group2.concord_messenger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.group2.concord_messenger.model.ChatMessageListAdapter
import com.group2.concord_messenger.model.MessageContent

class ChatActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var sendBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        editText = findViewById(R.id.edit_gchat_message)
        sendBtn = findViewById(R.id.button_gchat_send)

        val messageList = ArrayList<MessageContent>()
        val messageRecycler: RecyclerView = findViewById(R.id.recycler_gchat)
        val messageAdapter = ChatMessageListAdapter(messageList)
        messageRecycler.layoutManager = LinearLayoutManager(this)
        messageRecycler.adapter = messageAdapter

        sendBtn.setOnClickListener {
            messageList.add(MessageContent(editText.text.toString(), null, 0))
            editText.text.clear()
            messageAdapter.notifyItemInserted(messageList.size - 1)
        }
    }
}