package com.example.foodtracker.modelos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {

    @Insert
    suspend fun insert(notificacion: Notificacion)

    @Query("SELECT * FROM notificaciones ORDER BY id DESC")
    fun getTodas(): Flow<List<Notificacion>>

    @Query("DELETE FROM notificaciones WHERE mensaje LIKE '%' || :nombre || '%'")
    suspend fun borrarNotificacionesDe(nombre: String)
}
