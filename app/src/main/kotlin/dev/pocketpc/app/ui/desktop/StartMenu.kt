package dev.pocketpc.app.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketpc.app.ui.icons.AppIcons
import dev.pocketpc.app.ui.theme.Brand
import dev.pocketpc.desktop.apps.AppDefinition
import dev.pocketpc.desktop.apps.AppRegistry
import dev.pocketpc.desktop.shell.ShellModel

/**
 * Start menu: search field, recent apps, full app list and power/session options.
 *
 * The list is driven by [AppRegistry.search], which is unit tested for ranking, so
 * typing "te" reliably puts Terminal first.
 */
@Composable
fun StartMenu(
    shell: ShellModel,
    dark: Boolean,
    onLaunch: (AppDefinition) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { AppRegistry.search(query) }
    val recents = remember(shell.recentApps) { shell.recentApps }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val surface = if (dark) Brand.Surface else Color.White
    val border = if (dark) Brand.Border else Brand.LightBorder
    val fg = if (dark) Brand.TextPrimary else Brand.LightTextPrimary
    val subtle = if (dark) Brand.TextSecondary else Brand.LightTextSecondary

    Column(
        modifier = Modifier
            .width(340.dp)
            .heightIn(max = 520.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(14.dp)),
    ) {
        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (dark) Brand.NavyDeep.copy(alpha = 0.7f) else Color(0xFFEFF4FB))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.search, contentDescription = null, tint = subtle, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = fg),
                cursorBrush = SolidColor(Brand.Blue),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search apps", fontSize = 13.sp, color = subtle)
                    }
                    inner()
                },
            )
            if (query.isNotEmpty()) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clickable { query = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.close, contentDescription = "Clear search", tint = subtle, modifier = Modifier.size(12.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 360.dp),
        ) {
            if (query.isEmpty() && recents.isNotEmpty()) {
                item { SectionHeader("Recent", subtle) }
                items(recents, key = { "recent-" + it.id }) { app ->
                    AppRow(app, dark, onClick = { onLaunch(app) }, onPin = { shell.togglePin(app.id) }, pinned = shell.isPinned(app.id))
                }
                item { SectionHeader("All apps", subtle) }
            }
            if (results.isEmpty()) {
                item {
                    Text(
                        "No apps match \"$query\"",
                        fontSize = 12.sp,
                        color = subtle,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(results, key = { it.id }) { app ->
                AppRow(app, dark, onClick = { onLaunch(app) }, onPin = { shell.togglePin(app.id) }, pinned = shell.isPinned(app.id))
            }
        }

        // Footer: system shortcuts + power
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (dark) Brand.SurfaceRaised.copy(alpha = 0.6f) else Color(0xFFEFF4FB))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterAction("Settings", AppIcons.settings, dark) { onSettings(); onDismiss() }
            FooterAction("About", AppIcons.info, dark) { onAbout(); onDismiss() }
            Spacer(Modifier.weight(1f))
            FooterAction("Power", AppIcons.power, dark, tint = Brand.Amber) {
                onLaunch(AppRegistry.find("settings")!!)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(
        text.uppercase(),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun AppRow(
    app: AppDefinition,
    dark: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    pinned: Boolean,
) {
    val fg = if (dark) Brand.TextPrimary else Brand.LightTextPrimary
    val subtle = if (dark) Brand.TextSecondary else Brand.LightTextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(app.id, size = 24)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(app.name, fontSize = 13.sp, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.description, fontSize = 10.sp, color = subtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (pinned) {
            Text("PINNED", fontSize = 8.sp, color = Brand.Blue, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .size(26.dp)
                .clickable(onClick = onPin),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (pinned) "−" else "+", fontSize = 14.sp, color = subtle)
        }
    }
}

@Composable
private fun FooterAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    dark: Boolean,
    tint: Color = if (dark) Brand.TextSecondary else Brand.LightTextSecondary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.5.sp, color = tint)
    }
}
