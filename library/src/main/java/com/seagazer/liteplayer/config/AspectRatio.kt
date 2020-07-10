package com.seagazer.liteplayer.config

/**
 * AspectRatio mode define.
 *
 * Author: Seagazer
 * Date: 2020/6/20
 */
enum class AspectRatio {
    /**
     * W16:9
     */
    W_16_9,

    /**
     * W4:3
     */
    W_4_3,

    /**
     * W21:9
     */
    W_21_9,

    /**
     * Stretch to fill view size
     */
    STRETCH,

    /**
     * Keep video aspectRatio and fill view size
     */
    FILL,

    /**
     * Auto
     */
    AUTO,

    /**
     * Video origin size
     */
    ORIGIN,
}