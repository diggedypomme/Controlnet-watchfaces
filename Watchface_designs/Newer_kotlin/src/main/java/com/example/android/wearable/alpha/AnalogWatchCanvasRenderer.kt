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
package com.example.android.wearable.alpha

//import com.example.android.wearable.alpha.utils.DRAW_HANDS_STYLE_SETTING
//Rob
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.withRotation
import androidx.core.graphics.withScale
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.example.android.wearable.alpha.data.watchface.ColorStyleIdAndResourceIds
import com.example.android.wearable.alpha.data.watchface.WatchFaceColorPalette.Companion.convertToWatchFaceColorPalette
import com.example.android.wearable.alpha.data.watchface.WatchFaceData
import com.example.android.wearable.alpha.utils.COLOR_STYLE_SETTING
import com.example.android.wearable.alpha.utils.DRAW_HOUR_PIPS_STYLE_SETTING
import com.example.android.wearable.alpha.utils.WATCH_HAND_LENGTH_STYLE_SETTING
//import com.example.android.wearable.alpha.utils.WATCH_HAND_ROPACITY_STYLE_SETTING
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
//
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService




// Default for how long each frame is displayed at expected frame rate.
//private const val FRAME_PERIOD_MS_DEFAULT: Long = 1000L
private const val FRAME_PERIOD_MS_DEFAULT: Long = 60000L


//Rob
var  HandAlpha = 200 // Set alpha value (0-255)
private val ToggleShowHands = 0 // Set alpha value (0-255)

var previousHour: Int = -1
var previousMinute: Int = -1


/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
class AnalogWatchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<AnalogWatchCanvasRenderer.AnalogSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    //init {
    //    // Initialize your class
