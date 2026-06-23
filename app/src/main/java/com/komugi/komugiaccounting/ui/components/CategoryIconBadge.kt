package com.komugi.komugiaccounting.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.komugiaccounting.data.model.Category

@Composable
fun CategoryIconBadge(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    cornerRadius: Dp = 14.dp
) {
    CategoryIconBadge(
        name = category.displayNameForIcon(),
        iconName = if (category.name.startsWith("__group__")) category.iconName else category.name.firstIconText(),
        color = category.color,
        iconImageUri = category.iconImageUri,
        modifier = modifier,
        size = size,
        cornerRadius = cornerRadius
    )
}

@Composable
fun CategoryIconBadge(
    name: String,
    iconName: String,
    color: String,
    iconImageUri: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    cornerRadius: Dp = 14.dp
) {
    val themeColor = parseCategoryColor(color)
    val context = LocalContext.current
    val bitmap = remember(iconImageUri) {
        if (iconImageUri.isBlank()) {
            null
        } else {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(iconImageUri))?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(themeColor.copy(alpha = if (MaterialTheme.colorScheme.background == Color.Black) 0.72f else 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
            )
        } else {
            Text(
                text = iconText(iconName, name),
                color = if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFFEDEDED) else themeColor,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.38f).sp
            )
        }
    }
}

fun parseCategoryColor(value: String, fallback: Color = Color(0xFF9E9E9E)): Color =
    runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)

fun Category.displayNameForIcon(): String =
    if (name.startsWith("__group__")) groupName else name

private fun iconText(iconName: String, name: String): String =
    iconName.trim().ifBlank { name.trim().firstOrNull()?.toString().orEmpty() }.take(2)

private fun String.firstIconText(): String =
    trim().firstOrNull()?.toString().orEmpty()
