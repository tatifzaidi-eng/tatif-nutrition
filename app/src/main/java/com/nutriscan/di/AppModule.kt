package com.nutriscan.di

import android.content.Context
import androidx.room.Room
import com.nutriscan.BuildConfig
import com.nutriscan.data.api.OpenFoodFactsApi
import com.nutriscan.data.api.UsdaApi
import com.nutriscan.data.db.FoodDao
import com.nutriscan.data.db.NutriScanDatabase
import com.nutriscan.data.repository.FoodRepository
import com.nutriscan.data.repository.FoodRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("usda")
    fun provideUsdaRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.USDA_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("off")
    fun provideOffRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.OPEN_FOOD_FACTS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideUsdaApi(@Named("usda") retrofit: Retrofit): UsdaApi =
        retrofit.create(UsdaApi::class.java)

    @Provides
    @Singleton
    fun provideOffApi(@Named("off") retrofit: Retrofit): OpenFoodFactsApi =
        retrofit.create(OpenFoodFactsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NutriScanDatabase =
        Room.databaseBuilder(context, NutriScanDatabase::class.java, "nutriscan.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideFoodDao(db: NutriScanDatabase): FoodDao = db.foodDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository
}
