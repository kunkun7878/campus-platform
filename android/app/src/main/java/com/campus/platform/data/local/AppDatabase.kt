package com.campus.platform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.campus.platform.data.local.dao.CommunityDao
import com.campus.platform.data.local.dao.ConversationDao
import com.campus.platform.data.local.dao.LostFoundDao
import com.campus.platform.data.local.dao.MarketDao
import com.campus.platform.data.local.dao.MessageDao
import com.campus.platform.data.local.dao.MiscDao
import com.campus.platform.data.local.dao.ReferenceDao
import com.campus.platform.data.local.dao.RunnerDao
import com.campus.platform.data.local.dao.UserDao
import com.campus.platform.data.local.entity.AfterSaleEntity
import com.campus.platform.data.local.entity.AfterSaleTimelineEntity
import com.campus.platform.data.local.entity.AnnouncementEntity
import com.campus.platform.data.local.entity.CampusEntity
import com.campus.platform.data.local.entity.CommunityCommentEntity
import com.campus.platform.data.local.entity.CommunityPostEntity
import com.campus.platform.data.local.entity.ConversationEntity
import com.campus.platform.data.local.entity.CouponEntity
import com.campus.platform.data.local.entity.FeedbackEntity
import com.campus.platform.data.local.entity.GroupMemberEntity
import com.campus.platform.data.local.entity.LostFoundClaimEntity
import com.campus.platform.data.local.entity.LostFoundItemEntity
import com.campus.platform.data.local.entity.MarketListingEntity
import com.campus.platform.data.local.entity.MarketOrderEntity
import com.campus.platform.data.local.entity.MessageEntity
import com.campus.platform.data.local.entity.NotificationEntity
import com.campus.platform.data.local.entity.OfficialGroupEntity
import com.campus.platform.data.local.entity.OrderTimelineEntity
import com.campus.platform.data.local.entity.PostLikeEntity
import com.campus.platform.data.local.entity.ProfileEntity
import com.campus.platform.data.local.entity.RunnerApplicationEntity
import com.campus.platform.data.local.entity.RunnerOrderEntity
import com.campus.platform.data.local.entity.RunnerReviewEntity
import com.campus.platform.data.local.entity.RunnerTaskEntity
import com.campus.platform.data.local.entity.SchoolEntity
import com.campus.platform.data.local.entity.UserAddressEntity
import com.campus.platform.data.local.entity.UserCouponEntity
import com.campus.platform.data.local.entity.UserFavoriteEntity
import com.campus.platform.data.local.entity.WalletEntity

@Database(
    entities = [
        // Reference
        SchoolEntity::class,
        CampusEntity::class,
        // User
        ProfileEntity::class,
        WalletEntity::class,
        UserAddressEntity::class,
        UserFavoriteEntity::class,
        NotificationEntity::class,
        // Runner
        RunnerTaskEntity::class,
        RunnerOrderEntity::class,
        RunnerReviewEntity::class,
        AfterSaleEntity::class,
        AfterSaleTimelineEntity::class,
        OrderTimelineEntity::class,
        RunnerApplicationEntity::class,
        // Market
        MarketListingEntity::class,
        MarketOrderEntity::class,
        // Community
        CommunityPostEntity::class,
        CommunityCommentEntity::class,
        OfficialGroupEntity::class,
        GroupMemberEntity::class,
        PostLikeEntity::class,
        // Lost & Found
        LostFoundItemEntity::class,
        LostFoundClaimEntity::class,
        // Messaging
        ConversationEntity::class,
        MessageEntity::class,
        // Misc
        AnnouncementEntity::class,
        CouponEntity::class,
        UserCouponEntity::class,
        FeedbackEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(CampusTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun referenceDao(): ReferenceDao
    abstract fun userDao(): UserDao
    abstract fun runnerDao(): RunnerDao
    abstract fun marketDao(): MarketDao
    abstract fun communityDao(): CommunityDao
    abstract fun lostFoundDao(): LostFoundDao
    abstract fun miscDao(): MiscDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
