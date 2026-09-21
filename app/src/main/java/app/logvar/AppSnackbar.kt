package app.logvar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class NoticeKind { SUCCESS, INFO, STOPPED, ERROR }

// Each operation gets a new identity, so repeated messages are displayed again.
internal class AppNotice(
    override val message: String,
    val kind: NoticeKind = NoticeKind.SUCCESS
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction = kind == NoticeKind.ERROR
    override val duration = if (kind == NoticeKind.ERROR) SnackbarDuration.Long else SnackbarDuration.Short
}

@Composable
internal fun AppSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        val kind = (data.visuals as? AppNotice)?.kind ?: NoticeKind.INFO
        val colors = MaterialTheme.colorScheme
        val background = colors.primaryContainer
        val foreground = colors.onPrimaryContainer
        val icon = when (kind) {
            NoticeKind.SUCCESS -> Icons.Rounded.CheckCircle
            NoticeKind.INFO -> Icons.Rounded.Info
            NoticeKind.STOPPED -> Icons.Rounded.StopCircle
            NoticeKind.ERROR -> Icons.Rounded.ErrorOutline
        }
        var entered by remember(data) { mutableStateOf(false) }
        LaunchedEffect(data) { entered = true }
        val offset by animateDpAsState(if (entered) 0.dp else 8.dp, spring(), label = "notice entrance")
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = 360.dp).offset(y = offset),
            shape = RoundedCornerShape(28.dp),
            color = background,
            contentColor = foreground,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(data.visuals.message, modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                if (data.visuals.withDismissAction) {
                    IconButton(onClick = { data.dismiss() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭提示", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        }
    }
}
