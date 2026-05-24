package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.RunnerTaskEntity
import com.campus.platform.data.local.entity.RunnerOrderEntity
import com.campus.platform.data.local.entity.RunnerReviewEntity
import com.campus.platform.data.local.entity.AfterSaleEntity
import com.campus.platform.data.local.entity.RunnerApplicationEntity
import com.campus.platform.data.local.entity.OrderTimelineEntity
import com.campus.platform.data.local.entity.AfterSaleTimelineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunnerDao {

    // ── RunnerTask ───────────────────────────────────────────

    @Upsert
    suspend fun upsertTask(task: RunnerTaskEntity)

    @Upsert
    suspend fun upsertAllTasks(tasks: List<RunnerTaskEntity>)

    @Query("SELECT * FROM runner_tasks WHERE schoolId = :schoolId ORDER BY createdAt DESC")
    fun getTasksBySchoolId(schoolId: String): Flow<List<RunnerTaskEntity>>

    @Query("SELECT * FROM runner_tasks WHERE schoolId = :schoolId AND status = :status ORDER BY createdAt DESC")
    fun getTasksBySchoolAndStatus(schoolId: String, status: String): Flow<List<RunnerTaskEntity>>

    @Query("SELECT * FROM runner_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): RunnerTaskEntity?

    @Query("SELECT * FROM runner_tasks WHERE publisherId = :userId AND schoolId = :schoolId ORDER BY createdAt DESC")
    fun getTasksByPublisher(userId: String, schoolId: String): Flow<List<RunnerTaskEntity>>

    @Query("SELECT * FROM runner_tasks WHERE runnerId = :userId AND schoolId = :schoolId ORDER BY createdAt DESC")
    fun getTasksByRunner(userId: String, schoolId: String): Flow<List<RunnerTaskEntity>>

    @Query("DELETE FROM runner_tasks WHERE createdAt < :before")
    suspend fun deleteTasksOlderThan(before: String)

    @Query("DELETE FROM runner_tasks")
    suspend fun deleteAllTasks()

    // ── RunnerOrder ──────────────────────────────────────────

    @Upsert
    suspend fun upsertOrder(order: RunnerOrderEntity)

    @Upsert
    suspend fun upsertAllOrders(orders: List<RunnerOrderEntity>)

    @Query("SELECT * FROM runner_orders WHERE schoolId = :schoolId ORDER BY createdAt DESC")
    fun getOrdersBySchoolId(schoolId: String): Flow<List<RunnerOrderEntity>>

    @Query("SELECT * FROM runner_orders WHERE id = :id")
    suspend fun getOrderById(id: String): RunnerOrderEntity?

    @Query("SELECT * FROM runner_orders WHERE buyerId = :userId ORDER BY createdAt DESC")
    fun getOrdersByBuyer(userId: String): Flow<List<RunnerOrderEntity>>

    @Query("SELECT * FROM runner_orders WHERE runnerId = :userId AND schoolId = :schoolId ORDER BY createdAt DESC")
    fun getOrdersByRunner(userId: String, schoolId: String): Flow<List<RunnerOrderEntity>>

    @Query("SELECT * FROM runner_orders WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun getOrdersByTaskId(taskId: String): Flow<List<RunnerOrderEntity>>

    @Query("DELETE FROM runner_orders WHERE createdAt < :before")
    suspend fun deleteOrdersOlderThan(before: String)

    @Query("DELETE FROM runner_orders")
    suspend fun deleteAllOrders()

    // ── RunnerReview ─────────────────────────────────────────

    @Upsert
    suspend fun upsertReview(review: RunnerReviewEntity)

    @Upsert
    suspend fun upsertAllReviews(reviews: List<RunnerReviewEntity>)

    @Query("SELECT * FROM runner_reviews WHERE revieweeId = :userId ORDER BY createdAt DESC")
    fun getReviewsByReviewee(userId: String): Flow<List<RunnerReviewEntity>>

    @Query("SELECT * FROM runner_reviews WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun getReviewsByOrderId(orderId: String): Flow<List<RunnerReviewEntity>>

    @Query("SELECT * FROM runner_reviews WHERE reviewerId = :userId ORDER BY createdAt DESC")
    fun getReviewsByReviewer(userId: String): Flow<List<RunnerReviewEntity>>

    @Query("DELETE FROM runner_reviews WHERE createdAt < :before")
    suspend fun deleteReviewsOlderThan(before: String)

    @Query("DELETE FROM runner_reviews")
    suspend fun deleteAllReviews()

    // ── AfterSale ────────────────────────────────────────────

    @Upsert
    suspend fun upsertAfterSale(afterSale: AfterSaleEntity)

    @Upsert
    suspend fun upsertAllAfterSales(afterSales: List<AfterSaleEntity>)

    @Query("SELECT * FROM after_sales WHERE requesterId = :userId ORDER BY createdAt DESC")
    fun getAfterSalesByRequester(userId: String): Flow<List<AfterSaleEntity>>

    @Query("SELECT * FROM after_sales WHERE orderId = :orderId")
    fun getAfterSalesByOrderId(orderId: String): Flow<List<AfterSaleEntity>>

    @Query("SELECT * FROM after_sales WHERE id = :id")
    suspend fun getAfterSaleById(id: String): AfterSaleEntity?

    @Query("DELETE FROM after_sales WHERE createdAt < :before")
    suspend fun deleteAfterSalesOlderThan(before: String)

    @Query("DELETE FROM after_sales")
    suspend fun deleteAllAfterSales()

    // ── RunnerApplication ────────────────────────────────────

    @Upsert
    suspend fun upsertApplication(application: RunnerApplicationEntity)

    @Query("SELECT * FROM runner_applications WHERE userId = :userId AND schoolId = :schoolId")
    suspend fun getMyApplication(userId: String, schoolId: String): RunnerApplicationEntity?

    @Query("SELECT * FROM runner_applications WHERE userId = :userId")
    fun getApplicationsByUser(userId: String): Flow<List<RunnerApplicationEntity>>

    @Query("DELETE FROM runner_applications")
    suspend fun deleteAllApplications()

    // ── OrderTimeline ────────────────────────────────────────

    @Upsert
    suspend fun upsertTimelineEvent(event: OrderTimelineEntity)

    @Upsert
    suspend fun upsertAllTimelineEvents(events: List<OrderTimelineEntity>)

    @Query("SELECT * FROM order_timeline WHERE orderId = :orderId ORDER BY createdAt ASC")
    fun getTimelineByOrderId(orderId: String): Flow<List<OrderTimelineEntity>>

    @Query("DELETE FROM order_timeline WHERE orderId = :orderId")
    suspend fun deleteTimelineByOrderId(orderId: String)

    // ── AfterSaleTimeline ────────────────────────────────────

    @Upsert
    suspend fun upsertAfterSaleTimelineEvent(event: AfterSaleTimelineEntity)

    @Upsert
    suspend fun upsertAllAfterSaleTimelineEvents(events: List<AfterSaleTimelineEntity>)

    @Query("SELECT * FROM after_sale_timeline WHERE afterSaleId = :afterSaleId ORDER BY createdAt ASC")
    fun getTimelineByAfterSaleId(afterSaleId: String): Flow<List<AfterSaleTimelineEntity>>

    @Query("DELETE FROM after_sale_timeline WHERE afterSaleId = :afterSaleId")
    suspend fun deleteTimelineByAfterSaleId(afterSaleId: String)
}
