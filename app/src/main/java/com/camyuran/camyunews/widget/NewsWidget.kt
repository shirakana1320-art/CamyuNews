package com.camyuran.camyunews.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.camyuran.camyunews.MainActivity
import com.camyuran.camyunews.data.local.CamyuNewsDatabase
import com.camyuran.camyunews.data.local.MIGRATION_1_2
import com.camyuran.camyunews.util.todayDateKey
import kotlinx.coroutines.flow.firstOrNull

class NewsWidget : GlanceAppWidget() {

    companion object {
        suspend fun updateAll(context: Context) {
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(NewsWidget::class.java)
            ids.forEach { id -> NewsWidget().update(context, id) }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = CamyuNewsDatabase::class.java
            .let {
                androidx.room.Room.databaseBuilder(context, it, "camyunews.db")
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
            }
        val todayKey = todayDateKey()
        val articles = db.articleDao()
            .getArticlesByDate(todayKey)
            .firstOrNull()
            ?.take(3)
            ?: emptyList()
        db.close()

        provideContent {
            WidgetContent(articles.map { it.titleJa }, context)
        }
    }
}

@Composable
private fun WidgetContent(
    titles: List<String>,
    context: Context
) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Text(
                "CamyuNews",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.primary
                )
            )
            Spacer(GlanceModifier.height(8.dp))
            if (titles.isEmpty()) {
                Text(
                    "記事がありません",
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface)
                )
            } else {
                titles.forEachIndexed { i, title ->
                    if (i > 0) Spacer(GlanceModifier.height(4.dp))
                    Text(
                        "・$title",
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

class NewsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewsWidget()
}
