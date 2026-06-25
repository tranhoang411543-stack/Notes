package com.fini.todoapp.data.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "categories")
data class LocalCategory(
    @PrimaryKey val id: String,
    val name: String,
    val color: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val deleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val version: Int = 0
)

@Entity(tableName = "tasks")
data class LocalTask(
    @PrimaryKey val id: String,
    val categoryId: String?,
    val title: String,
    val note: String?,
    val completed: Boolean,
    val priority: Boolean,
    val notifyAt: String?,
    val notifyTime: String?,
    val autoTrashAfterNotification: Boolean,
    val dueAt: String?,
    val reminderEnabled: Boolean,
    val repeatType: String,
    val repeatDays: String?,
    val hasLocation: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val address: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val trashedAt: String?,
    val purgeAfter: String?,
    val deleted: Boolean = false,
    val syncStatus: String = "SYNCED",
    val version: Int = 0
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deleted = 0")
    suspend fun getAllActive(): List<LocalCategory>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<LocalCategory>

    @Query("SELECT * FROM categories WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsynced(): List<LocalCategory>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): LocalCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: LocalCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<LocalCategory>)

    @Update
    suspend fun update(category: LocalCategory)

    @Delete
    suspend fun delete(category: LocalCategory)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deleted = 0 AND trashedAt IS NULL")
    suspend fun getAllActive(): List<LocalTask>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<LocalTask>

    @Query("SELECT * FROM tasks WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsynced(): List<LocalTask>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): LocalTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: LocalTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<LocalTask>)

    @Update
    suspend fun update(task: LocalTask)

    @Delete
    suspend fun delete(task: LocalTask)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}

@Database(entities = [LocalCategory::class, LocalTask::class], version = 1, exportSchema = false)
abstract class FiniDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: FiniDatabase? = null

        fun getDatabase(context: Context): FiniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FiniDatabase::class.java,
                    "fini_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
