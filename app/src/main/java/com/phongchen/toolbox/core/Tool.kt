package com.phongchen.toolbox.core

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.phongchen.toolbox.R
import com.phongchen.toolbox.widget.receiver.AltimeterWidgetReceiver
import com.phongchen.toolbox.widget.receiver.AstroWidgetReceiver
import com.phongchen.toolbox.widget.receiver.AudioWidgetReceiver
import com.phongchen.toolbox.widget.receiver.CompassWidgetReceiver
import com.phongchen.toolbox.widget.receiver.FlashlightWidgetReceiver
import com.phongchen.toolbox.widget.receiver.GpsWidgetReceiver
import com.phongchen.toolbox.widget.receiver.LevelWidgetReceiver
import com.phongchen.toolbox.widget.receiver.LightWidgetReceiver
import com.phongchen.toolbox.widget.receiver.MagnifierWidgetReceiver
import com.phongchen.toolbox.widget.receiver.MeasureWidgetReceiver
import com.phongchen.toolbox.widget.receiver.MetalWidgetReceiver
import com.phongchen.toolbox.widget.receiver.SoundWidgetReceiver
import com.phongchen.toolbox.widget.receiver.SpeedWidgetReceiver
import com.phongchen.toolbox.widget.receiver.StepWidgetReceiver
import com.phongchen.toolbox.widget.receiver.TimerWidgetReceiver
import com.phongchen.toolbox.widget.receiver.VibrationWidgetReceiver

/**
 * The single registry of tools. Home grid, deep links and home-screen widgets are all
 * generated from this enum. Adding a tool = adding an entry plus its feature package.
 */
enum class Tool(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val widgetDescriptionRes: Int,
    val widgetReceiver: Class<*>,
) {
    COMPASS(
        route = "compass",
        titleRes = R.string.tool_compass,
        subtitleRes = R.string.subtitle_compass,
        iconRes = R.drawable.ic_tool_compass,
        widgetDescriptionRes = R.string.widget_description_compass,
        widgetReceiver = CompassWidgetReceiver::class.java,
    ),
    LIGHT(
        route = "light",
        titleRes = R.string.tool_light,
        subtitleRes = R.string.subtitle_light,
        iconRes = R.drawable.ic_tool_light,
        widgetDescriptionRes = R.string.widget_description_light,
        widgetReceiver = LightWidgetReceiver::class.java,
    ),
    ALTIMETER(
        route = "altimeter",
        titleRes = R.string.tool_altimeter,
        subtitleRes = R.string.subtitle_altimeter,
        iconRes = R.drawable.ic_tool_altimeter,
        widgetDescriptionRes = R.string.widget_description_altimeter,
        widgetReceiver = AltimeterWidgetReceiver::class.java,
    ),
    FLASHLIGHT(
        route = "flashlight",
        titleRes = R.string.tool_flashlight,
        subtitleRes = R.string.subtitle_flashlight,
        iconRes = R.drawable.ic_tool_flashlight,
        widgetDescriptionRes = R.string.widget_description_flashlight,
        widgetReceiver = FlashlightWidgetReceiver::class.java,
    ),
    LEVEL(
        route = "level",
        titleRes = R.string.tool_level,
        subtitleRes = R.string.subtitle_level,
        iconRes = R.drawable.ic_tool_level,
        widgetDescriptionRes = R.string.widget_description_level,
        widgetReceiver = LevelWidgetReceiver::class.java,
    ),
    SOUND(
        route = "sound",
        titleRes = R.string.tool_sound,
        subtitleRes = R.string.subtitle_sound,
        iconRes = R.drawable.ic_tool_sound,
        widgetDescriptionRes = R.string.widget_description_sound,
        widgetReceiver = SoundWidgetReceiver::class.java,
    ),
    SPEED(
        route = "speed",
        titleRes = R.string.tool_speed,
        subtitleRes = R.string.subtitle_speed,
        iconRes = R.drawable.ic_tool_speed,
        widgetDescriptionRes = R.string.widget_description_speed,
        widgetReceiver = SpeedWidgetReceiver::class.java,
    ),
    METAL(
        route = "metal",
        titleRes = R.string.tool_metal,
        subtitleRes = R.string.subtitle_metal,
        iconRes = R.drawable.ic_tool_metal,
        widgetDescriptionRes = R.string.widget_description_metal,
        widgetReceiver = MetalWidgetReceiver::class.java,
    ),
    STEP(
        route = "step",
        titleRes = R.string.tool_step,
        subtitleRes = R.string.subtitle_step,
        iconRes = R.drawable.ic_tool_step,
        widgetDescriptionRes = R.string.widget_description_step,
        widgetReceiver = StepWidgetReceiver::class.java,
    ),
    GPS(
        route = "gps",
        titleRes = R.string.tool_gps,
        subtitleRes = R.string.subtitle_gps,
        iconRes = R.drawable.ic_tool_gps,
        widgetDescriptionRes = R.string.widget_description_gps,
        widgetReceiver = GpsWidgetReceiver::class.java,
    ),
    VIBRATION(
        route = "vibration",
        titleRes = R.string.tool_vibration,
        subtitleRes = R.string.subtitle_vibration,
        iconRes = R.drawable.ic_tool_vibration,
        widgetDescriptionRes = R.string.widget_description_vibration,
        widgetReceiver = VibrationWidgetReceiver::class.java,
    ),
    AUDIO(
        route = "audio",
        titleRes = R.string.tool_audio,
        subtitleRes = R.string.subtitle_audio,
        iconRes = R.drawable.ic_tool_audio,
        widgetDescriptionRes = R.string.widget_description_audio,
        widgetReceiver = AudioWidgetReceiver::class.java,
    ),
    TIMER(
        route = "timer",
        titleRes = R.string.tool_timer,
        subtitleRes = R.string.subtitle_timer,
        iconRes = R.drawable.ic_tool_timer,
        widgetDescriptionRes = R.string.widget_description_timer,
        widgetReceiver = TimerWidgetReceiver::class.java,
    ),
    MEASURE(
        route = "measure",
        titleRes = R.string.tool_measure,
        subtitleRes = R.string.subtitle_measure,
        iconRes = R.drawable.ic_tool_measure,
        widgetDescriptionRes = R.string.widget_description_measure,
        widgetReceiver = MeasureWidgetReceiver::class.java,
    ),
    MAGNIFIER(
        route = "magnifier",
        titleRes = R.string.tool_magnifier,
        subtitleRes = R.string.subtitle_magnifier,
        iconRes = R.drawable.ic_tool_magnifier,
        widgetDescriptionRes = R.string.widget_description_magnifier,
        widgetReceiver = MagnifierWidgetReceiver::class.java,
    ),
    ASTRO(
        route = "astro",
        titleRes = R.string.tool_astro,
        subtitleRes = R.string.subtitle_astro,
        iconRes = R.drawable.ic_tool_astro,
        widgetDescriptionRes = R.string.widget_description_astro,
        widgetReceiver = AstroWidgetReceiver::class.java,
    ),
    ;

    companion object {
        fun fromRoute(route: String?): Tool? = entries.firstOrNull { it.route == route }

        /**
         * Applies the persisted home-grid order (list of routes); unknown routes are ignored
         * and tools added in later versions are appended at the end.
         */
        fun orderedBy(routes: List<String>): List<Tool> {
            val byRoute = entries.associateBy { it.route }
            val ordered = routes.mapNotNull { byRoute[it] }
            return ordered + entries.filterNot { it in ordered }
        }
    }
}
