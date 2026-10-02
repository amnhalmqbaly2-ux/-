package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AssessmentRecordEntity
import com.example.data.model.ChildProfileEntity
import com.example.data.model.CustomStoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VillageDao {
    @Query("SELECT * FROM child_profiles ORDER BY updatedAtEpoch DESC")
    fun observeAllProfiles(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM child_profiles ORDER BY updatedAtEpoch DESC")
    suspend fun getAllProfilesOnce(): List<ChildProfileEntity>

    @Query("SELECT * FROM child_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): ChildProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ChildProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<ChildProfileEntity>)

    @Query("SELECT * FROM assessment_records WHERE profileId = :profileId ORDER BY timestampEpoch DESC")
    fun observeAssessmentsForProfile(profileId: String): Flow<List<AssessmentRecordEntity>>

    @Query("SELECT * FROM assessment_records WHERE profileId = :profileId ORDER BY timestampEpoch DESC")
    suspend fun getAssessmentsForProfileOnce(profileId: String): List<AssessmentRecordEntity>

    @Query("SELECT * FROM assessment_records ORDER BY timestampEpoch DESC")
    fun observeAllAssessments(): Flow<List<AssessmentRecordEntity>>

    @Query("SELECT * FROM assessment_records ORDER BY timestampEpoch DESC")
    suspend fun getAllAssessmentsOnce(): List<AssessmentRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(record: AssessmentRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(records: List<AssessmentRecordEntity>)

    @Query("SELECT * FROM custom_stories ORDER BY createdAtEpoch DESC")
    fun observeCustomStories(): Flow<List<CustomStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomStory(story: CustomStoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomStories(stories: List<CustomStoryEntity>)
}

@Database(
    entities = [
        ChildProfileEntity::class,
        AssessmentRecordEntity::class,
        CustomStoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VillageDatabase : RoomDatabase() {
    abstract fun villageDao(): VillageDao

    companion object {
        @Volatile
        private var INSTANCE: VillageDatabase? = null

        fun getInstance(context: Context): VillageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VillageDatabase::class.java,
                    "omani_village_storybook.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
