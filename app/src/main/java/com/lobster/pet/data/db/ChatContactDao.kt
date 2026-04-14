package com.lobster.pet.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 联系人数据访问对象
 */
@Dao
interface ChatContactDao {
    @Query("SELECT * FROM chat_contacts")
    fun getAll(): Flow<List<ChatContact>>
    
    @Query("SELECT * FROM chat_contacts WHERE contactId = :id")
    suspend fun getById(id: String): ChatContact?
    
    @Query("SELECT * FROM chat_contacts WHERE isEnabled = 1")
    suspend fun getEnabled(): List<ChatContact>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ChatContact)
    
    @Update
    suspend fun update(contact: ChatContact)
    
    @Delete
    suspend fun delete(contact: ChatContact)
}
