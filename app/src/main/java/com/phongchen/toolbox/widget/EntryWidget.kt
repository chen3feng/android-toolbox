package com.phongchen.toolbox.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.phongchen.toolbox.MainActivity
import com.phongchen.toolbox.core.Tool

private val WidgetBackground = ColorProvider(
    day = Color(0xFFEFF3F8),
    night = Color(0xFF23272E),
)
private val WidgetForeground = ColorProvider(
    day = Color(0xFF15202B),
    night = Color(0xFFE6EAF0),
)

/** 1x1 shortcut widget: icon + label, tap opens the tool directly via a deep-link intent. */
class EntryWidget(private val tool: Tool) : GlanceAppWidget() {

    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        provideContent { WidgetContent(tool) }
    }

    @Composable
    private fun WidgetContent(tool: Tool) {
        val context = LocalContext.current
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_TOOL_ROUTE, tool.route)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(2.dp)
                .background(WidgetBackground)
                .cornerRadius(20.dp)
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(tool.iconRes),
                    contentDescription = context.getString(tool.widgetDescriptionRes),
                    colorFilter = androidx.glance.ColorFilter.tint(WidgetForeground),
                    modifier = GlanceModifier.size(30.dp),
                )
                Text(
                    text = context.getString(tool.titleRes),
                    style = TextStyle(
                        color = WidgetForeground,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(top = 2.dp),
                )
            }
        }
    }
}
