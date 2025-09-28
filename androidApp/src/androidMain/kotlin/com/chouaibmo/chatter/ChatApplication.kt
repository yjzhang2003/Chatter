package com.chouaibmo.chatter

import android.app.Application
import di.PlatformModule

/**
 * Android应用程序类
 * 负责初始化全局依赖
 */
class ChatApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化平台模块
        PlatformModule.initialize(this)
    }
}