//
    //    // Set tap listener
    //    setTapListener()
    //}
    class AnalogSharedAssets : SharedAssets {
        override fun onDestroy() {
        }
    }






    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mBackgroundBitmap: Bitmap? = null // Define mBackgroundBitmap here


    // Represents all data needed to render the watch face. All value defaults are constants. Only
    // three values are changeable by the user (color scheme, ticks being rendered, and length of
    // the minute arm). Those dynamic values are saved in the watch face APIs and we update those
    // here (in the renderer) through a Kotlin Flow.
    private var watchFaceData: WatchFaceData = WatchFaceData()

    // Converts resource ids into Colors and ComplicationDrawable.
    private var watchFaceColors = convertToWatchFaceColorPalette(
        context,
        watchFaceData.activeColorStyle,
        watchFaceData.ambientColorStyle
    )

    // Initializes paint object for painting the clock hands with default values.
    private val clockHandPaint = Paint().apply {
        isAntiAlias = true
        //color = Color.GREEN  Rob
        //alpha = minuteHandAlpha
        strokeWidth =
            context.resources.getDimensionPixelSize(R.dimen.clock_hand_stroke_width).toFloat()
    }

    private val outerElementPaint = Paint().apply {
        isAntiAlias = true
    }

    // Used to paint the main hour hand text with the hour pips, i.e., 3, 6, 9, and 12 o'clock.
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.hour_mark_size).toFloat()
    }

    private lateinit var hourHandFill: Path
    private lateinit var hourHandBorder: Path
    private lateinit var minuteHandFill: Path
    private lateinit var minuteHandBorder: Path
    private lateinit var secondHand: Path

    // Changed when setting changes cause a change in the minute hand arm (triggered by user in
    // updateUserStyle() via userStyleRepository.addUserStyleListener()).
    private var armLengthChangedRecalculateClockHands: Boolean = false

    // Default size of watch face drawing area, that is, a no size rectangle. Will be replaced with
    // valid dimensions from the system.
    private var currentWatchFaceSize = Rect(0, 0, 0, 0)

    init {
        scope.launch {
            println("ran scope.launch")
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateWatchFaceData(userStyle)
            }
        }
    }

    override suspend fun createSharedAssets(): AnalogSharedAssets {
        println("ran createSharedAssets")
        return AnalogSharedAssets()
    }

    /*
     * Triggered when the user makes changes to the watch face through the settings activity. The
     * function is called by a flow.
     */
    private fun updateWatchFaceData(userStyle: UserStyle) {
        Log.d(TAG, "updateWatchFace(): $userStyle")

        println("Checking for updateWatchFaceData")
        println("ran updateWatchFaceData")

        var newWatchFaceData: WatchFaceData = watchFaceData

        // Loops through user style and applies new values to watchFaceData.
        for (options in userStyle) {
            when (options.key.id.toString()) {
                COLOR_STYLE_SETTING -> {
                    val listOption = options.value as
                        UserStyleSetting.ListUserStyleSetting.ListOption

                    newWatchFaceData = newWatchFaceData.copy(
                        activeColorStyle = ColorStyleIdAndResourceIds.getColorStyleConfig(
                            listOption.id.toString()
                        )
                    )
                }
                DRAW_HOUR_PIPS_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        drawHourPips = booleanValue.value
                    )
                }

                //DRAW_HANDS_STYLE_SETTING -> {
                //    val booleanValue = options.value as
                //        UserStyleSetting.BooleanUserStyleSetting.BooleanOption
//
                //    newWatchFaceData = newWatchFaceData.copy(
                //        drawTheHands = booleanValue.value
                //    )
                //}
                WATCH_HAND_LENGTH_STYLE_SETTING -> {
                    val doubleValue = options.value as
                        UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption

                    // The arm lengths are usually only calculated the first time the watch face is
                    // loaded to reduce the ops in the onDraw(). Because we updated the minute hand
                    // watch length, we need to trigger a recalculation.
                    armLengthChangedRecalculateClockHands = true

// Updates length of minute hand based on edits from user.
                    val newMinuteHandDimensions = newWatchFaceData.minuteHandDimensions.copy(
                        lengthFraction = doubleValue.value.toFloat()
                    )

// Access lengthFraction property within the same block

                    //
                    println(newMinuteHandDimensions.lengthFraction)

                    val minHandAlpha = 0 // Assuming HandAlpha starts from 0
                    val maxHandAlpha = 255

                    val mappedHandAlpha = mapValue(
                        newMinuteHandDimensions.lengthFraction,
                        0.1f, 0.4f,  // Input range of lengthFraction
                        minHandAlpha.toFloat(), maxHandAlpha.toFloat()  // Output range of HandAlpha
                    ).toInt()
                    HandAlpha=mappedHandAlpha

                    //newWatchFaceData = newWatchFaceData.copy(
                    //    minuteHandDimensions = newMinuteHandDimensions
                    //)
                }
                //WATCH_HAND_ROPACITY_STYLE_SETTING -> {
                //    val doubleValue = options.value as
                //        UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
//
                //    // The arm lengths are usually only calculated the first time the watch face is
                //    // loaded to reduce the ops in the onDraw(). Because we updated the minute hand
                //    // watch length, we need to trigger a recalculation.
                //    armLengthChangedRecalculateClockHands = true
//
                //    // Updates length of minute hand based on edits from user.
                //    val newMinuteHandDimensions = newWatchFaceData.minuteHandDimensions.copy(
                //        lengthFraction = doubleValue.value.toFloat()
                //    )
//
                //    newWatchFaceData = newWatchFaceData.copy(
                //        minuteHandDimensions = newMinuteHandDimensions
                //    )
                //}
            }
        }

        // Only updates if something changed.
        println("Checking for change")
        println("ran checking for change")
        println(watchFaceData)
        println(newWatchFaceData)
        if (watchFaceData != newWatchFaceData) {
            watchFaceData = newWatchFaceData

            println("ran check passed")
            // Recreates Color and ComplicationDrawable from resource ids.
            watchFaceColors = convertToWatchFaceColorPalette(
                context,
                watchFaceData.activeColorStyle,
                watchFaceData.ambientColorStyle
            )

            // Applies the user chosen complication color scheme changes. ComplicationDrawables for
            // each of the styles are defined in XML so we need to replace the complication's
            // drawables.
            for ((_, complication) in complicationSlotsManager.complicationSlots) {
                ComplicationDrawable.getDrawable(
                    context,
                    watchFaceColors.complicationStyleDrawableId
                )?.let {
                    (complication.renderer as CanvasComplicationDrawable).drawable = it
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        println("ran onDestroy")
        scope.cancel("AnalogWatchCanvasRenderer scope clear() request")
        super.onDestroy()
    }
    fun mapValue(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float {
        return (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow
    }
    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        println("ran render highlight")
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        println("ran render")
        //val backgroundColor = if (renderParameters.drawMode == DrawMode.AMBIENT) {
        //    watchFaceColors.ambientBackgroundColor
//
//
        //} else {
        //    watchFaceColors.activeBackgroundColor
        //}

        val hour = zonedDateTime.hour
        val minute = zonedDateTime.minute


        // Draw the background image
        //val backgroundImage = BitmapFactory.decodeResource(context.resources, R.drawable.c0023)
        // canvas.drawBitmap(backgroundImage, null, bounds, null)


        //this is where the ifs should go, not inside the updatebasedontime
        //println("---")
        //println(renderParameters.drawMode)
        //println(DrawMode.AMBIENT)

        val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        if (drawAmbient) {
            //canvas.drawColor(Color.BLACK)

            //previousHour = hour
            //previousMinute = minute


        } else {

           // if (hour != previousHour || minute != previousMinute) {


                // Update the background based on the current time
                updateBackgroundBasedOnTime(hour, minute)

                println("ran drawing backdrop")
                // Draw the background bitmap if it's not null
                mBackgroundBitmap?.let {
                    canvas.drawBitmap(it, null, bounds, null)
                }
          //  }
            println("ran should have been drawing backdrop")


            // Update previousHour and previousMinute
            //previousHour = hour
            //previousMinute = minute
        }




//cant get it to skip updating it by setting this. it still overwrites
       // if (drawAmbient || (hour != previousHour || minute != previousMinute) || 1==1) {
        //if (drawAmbient || (hour != previousHour || minute != previousMinute) ) {

            println("ran should have drawAmbient || (hour != previousHour || minute != previousMinute) ")
            // Draw the complications
            drawComplications(canvas, zonedDateTime)

            if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)) {
                drawClockHands(canvas, bounds, zonedDateTime)
            }

            if (renderParameters.drawMode == DrawMode.INTERACTIVE &&
                renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE) &&
                watchFaceData.drawHourPips
            ) {
                drawNumberStyleOuterElement(
                    canvas,
                    bounds,
                    watchFaceData.numberRadiusFraction,
                    watchFaceData.numberStyleOuterCircleRadiusFraction,
                    watchFaceColors.activeOuterElementColor,
                    watchFaceData.numberStyleOuterCircleRadiusFraction,
                    watchFaceData.gapBetweenOuterCircleAndBorderFraction
                )
            }
        // }
        previousHour = hour
        previousMinute = minute
    }


    fun updateBackgroundBasedOnTime(hour: Int, minute: Int) {
        //println("-----!")
        println("ran updater!")
        println("ran updateBackgroundBasedOnTime")
        var updatedHour = hour

        // Convert to a 12-hour format if needed
        if (updatedHour > 11) {

                updatedHour -= 12

        }
        //println(hour)
        //println(updatedHour)
//is it 12 or 00?






                println("saw a change!")


            // Construct the resource ID for the corresponding image
            val timeString = String.format(Locale.US, "%02d%02d", updatedHour, minute)
            val imageName = "c$timeString"
            val resourceId = context.resources.getIdentifier(imageName, "drawable", context.packageName)

            // Load the background bitmap
            mBackgroundBitmap = BitmapFactory.decodeResource(context.resources, resourceId)

    }


    // ----- All drawing functions -----
    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        println("ran drawComplications")
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawClockHands(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        println("ran drawClockHands")
        // Only recalculate bounds (watch face size/surface) has changed or the arm of one of the
        // clock hands has changed (via user input in the settings).
        // NOTE: Watch face surface usually only updates one time (when the size of the device is
        // initially broadcasted).
        if (currentWatchFaceSize != bounds || armLengthChangedRecalculateClockHands) {
            armLengthChangedRecalculateClockHands = false
            currentWatchFaceSize = bounds
            recalculateClockHands(bounds)
        }

        // Retrieve current time to calculate location/rotation of watch arms.
        val secondOfDay = zonedDateTime.toLocalTime().toSecondOfDay()

        // Determine the rotation of the hour and minute hand.

        // Determine how many seconds it takes to make a complete rotation for each hand
        // It takes the hour hand 12 hours to make a complete rotation
        val secondsPerHourHandRotation = Duration.ofHours(12).seconds
        // It takes the minute hand 1 hour to make a complete rotation
        val secondsPerMinuteHandRotation = Duration.ofHours(1).seconds

        // Determine the angle to draw each hand expressed as an angle in degrees from 0 to 360
        // Since each hand does more than one cycle a day, we are only interested in the remainder
        // of the secondOfDay modulo the hand interval
        val hourRotation = secondOfDay.rem(secondsPerHourHandRotation) * 360.0f /
            secondsPerHourHandRotation
        val minuteRotation = secondOfDay.rem(secondsPerMinuteHandRotation) * 360.0f /
            secondsPerMinuteHandRotation

        canvas.withScale(
            x = WATCH_HAND_SCALE,
            y = WATCH_HAND_SCALE,
            pivotX = bounds.exactCenterX(),
            pivotY = bounds.exactCenterY()
        ) {
            val drawAmbient = renderParameters.drawMode == DrawMode.AMBIENT


            clockHandPaint.style = if (drawAmbient) Paint.Style.STROKE else Paint.Style.FILL
            clockHandPaint.color = if (drawAmbient) {
                watchFaceColors.ambientPrimaryColor
            } else {
                watchFaceColors.activePrimaryColor

            }

            //rob
            //clockHandPaint.color = Color.GREEN
            //if ( ToggleShowHands==1){
            if ( watchFaceData.drawHourPips){
                //clockHandPaint.alpha = HandAlpha
                //clockHandPaint.color = Color.GREEN
                clockHandPaint.alpha = HandAlpha

            }
            else
            {
                //clockHandPaint.color = Color.YELLOW
                clockHandPaint.alpha = 0
            }

            if (drawAmbient)
            {
                clockHandPaint.color = Color.WHITE

            }

            // Draw hour hand.
            withRotation(hourRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                drawPath(hourHandBorder, clockHandPaint)
            }

            // Draw minute hand.
            withRotation(minuteRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                drawPath(minuteHandBorder, clockHandPaint)
            }

            // Draw second hand if not in ambient mode
            //if (!drawAmbient) {
            if (1==2) {
                clockHandPaint.color = watchFaceColors.activeSecondaryColor

                // Second hand has a different color style (secondary color) and is only drawn in
                // active mode, so we calculate it here (not above with others).
                val secondsPerSecondHandRotation = Duration.ofMinutes(1).seconds
                val secondsRotation = secondOfDay.rem(secondsPerSecondHandRotation) * 360.0f /
                    secondsPerSecondHandRotation
                //clockHandPaint.color = watchFaceColors.activeSecondaryColor
                //clockHandPaint.color = Color.GREEN
                //clockHandPaint.alpha = HandAlpha



                //I need to stop it rendering so often. without the seconds it could just update once a minute

                //if ( watchFaceData.drawHourPips){
                //    //clockHandPaint.alpha = HandAlpha
                //    //clockHandPaint.color = Color.GREEN
                //    clockHandPaint.alpha = HandAlpha
                //}
                //else
                //{
                //    //clockHandPaint.color = Color.YELLOW
                //    clockHandPaint.alpha = 0
                //}

                clockHandPaint.alpha = HandAlpha

                //color = Color.GREEN  Rob
                //alpha = minuteHandAlpha

                withRotation(secondsRotation, bounds.exactCenterX(), bounds.exactCenterY()) {
                    drawPath(secondHand, clockHandPaint)
                }
            }
        }
    }

    /*
     * Rarely called (only when watch face surface changes; usually only once) from the
     * drawClockHands() method.
     */
    private fun recalculateClockHands(bounds: Rect) {
        println("ran recalculateClockHands")
        Log.d(TAG, "recalculateClockHands()")
        hourHandBorder =
            createClockHand(
                bounds,
                watchFaceData.hourHandDimensions.lengthFraction,
                watchFaceData.hourHandDimensions.widthFraction,
                watchFaceData.gapBetweenHandAndCenterFraction,
                watchFaceData.hourHandDimensions.xRadiusRoundedCorners,
                watchFaceData.hourHandDimensions.yRadiusRoundedCorners
            )
        hourHandFill = hourHandBorder

        minuteHandBorder =
            createClockHand(
                bounds,
                watchFaceData.minuteHandDimensions.lengthFraction,
                watchFaceData.minuteHandDimensions.widthFraction,
                watchFaceData.gapBetweenHandAndCenterFraction,
                watchFaceData.minuteHandDimensions.xRadiusRoundedCorners,
                watchFaceData.minuteHandDimensions.yRadiusRoundedCorners


            )
        minuteHandFill = minuteHandBorder

        secondHand =
            createClockHand(
                bounds,
                watchFaceData.secondHandDimensions.lengthFraction,
                watchFaceData.secondHandDimensions.widthFraction,
                watchFaceData.gapBetweenHandAndCenterFraction,
                watchFaceData.secondHandDimensions.xRadiusRoundedCorners,
                watchFaceData.secondHandDimensions.yRadiusRoundedCorners
            )
    }

    /**
     * Returns a round rect clock hand if {@code rx} and {@code ry} equals to 0, otherwise return a
     * rect clock hand.
     *
     * @param bounds The bounds use to determine the coordinate of the clock hand.
     * @param length Clock hand's length, in fraction of {@code bounds.width()}.
     * @param thickness Clock hand's thickness, in fraction of {@code bounds.width()}.
     * @param gapBetweenHandAndCenter Gap between inner side of arm and center.
     * @param roundedCornerXRadius The x-radius of the rounded corners on the round-rectangle.
     * @param roundedCornerYRadius The y-radius of the rounded corners on the round-rectangle.
     */
    private fun createClockHand(
        bounds: Rect,
        length: Float,
        thickness: Float,
        gapBetweenHandAndCenter: Float,
        roundedCornerXRadius: Float,
        roundedCornerYRadius: Float
    ): Path {
        println("ran createClockHand")
        val width = bounds.width()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val left = centerX - thickness / 2 * width
        val top = centerY - (gapBetweenHandAndCenter + length) * width
        val right = centerX + thickness / 2 * width
        val bottom = centerY - gapBetweenHandAndCenter * width
        val path = Path()

        if (roundedCornerXRadius != 0.0f || roundedCornerYRadius != 0.0f) {
            path.addRoundRect(
                left,
                top,
                right,
                bottom,
                roundedCornerXRadius,
                roundedCornerYRadius,
                Path.Direction.CW
            )
        } else {
            path.addRect(
                left,
                top,
                right,
                bottom,
                Path.Direction.CW
            )
        }
        return path
    }

    private fun drawNumberStyleOuterElement(
        canvas: Canvas,
        bounds: Rect,
        numberRadiusFraction: Float,
        outerCircleStokeWidthFraction: Float,
        outerElementColor: Int,
        numberStyleOuterCircleRadiusFraction: Float,
        gapBetweenOuterCircleAndBorderFraction: Float
    ) {
        println("ran drawNumberStyleOuterElement")
        // Draws text hour indicators (12, 3, 6, and 9).
        val textBounds = Rect()
        textPaint.color = outerElementColor
        for (i in 0 until 4) {
            val rotation = 0.5f * (i + 1).toFloat() * Math.PI
            val dx = sin(rotation).toFloat() * numberRadiusFraction * bounds.width().toFloat()
            val dy = -cos(rotation).toFloat() * numberRadiusFraction * bounds.width().toFloat()
            textPaint.getTextBounds(HOUR_MARKS[i], 0, HOUR_MARKS[i].length, textBounds)
            canvas.drawText(
                HOUR_MARKS[i],
                bounds.exactCenterX() + dx - textBounds.width() / 2.0f,
                bounds.exactCenterY() + dy + textBounds.height() / 2.0f,
                textPaint
            )
        }

        // Draws dots for the remain hour indicators between the numbers above.
        outerElementPaint.strokeWidth = outerCircleStokeWidthFraction * bounds.width()
        outerElementPaint.color = outerElementColor
        canvas.save()
        for (i in 0 until 12) {
            if (i % 3 != 0) {
                drawTopMiddleCircle(
                    canvas,
                    bounds,
                    numberStyleOuterCircleRadiusFraction,
                    gapBetweenOuterCircleAndBorderFraction
                )
            }
            canvas.rotate(360.0f / 12.0f, bounds.exactCenterX(), bounds.exactCenterY())
        }
        canvas.restore()
    }

    /** Draws the outer circle on the top middle of the given bounds. */
    private fun drawTopMiddleCircle(
        canvas: Canvas,
        bounds: Rect,
        radiusFraction: Float,
        gapBetweenOuterCircleAndBorderFraction: Float
    ) {
        println("ran drawTopMiddleCircle")
        outerElementPaint.style = Paint.Style.FILL_AND_STROKE

        // X and Y coordinates of the center of the circle.
        val centerX = 0.5f * bounds.width().toFloat()
        val centerY = bounds.width() * (gapBetweenOuterCircleAndBorderFraction + radiusFraction)

        canvas.drawCircle(
            centerX,
            centerY,
            radiusFraction * bounds.width(),
            outerElementPaint
        )
    }

    companion object {
        private const val TAG = "AnalogWatchCanvasRenderer"

        // Painted between pips on watch face for hour marks.
        private val HOUR_MARKS = arrayOf("3", "6", "9", "12")

        // Used to canvas.scale() to scale watch hands in proper bounds. This will always be 1.0.
        private const val WATCH_HAND_SCALE = 1.0f
    }




    private fun toggleHourHandsVisibility() {
        if (ToggleShowHands==1)
        {
           // ToggleShowHands=0

        }
        else
        {
          //  ToggleShowHands=1
        }

        // Recalculate clock hands to apply changes
        //recalculateClockHands(currentWatchFaceSize)
        // Redraw the watch face
        //invalidate()
    }
    private fun setTapListener() {
        println("-----!")
        println("-----!")
        println("-----!")
        println("-----!")
        println("--setTapListener---!")
        //setTapListener { tapEvent ->
        //    when (tapEvent.type) {
        //        TapType.DOWN -> {
        //            // Handle tap down event
        //        }
        //        TapType.UP -> {
        //            // Handle tap up event
        //        }
        //        TapType.CANCEL -> {
        //            // Handle tap cancel event
        //        }
        //    }
        //}
    }



}
