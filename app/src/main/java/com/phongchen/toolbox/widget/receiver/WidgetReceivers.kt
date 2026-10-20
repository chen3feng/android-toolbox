package com.phongchen.toolbox.widget.receiver

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.phongchen.toolbox.core.Tool
import com.phongchen.toolbox.widget.EntryWidget

class CompassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.COMPASS)
}

class LightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.LIGHT)
}

class AltimeterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.ALTIMETER)
}

class FlashlightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.FLASHLIGHT)
}

class LevelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.LEVEL)
}

class SoundWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.SOUND)
}

class SpeedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.SPEED)
}

class MetalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.METAL)
}

class StepWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.STEP)
}

class GpsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.GPS)
}

class VibrationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.VIBRATION)
}

class AudioWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.AUDIO)
}

class TimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.TIMER)
}

class MeasureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.MEASURE)
}

class MagnifierWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EntryWidget(Tool.MAGNIFIER)
}
