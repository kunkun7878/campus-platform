package com.campus.platform.di

import com.campus.platform.data.repository.AddressRepository
import com.campus.platform.data.repository.AfterSaleRepository
import com.campus.platform.data.repository.AfterSaleTimelineRepository
import com.campus.platform.data.repository.CommunityRepository
import com.campus.platform.data.repository.FavoriteRepository
import com.campus.platform.data.repository.GroupRepository
import com.campus.platform.data.repository.ImageUploadRepository
import com.campus.platform.data.repository.InviteRepository
import com.campus.platform.data.repository.LostFoundRepository
import com.campus.platform.data.repository.MarketOrderRepository
import com.campus.platform.data.repository.MarketRepository
import com.campus.platform.data.repository.MessageRepository
import com.campus.platform.data.repository.MiscRepository
import com.campus.platform.data.repository.NotificationRepository
import com.campus.platform.data.repository.OrderTimelineRepository
import com.campus.platform.data.repository.ReferenceRepository
import com.campus.platform.data.repository.RunnerApplicationRepository
import com.campus.platform.data.repository.RunnerOrderRepository
import com.campus.platform.data.repository.RunnerReviewRepository
import com.campus.platform.data.repository.RunnerTaskRepository
import com.campus.platform.data.repository.UserRepository
import com.campus.platform.domain.repository.IAddressRepository
import com.campus.platform.domain.repository.IAfterSaleRepository
import com.campus.platform.domain.repository.IAfterSaleTimelineRepository
import com.campus.platform.domain.repository.ICommunityRepository
import com.campus.platform.domain.repository.IFavoriteRepository
import com.campus.platform.domain.repository.IGroupRepository
import com.campus.platform.domain.repository.IImageUploadRepository
import com.campus.platform.domain.repository.IInviteRepository
import com.campus.platform.domain.repository.ILostFoundRepository
import com.campus.platform.domain.repository.IMarketOrderRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.domain.repository.IMessageRepository
import com.campus.platform.domain.repository.IMiscRepository
import com.campus.platform.domain.repository.INotificationRepository
import com.campus.platform.domain.repository.IOrderTimelineRepository
import com.campus.platform.domain.repository.IReferenceRepository
import com.campus.platform.domain.repository.IRunnerApplicationRepository
import com.campus.platform.domain.repository.IRunnerOrderRepository
import com.campus.platform.domain.repository.IRunnerReviewRepository
import com.campus.platform.domain.repository.IRunnerTaskRepository
import com.campus.platform.domain.repository.IUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReferenceRepository(impl: ReferenceRepository): IReferenceRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepository): IUserRepository

    @Binds
    @Singleton
    abstract fun bindAddressRepository(impl: AddressRepository): IAddressRepository

    @Binds
    @Singleton
    abstract fun bindRunnerTaskRepository(impl: RunnerTaskRepository): IRunnerTaskRepository

    @Binds
    @Singleton
    abstract fun bindRunnerOrderRepository(impl: RunnerOrderRepository): IRunnerOrderRepository

    @Binds
    @Singleton
    abstract fun bindAfterSaleRepository(impl: AfterSaleRepository): IAfterSaleRepository

    @Binds
    @Singleton
    abstract fun bindMarketRepository(impl: MarketRepository): IMarketRepository

    @Binds
    @Singleton
    abstract fun bindMarketOrderRepository(impl: MarketOrderRepository): IMarketOrderRepository

    @Binds
    @Singleton
    abstract fun bindLostFoundRepository(impl: LostFoundRepository): ILostFoundRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(impl: CommunityRepository): ICommunityRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(impl: GroupRepository): IGroupRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepository): INotificationRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepository): IFavoriteRepository

    @Binds
    @Singleton
    abstract fun bindMiscRepository(impl: MiscRepository): IMiscRepository

    @Binds
    @Singleton
    abstract fun bindRunnerReviewRepository(impl: RunnerReviewRepository): IRunnerReviewRepository

    @Binds
    @Singleton
    abstract fun bindRunnerApplicationRepository(impl: RunnerApplicationRepository): IRunnerApplicationRepository

    @Binds
    @Singleton
    abstract fun bindOrderTimelineRepository(impl: OrderTimelineRepository): IOrderTimelineRepository

    @Binds
    @Singleton
    abstract fun bindAfterSaleTimelineRepository(impl: AfterSaleTimelineRepository): IAfterSaleTimelineRepository

    @Binds
    @Singleton
    abstract fun bindImageUploadRepository(impl: ImageUploadRepository): IImageUploadRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepository): IMessageRepository

    @Binds
    @Singleton
    abstract fun bindInviteRepository(impl: InviteRepository): IInviteRepository
}
