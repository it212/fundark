package com.silencefly96.module_base.net.funnet

import com.silencefly96.module_base.net.funnet.interceptor.Interceptor
import java.util.ArrayList


/**
 * FunNet网络请求框架:
 *
 * 核心思想：
 * 1、使用Request、Response、BlockingQueue形式的HTTP网络请求框架
 * 2、支持同步请求、异步请求
 * 3、多线程并行请求，支持请求取消，response主线程投递
 * 4、支持三级缓存：内存缓存、文件缓存、网络缓存
 * 5、支持多类型请求：String、Json、File
 *
 * 流程分析(volley)：
 * Request -> RequestQueue -> Dispatcher -> ResponseDelivery -> Response
 * 流程分析(okhttp)：
 * OkHttpClient -> Request -> Call -> Dispatcher -> Interceptors -> Response
 */
class FunNetClient {

    // 线程调度器
    val dispatcher = Dispatcher()

    // 拦截器
    val interceptors: MutableList<Interceptor> = ArrayList()

    val cookieJar: CookieJar = CookieJar()
    var cache: Cache? = null

    fun newCall(request: Request): Call {
        return RealCall.newRealCall(FunNetClient(), Request(""))
    }
}

class CookieJar()
class Cache()
