/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.alpha.utils

import android.content.Context
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.example.android.wearable.alpha.R
import com.example.android.wearable.alpha.data.watchface.ColorStyleIdAndResourceIds
import com.example.android.wearable.alpha.data.watchface.DRAW_HANDS_DEFAULT
import com.example.android.wearable.alpha.data.watchface.DRAW_HOUR_PIPS_DEFAULT
//import com.example.android.wearable.alpha.data.watchface.DRAW_HOUR_PIPS_DEFAULT
import com.example.android.wearable.alpha.data.watchface.MINUTE_HAND_LENGTH_FRACTION_DEFAULT
import com.example.android.wearable.alpha.data.watchface.MINUTE_HAND_LENGTH_FRACTION_MAXIMUM
import com.example.android.wearable.alpha.data.watchface.MINUTE_HAND_LENGTH_FRACTION_MINIMUM

// Keys to matched content in the  the user style settings. We listen for changes to these
// values in the renderer and if new, we will update the database and update the watch face
// being rendered.
const val COLOR_STYLE_SETTING = "color_style_setting"
const val DRAW_HOUR_PIPS_STYLE_SETTING = "draw_hour_pips_style_setting"
const val ABOUT_STYLE_SETTING = "about_style_setting"
//const val DRAW_HANDS_STYLE_SETTING = "draw_hour_pips_style_setting"
const val WATCH_HAND_LENGTH_STYLE_SETTING = "watch_hand_length_style_setting"
//const val WATCH_HAND_ROPACITY_STYLE_SETTING = "watch_hand_ropacity_style_setting"

/*
 * Creates user styles in the settings activity associated with the watch face, so users can
 * edit different parts of the watch face. In the renderer (after something has changed), the
 * watch face listens for a flow from the watch face API data layer and updates the watch face.
 */
fun createUserStyleSchema(context: Context): UserStyleSchema {


    val aboutsStyleSetting = UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(ABOUT_STYLE_SETTING),
        context.resources,
        //R.string.watchface_pips_setting,
        //R.string.watchface_pips_setting_description,
        R.string.about_style_setting,
        R.string.about_style_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        true
    )



    // 1. Allows user to change the color styles of the watch face (if any are available).
    val colorStyleSetting =
        UserStyleSetting.ListUserStyleSetting(
            UserStyleSetting.Id(COLOR_STYLE_SETTING),
            context.resources,
            R.string.colors_style_setting,
            R.string.colors_style_setting_description,
            null,
            ColorStyleIdAndResourceIds.toOptionList(context),
            listOf(
                WatchFaceLayer.BASE,
                WatchFaceLayer.COMPLICATIONS,
                WatchFaceLayer.COMPLICATIONS_OVERLAY
            )
        )

    // 2. Allows user to toggle on/off the hour pips (dashes around the outer edge of the watch
    // face).




    val drawHourPipsStyleSetting = UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(DRAW_HOUR_PIPS_STYLE_SETTING),
        context.resources,
        //R.string.watchface_pips_setting,
        //R.string.watchface_pips_setting_description,
        R.string.watchface_draw_hands_setting,
        R.string.watchface_draw_hands_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        false
    )






//2.5 - junk
    val drawNewrobhandsStyleSetting = UserStyleSetting.BooleanUserStyleSetting(
        UserStyleSetting.Id(DRAW_HOUR_PIPS_STYLE_SETTING),
        context.resources,
        R.string.watchface_draw_hands_setting,
        R.string.watchface_draw_hands_setting_description,
        null,
        listOf(WatchFaceLayer.BASE),
        DRAW_HANDS_DEFAULT
    )



    //val drawHourHandsStyleSetting = UserStyleSetting.BooleanUserStyleSetting(
    //    UserStyleSetting.Id(DRAW_HANDS_STYLE_SETTING),
    //    context.resources,
    //    R.string.watchface_pips_setting,
    //    R.string.watchface_pips_setting_description,
    //    null,
    //    listOf(WatchFaceLayer.BASE),
    //    DRAW_HOUR_PIPS_DEFAULT
    //)



    // 3. Allows user to change the length of the minute hand. (actually the opacity now)
    val watchHandLengthStyleSetting = UserStyleSetting.DoubleRangeUserStyleSetting(
        UserStyleSetting.Id(WATCH_HAND_LENGTH_STYLE_SETTING),
        context.resources,
        R.string.watchface_hand_ropacity_setting,
        R.string.watchface_hand_ropacity_setting_description,
        null,
        MINUTE_HAND_LENGTH_FRACTION_MINIMUM.toDouble(),
        MINUTE_HAND_LENGTH_FRACTION_MAXIMUM.toDouble(),
        listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
        MINUTE_HAND_LENGTH_FRACTION_DEFAULT.toDouble()
    )
//3b
    //val watchHandRopacitySetting = UserStyleSetting.DoubleRangeUserStyleSetting(
    //    UserStyleSetting.Id(WATCH_HAND_ROPACITY_STYLE_SETTING),
    //    context.resources,
    //    R.string.watchface_hand_ropacity_setting,
    //    R.string.watchface_hand_ropacity_setting_description,
    //    null,
    //    MINUTE_HAND_LENGTH_FRACTION_MINIMUM.toDouble(),
    //    MINUTE_HAND_LENGTH_FRACTION_MAXIMUM.toDouble(),
    //    listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
    //    MINUTE_HAND_LENGTH_FRACTION_DEFAULT.toDouble()
    //)
// Define the opacity setting
    //val watchHandOpacitySetting = UserStyleSetting.IntegerRangeUserStyleSetting(
    //    UserStyleSetting.Id(WATCH_HAND_ROPACITY_STYLE_SETTING),
    //    context.resources,
    //    R.string.watchface_hand_ropacity_setting,
    //    R.string.watchface_hand_ropacity_setting_description,
    //    null,
    //    0, // Minimum opacity value
    //    255, // Maximum opacity value
    //    listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
    //    100 // Default opacity value
    //)


    // 4. Create style settings to hold all options.
    return UserStyleSchema(
        listOf(
            aboutsStyleSetting,
            colorStyleSetting,
            drawHourPipsStyleSetting,
            //drawNewrobhandsStyleSetting,
            watchHandLengthStyleSetting
            //watchHandRopacitySetting
        )
    )
}
