package com.seagazer.liteplayer.config

/**
 *
 * Author: Seagazer
 * Date: 2020/6/20
 */
enum class AspectRatio {
    /**
     * 宽屏16:9模式
     */
    W_16_9,

    /**
     * 宽屏4:3模式
     */
    W_4_3,

    /**
     * 宽屏21:9模式
     */
    W_21_9,

    /**
     * 拉伸模式
     */
    STRETCH,

    /**
     * 填充模式(原始比例填满视图)
     */
    FILL,

    /**
     * 自适应
     */
    AUTO,

    /**
     * 原始尺寸
     */
    ORIGIN,
}