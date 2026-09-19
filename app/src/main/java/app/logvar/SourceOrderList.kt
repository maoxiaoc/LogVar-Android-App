package app.logvar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
internal fun SourceOrderList(
    order: List<String>, enabled: Set<String>, onOrder: (List<String>) -> Unit,
    onEnabled: (String, Boolean) -> Unit
) {
    val currentOrder by rememberUpdatedState(order)
    val updateOrder by rememberUpdatedState(onOrder)
    val rowHeight = with(LocalDensity.current) { 48.dp.toPx() }
    var active by remember { mutableStateOf<String?>(null) }
    var dragTop by remember { mutableFloatStateOf(0f) }
    // Keep composition/gesture ownership stable; only the visual slots change.
    val identities = remember { order.toList() }
    fun move(id: String, destination: Int) {
        val next = currentOrder.toMutableList()
        val from = next.indexOf(id)
        if (from >= 0 && destination in next.indices && from != destination) {
            next.add(destination, next.removeAt(from))
            updateOrder(next)
        }
    }
    Surface(shape=RoundedCornerShape(20.dp), color=MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(Modifier.fillMaxWidth().padding(vertical=8.dp).height(48.dp * order.size)) {
            identities.forEach { id -> key(id) {
                val dragging = active == id
                val position = order.indexOf(id)
                // Drag directly follows the finger. Only neighbouring rows and release animate.
                val target = if (dragging) dragTop else position * rowHeight
                val animatedTop by animateFloatAsState(target, spring(dampingRatio=1f, stiffness=700f), label="source position")
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(horizontal=8.dp)
                        .zIndex(if(dragging) 1f else 0f)
                        .graphicsLayer { translationY=if(dragging) dragTop else animatedTop }
                        .background(if(dragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp)),
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    Box(Modifier.size(48.dp).semantics {
                        customActions=listOf(
                            CustomAccessibilityAction("提高优先级") { move(id, position-1); position>0 },
                            CustomAccessibilityAction("降低优先级") { move(id, position+1); position<order.lastIndex }
                        )
                    }.pointerInput(id, rowHeight) {
                        var startTop=0f
                        var distance=0f
                        var before=currentOrder
                        detectDragGestures(
                            onDragStart={
                                before=currentOrder.toList()
                                startTop=currentOrder.indexOf(id)*rowHeight
                                distance=0f; dragTop=startTop; active=id
                            },
                            onDragEnd={ active=null },
                            onDragCancel={ updateOrder(before); active=null },
                            onDrag={ change, amount ->
                                change.consume()
                                distance+=amount.y
                                dragTop=SourceDrag.top(startTop, distance, rowHeight, currentOrder.size)
                                move(id, SourceDrag.slot(dragTop, rowHeight, currentOrder.size))
                            }
                        )
                    }, contentAlignment=Alignment.Center) {
                        Icon(Icons.Rounded.DragHandle,"拖动调整${sourceByKey(id).label}优先级", tint=MaterialTheme.colorScheme.primary)
                    }
                    Checkbox(id in enabled, { onEnabled(id,it) })
                    Text(sourceByKey(id).label, style=MaterialTheme.typography.bodyLarge,
                        color=if(id in enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.5f))
                }
            } }
        }
    }
}
