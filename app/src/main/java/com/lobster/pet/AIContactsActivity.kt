package com.lobster.pet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lobster.pet.ai.AIChatManager
import com.lobster.pet.data.db.ChatContact
import kotlinx.coroutines.launch

/**
 * AI 代聊联系人管理界面
 */
class AIContactsActivity : AppCompatActivity() {
    
    private lateinit var aiChatManager: AIChatManager
    private lateinit var adapter: ContactAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_contacts)
        
        aiChatManager = AIChatManager(this)
        
        // 设置标题
        supportActionBar?.title = "🤖 AI 代聊管理"
        
        // 初始化视图
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        
        // 设置 RecyclerView
        adapter = ContactAdapter(
            onToggleEnabled = { contact, enabled ->
                toggleContact(contact, enabled)
            },
            onEditClick = { contact ->
                showEditDialog(contact)
            },
            onDeleteClick = { contact ->
                deleteContact(contact)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // API Key 设置按钮
        findViewById<Button>(R.id.btnSetApiKey).setOnClickListener {
            showApiKeyDialog()
        }
        
        // 添加联系人按钮
        findViewById<Button>(R.id.btnAddContact).setOnClickListener {
            showAddContactDialog()
        }
        
        // 加载联系人
        loadContacts()
    }
    
    private fun loadContacts() {
        lifecycleScope.launch {
            aiChatManager.getAllContacts().collect { contacts ->
                adapter.submitList(contacts)
                emptyView.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun toggleContact(contact: ChatContact, enabled: Boolean) {
        lifecycleScope.launch {
            aiChatManager.toggleContactEnabled(contact.contactId, enabled)
        }
    }
    
    private fun showEditDialog(contact: ChatContact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etPrompt = dialogView.findViewById<EditText>(R.id.etPrompt)
        
        etName.setText(contact.contactName)
        etPrompt.setText(contact.systemPrompt)
        
        AlertDialog.Builder(this)
            .setTitle("编辑联系人")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                lifecycleScope.launch {
                    val updated = contact.copy(
                        contactName = etName.text.toString(),
                        systemPrompt = etPrompt.text.toString()
                    )
                    aiChatManager.saveContact(updated)
                    Toast.makeText(this@AIContactsActivity, "已保存", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteContact(contact: ChatContact) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("删除 ${contact.contactName} 的代聊记录？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    aiChatManager.deleteContact(contact)
                    Toast.makeText(this@AIContactsActivity, "已删除", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etPrompt = dialogView.findViewById<EditText>(R.id.etPrompt)
        
        AlertDialog.Builder(this)
            .setTitle("添加联系人")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val name = etName.text.toString()
                if (name.isBlank()) {
                    Toast.makeText(this, "请输入联系人名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                lifecycleScope.launch {
                    val contactId = "manual_${name.hashCode()}_${System.currentTimeMillis()}"
                    val contact = ChatContact(
                        contactId = contactId,
                        contactName = name,
                        systemPrompt = etPrompt.text.toString(),
                        isEnabled = true
                    )
                    aiChatManager.saveContact(contact)
                    Toast.makeText(this@AIContactsActivity, "已添加", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showApiKeyDialog() {
        val prefs = getSharedPreferences("lobster_prefs", MODE_PRIVATE)
        val currentKey = prefs.getString("glm_api_key", "")
        
        val et = EditText(this).apply {
            setText(currentKey)
            hint = "请输入智谱 AI API Key"
        }
        
        AlertDialog.Builder(this)
            .setTitle("设置 API Key")
            .setView(et)
            .setPositiveButton("保存") { _, _ ->
                prefs.edit().putString("glm_api_key", et.text.toString()).apply()
                Toast.makeText(this, "API Key 已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 联系人列表适配器
     */
    class ContactAdapter(
        private val onToggleEnabled: (ChatContact, Boolean) -> Unit,
        private val onEditClick: (ChatContact) -> Unit,
        private val onDeleteClick: (ChatContact) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {
        
        private var contacts: List<ChatContact> = emptyList()
        
        fun submitList(list: List<ChatContact>) {
            contacts = list
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contact, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(contacts[position])
        }
        
        override fun getItemCount() = contacts.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: TextView = itemView.findViewById(R.id.tvName)
            private val tvPrompt: TextView = itemView.findViewById(R.id.tvPrompt)
            private val switchEnabled: Switch = itemView.findViewById(R.id.switchEnabled)
            private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
            private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
            
            fun bind(contact: ChatContact) {
                tvName.text = contact.contactName
                tvPrompt.text = contact.systemPrompt.takeIf { it.isNotBlank() } ?: "使用默认人设"
                switchEnabled.isChecked = contact.isEnabled
                
                switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    onToggleEnabled(contact, isChecked)
                }
                
                btnEdit.setOnClickListener { onEditClick(contact) }
                btnDelete.setOnClickListener { onDeleteClick(contact) }
            }
        }
    }
}
