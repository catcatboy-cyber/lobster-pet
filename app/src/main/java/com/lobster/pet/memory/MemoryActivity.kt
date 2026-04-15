package com.lobster.pet.memory

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lobster.pet.R
import kotlinx.coroutines.launch

/**
 * 记忆管理界面
 * 查看和编辑龙虾记住的内容
 */
class MemoryActivity : AppCompatActivity() {

    private lateinit var memoryManager: MemoryManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MemoryAdapter
    private lateinit var emptyView: TextView
    private lateinit var tvUserName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory)
        
        memoryManager = MemoryManager(this)
        
        supportActionBar?.title = "🧠 我的记忆"
        
        // 初始化视图
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        tvUserName = findViewById(R.id.tvUserName)
        
        // 设置RecyclerView
        adapter = MemoryAdapter(
            onDeleteClick = { memory ->
                deleteMemory(memory)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // 设置名字按钮
        findViewById<Button>(R.id.btnSetName).setOnClickListener {
            showSetNameDialog()
        }
        
        // 添加记忆按钮
        findViewById<Button>(R.id.btnAddMemory).setOnClickListener {
            showAddMemoryDialog()
        }
        
        // 加载数据
        loadMemories()
        loadUserName()
    }

    private fun loadUserName() {
        val name = memoryManager.getUserName()
        tvUserName.text = if (name != null) "龙虾记得你的名字：$name" else "告诉龙虾你的名字"
    }

    private fun loadMemories() {
        lifecycleScope.launch {
            val memories = memoryManager.getRecentMemories(50)
            adapter.submitList(memories)
            
            if (memories.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun deleteMemory(memory: UserMemory) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("删除这条记忆？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    // 通过重新加载来刷新
                    loadMemories()
                    Toast.makeText(this@MemoryActivity, "已删除", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSetNameDialog() {
        val currentName = memoryManager.getUserName() ?: ""
        
        val editText = EditText(this).apply {
            setText(currentName)
            hint = "你的名字"
        }
        
        AlertDialog.Builder(this)
            .setTitle("设置名字")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    memoryManager.setUserName(name)
                    loadUserName()
                    Toast.makeText(this, "名字已保存", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddMemoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_memory, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        
        // 设置类型选择器
        val types = MemoryType.values().map { it.name }
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        
        AlertDialog.Builder(this)
            .setTitle("添加记忆")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val content = etContent.text.toString().trim()
                if (content.isNotEmpty()) {
                    val type = MemoryType.values()[spinnerType.selectedItemPosition]
                    memoryManager.rememberFact(content, type)
                    Toast.makeText(this, "已记住", Toast.LENGTH_SHORT).show()
                    loadMemories()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 记忆列表适配器
     */
    class MemoryAdapter(
        private val onDeleteClick: (UserMemory) -> Unit
    ) : RecyclerView.Adapter<MemoryAdapter.ViewHolder>() {

        private var memories: List<UserMemory> = emptyList()

        fun submitList(list: List<UserMemory>) {
            memories = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_memory, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(memories[position])
        }

        override fun getItemCount() = memories.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvType: TextView = itemView.findViewById(R.id.tvType)
            private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
            private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

            fun bind(memory: UserMemory) {
                tvType.text = getTypeEmoji(memory.type)
                tvContent.text = memory.content
                btnDelete.setOnClickListener { onDeleteClick(memory) }
            }

            private fun getTypeEmoji(type: MemoryType): String {
                return when (type) {
                    MemoryType.LIKES -> "❤️ 喜欢"
                    MemoryType.DISLIKES -> "💔 讨厌"
                    MemoryType.HABIT -> "🔄 习惯"
                    MemoryType.WORK -> "💼 工作"
                    MemoryType.FAMILY -> "👨‍👩‍👧 家庭"
                    MemoryType.FRIEND -> "👫 朋友"
                    MemoryType.MOOD -> "😊 心情"
                    MemoryType.GOAL -> "🎯 目标"
                    MemoryType.RANDOM -> "📝 其他"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        memoryManager.cleanup()
    }
}
