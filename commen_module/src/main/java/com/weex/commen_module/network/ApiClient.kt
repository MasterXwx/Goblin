package com.weex.commen_module.network

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.weex.commen_module.network.interceptor.HttpLoggingInterceptor
import com.weex.commen_module.permission.PermissionHelper
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by xuwx on 2019/1/8.
 */
class ApiClient {

    private var mRetrofit: Retrofit
    private val serviceMap = TreeMap<String, Object>()


    companion object {

        const val BASE_URL = ""

        @Volatile
        var instance: ApiClient? = null
            get() {
                if (instance == null) {
                    synchronized(ApiClient::class.java)
                    {
                        if (instance == null) {
                            instance = ApiClient()
                        }
                    }
                }
                return instance
            }
    }

    private constructor() {
        PermissionHelper.instance.
        val okHttpClientBuilder = OkHttpClient.Builder()
        okHttpClientBuilder.readTimeout(2, TimeUnit.MINUTES)
        okHttpClientBuilder.writeTimeout(2, TimeUnit.MINUTES)
        okHttpClientBuilder.connectTimeout(2, TimeUnit.MINUTES)
        okHttpClientBuilder.addInterceptor(HttpLoggingInterceptor(true).setLevel(HttpLoggingInterceptor.Level.BODY))
        mRetrofit = Retrofit.Builder().baseUrl(BASE_URL)
                .client(okHttpClientBuilder.build())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    fun <T> getService(clazz: Class<T>): T {
        if (serviceMap.containsKey(clazz.simpleName)) {
            return serviceMap[clazz.simpleName] as T
        }

        val service = mRetrofit.create(clazz)
        serviceMap[clazz.simpleName] = service as Object
        return service
    }

}