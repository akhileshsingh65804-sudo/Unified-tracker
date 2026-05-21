package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Email::class, Order::class, Ticket::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao
    abstract fun orderDao(): OrderDao
    abstract fun ticketDao(): TicketDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unified_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
