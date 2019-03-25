package com.weex.commen_module.network.entity

/**
 * Created by xuwx on 2019/1/8.
 */
data class ApiResult<T>(var code: Int, var message: String, var data: T)