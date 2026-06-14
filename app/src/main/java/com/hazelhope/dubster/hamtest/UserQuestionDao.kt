package com.hazelhope.dubster.hamtest

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserQuestionDao {
    @Query("SELECT * FROM question_info WHERE pool = :pool ORDER BY score ASC")
    fun getAll(pool: String): List<UserQuestionInfo>

    @Query("SELECT * FROM question_info WHERE pool = :pool AND score < 0")
    fun getAmountOfScoresLessThanZero(pool: String): List<UserQuestionInfo>

    @Query("SELECT * FROM question_info WHERE id IN (:ids)")
    fun loadAllByIds(ids: List<String>): List<UserQuestionInfo>

    @Query("UPDATE question_info SET score = :score, firstTime = 0 WHERE id = :id")
    fun updateScore(id: String, score: Int)

    @Insert
    fun insertAll(vararg questionInfo: UserQuestionInfo)

    @Delete
    fun delete(questionInfo: UserQuestionInfo)
}