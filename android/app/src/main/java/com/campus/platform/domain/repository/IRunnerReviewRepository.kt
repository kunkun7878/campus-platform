package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.RunnerReviewDto
import kotlinx.coroutines.flow.Flow

interface IRunnerReviewRepository {

    fun getReviewsByOrder(orderId: String): Flow<List<RunnerReviewDto>>

    fun getReviewsByReviewee(userId: String): Flow<List<RunnerReviewDto>>

    suspend fun createReview(review: RunnerReviewDto)
}
