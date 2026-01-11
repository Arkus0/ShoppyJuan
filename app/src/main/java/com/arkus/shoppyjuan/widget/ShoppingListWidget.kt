package com.arkus.shoppyjuan.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arkus.shoppyjuan.presentation.MainActivity
import com.arkus.shoppyjuan.data.local.ShoppyDatabase
import kotlinx.coroutines.flow.first

class ShoppingListWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = ShoppyDatabase.getInstance(context)
        val lists = database.shoppingListDao().getAllLists().first()
        val activeList = lists.firstOrNull { !it.isArchived }

        val items = if (activeList != null) {
            database.listItemDao().getUncheckedItems(activeList.id).first()
        } else {
            emptyList()
        }

        provideContent {
            ShoppingListWidgetContent(
                listName = activeList?.name ?: "Sin listas",
                items = items.map { WidgetItem(it.id, it.name, it.emoji, it.checked) },
                totalItems = items.size
            )
        }
    }
}

data class WidgetItem(
    val id: String,
    val name: String,
    val emoji: String?,
    val checked: Boolean
)

@Composable
private fun ShoppingListWidgetContent(
    listName: String,
    items: List<WidgetItem>,
    totalItems: Int
) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Text(
                    text = "$totalItems pendientes",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Items list
            if (items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lista vacia",
                        style = TextStyle(
                            color = ColorProvider(
                                day = android.graphics.Color.GRAY,
                                night = android.graphics.Color.LTGRAY
                            ),
                            fontSize = 14.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    items(items.take(8)) { item ->
                        WidgetItemRow(item)
                    }

                    if (items.size > 8) {
                        item {
                            Text(
                                text = "+${items.size - 8} mas...",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 12.sp
                                ),
                                modifier = GlanceModifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Footer with tap hint
            Text(
                text = "Toca para abrir",
                style = TextStyle(
                    color = ColorProvider(
                        day = android.graphics.Color.GRAY,
                        night = android.graphics.Color.LTGRAY
                    ),
                    fontSize = 10.sp
                ),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WidgetItemRow(item: WidgetItem) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bullet point
        Text(
            text = item.emoji ?: "•",
            style = TextStyle(fontSize = 14.sp)
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        Text(
            text = item.name,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

class ShoppingListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShoppingListWidget()
}
