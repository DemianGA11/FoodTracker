package com.example.foodtracker.modelos

// AlimentoDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface AlimentoDao {
    // Insertar
    @Insert
    suspend fun insert(alimento: Alimento)

    // Actualizar
    @Update
    suspend fun update(alimento: Alimento)

    // Eliminar
    @Delete
    suspend fun delete(alimento: Alimento)

    // Consultas
    @Query("SELECT * FROM alimentos ORDER BY fechaCaducidad ASC")
    fun getAllAlimentos(): Flow<List<Alimento>> // Flujo reactivo para la UI

    @Query("SELECT * FROM alimentos WHERE fechaCaducidad BETWEEN :hoy AND :limite")
    suspend fun getAlimentosProximosACaducar(hoy: String, limite: String): List<Alimento>


    @Query("SELECT * FROM alimentos WHERE id = :id")
    suspend fun getAlimentoById(id: Int): Alimento?
    @Query("SELECT * FROM alimentos")
    suspend fun getAllForStats(): List<Alimento>

}