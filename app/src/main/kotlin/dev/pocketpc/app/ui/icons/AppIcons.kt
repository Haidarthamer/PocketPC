package dev.pocketpc.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-authored vector icons for the desktop.
 *
 * Written as path data rather than pulled from a font/Material icon set so the desktop
 * has one coherent visual language and no dependency on an icon font that would not
 * match the brand. Each icon is drawn inside a 24x24 viewport.
 */
object AppIcons {

    private fun icon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(build).build()

    private fun ImageVector.Builder.stroke(color: Color, width: Float = 1.7f, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        path(
            stroke = SolidColor(color),
            strokeLineWidth = width,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
            fill = null,
            pathBuilder = block,
        )
    }

    private fun ImageVector.Builder.fill(color: Color, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        path(fill = SolidColor(color), pathBuilder = block)
    }

    val folder: ImageVector = icon("folder") {
        stroke(Color.White) {
            moveTo(3f, 7.5f); lineTo(3f, 18f); lineTo(21f, 18f); lineTo(21f, 8.5f); lineTo(12f, 8.5f)
            lineTo(10f, 6f); lineTo(3f, 6f); close()
        }
        fill(Color.White.copy(alpha = 0.18f)) {
            moveTo(4.5f, 10f); lineTo(19.5f, 10f); lineTo(19.5f, 16.5f); lineTo(4.5f, 16.5f); close()
        }
    }

    val terminal: ImageVector = icon("terminal") {
        stroke(Color.White) {
            moveTo(3f, 4.5f); lineTo(21f, 4.5f); lineTo(21f, 19.5f); lineTo(3f, 19.5f); close()
        }
        stroke(Color.White) {
            moveTo(6.5f, 9.5f); lineTo(9.5f, 12f); lineTo(6.5f, 14.5f)
        }
        stroke(Color.White) {
            moveTo(11.5f, 15f); lineTo(16f, 15f)
        }
    }

    val editor: ImageVector = icon("editor") {
        stroke(Color.White) {
            moveTo(5f, 3.5f); lineTo(14f, 3.5f); lineTo(19f, 8.5f); lineTo(19f, 20.5f); lineTo(5f, 20.5f); close()
        }
        stroke(Color.White) {
            moveTo(14f, 3.5f); lineTo(14f, 8.5f); lineTo(19f, 8.5f)
        }
        stroke(Color.White) {
            moveTo(8f, 12.5f); lineTo(16f, 12.5f); moveTo(8f, 16f); lineTo(14f, 16f)
        }
    }

    val calculator: ImageVector = icon("calculator") {
        stroke(Color.White) {
            moveTo(5f, 3.5f); lineTo(19f, 3.5f); lineTo(19f, 20.5f); lineTo(5f, 20.5f); close()
        }
        stroke(Color.White) {
            moveTo(7.5f, 6.5f); lineTo(16.5f, 6.5f); lineTo(16.5f, 9.5f); lineTo(7.5f, 9.5f); close()
        }
        fill(Color.White) {
            moveTo(8f, 12.5f); lineTo(10f, 12.5f); lineTo(10f, 14.5f); lineTo(8f, 14.5f); close()
            moveTo(12f, 12.5f); lineTo(14f, 12.5f); lineTo(14f, 14.5f); lineTo(12f, 14.5f); close()
            moveTo(16f, 12.5f); lineTo(18f, 12.5f); lineTo(18f, 14.5f); lineTo(16f, 14.5f); close()
            moveTo(8f, 16.5f); lineTo(10f, 16.5f); lineTo(10f, 18.5f); lineTo(8f, 18.5f); close()
            moveTo(12f, 16.5f); lineTo(14f, 16.5f); lineTo(14f, 18.5f); lineTo(12f, 18.5f); close()
            moveTo(16f, 16.5f); lineTo(18f, 16.5f); lineTo(18f, 18.5f); lineTo(16f, 18.5f); close()
        }
    }

    val taskmanager: ImageVector = icon("taskmanager") {
        stroke(Color.White) {
            moveTo(3f, 4.5f); lineTo(21f, 4.5f); lineTo(21f, 19.5f); lineTo(3f, 19.5f); close()
            moveTo(3f, 9f); lineTo(21f, 9f)
        }
        fill(Color.White) {
            moveTo(6f, 11.5f); lineTo(8f, 11.5f); lineTo(8f, 17f); lineTo(6f, 17f); close()
            moveTo(11f, 13.5f); lineTo(13f, 13.5f); lineTo(13f, 17f); lineTo(11f, 17f); close()
            moveTo(16f, 12f); lineTo(18f, 12f); lineTo(18f, 17f); lineTo(16f, 17f); close()
        }
    }

    val monitor: ImageVector = icon("monitor") {
        stroke(Color.White) {
            moveTo(3f, 4.5f); lineTo(21f, 4.5f); lineTo(21f, 17f); lineTo(3f, 17f); close()
            moveTo(9f, 20.5f); lineTo(15f, 20.5f); moveTo(12f, 17f); lineTo(12f, 20.5f)
        }
        stroke(Color.White) {
            moveTo(6f, 13.5f); lineTo(9f, 9f); lineTo(12f, 12f); lineTo(15f, 7.5f); lineTo(18f, 11f)
        }
    }

    val gpu: ImageVector = icon("gpu") {
        stroke(Color.White) {
            moveTo(6f, 6f); lineTo(18f, 6f); lineTo(18f, 18f); lineTo(6f, 18f); close()
        }
        stroke(Color.White) {
            moveTo(9.5f, 9.5f); lineTo(14.5f, 9.5f); lineTo(14.5f, 14.5f); lineTo(9.5f, 14.5f); close()
        }
        stroke(Color.White, 1.4f) {
            moveTo(9f, 3f); lineTo(9f, 6f); moveTo(15f, 3f); lineTo(15f, 6f)
            moveTo(9f, 18f); lineTo(9f, 21f); moveTo(15f, 18f); lineTo(15f, 21f)
            moveTo(3f, 9f); lineTo(6f, 9f); moveTo(3f, 15f); lineTo(6f, 15f)
            moveTo(18f, 9f); lineTo(21f, 9f); moveTo(18f, 15f); lineTo(21f, 15f)
        }
    }

    val settings: ImageVector = icon("settings") {
        stroke(Color.White) {
            moveTo(12f, 9f); curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
            curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
            curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
            curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f); close()
            moveTo(12f, 3f); lineTo(12f, 5.2f); moveTo(12f, 18.8f); lineTo(12f, 21f)
            moveTo(3f, 12f); lineTo(5.2f, 12f); moveTo(18.8f, 12f); lineTo(21f, 12f)
            moveTo(5.6f, 5.6f); lineTo(7.2f, 7.2f); moveTo(16.8f, 16.8f); lineTo(18.4f, 18.4f)
            moveTo(18.4f, 5.6f); lineTo(16.8f, 7.2f); moveTo(7.2f, 16.8f); lineTo(5.6f, 18.4f)
        }
    }

    val info: ImageVector = icon("info") {
        stroke(Color.White) {
            moveTo(12f, 3.2f); curveTo(16.86f, 3.2f, 20.8f, 7.14f, 20.8f, 12f)
            curveTo(20.8f, 16.86f, 16.86f, 20.8f, 12f, 20.8f)
            curveTo(7.14f, 20.8f, 3.2f, 16.86f, 3.2f, 12f)
            curveTo(3.2f, 7.14f, 7.14f, 3.2f, 12f, 3.2f); close()
        }
        fill(Color.White) {
            moveTo(11f, 10.5f); lineTo(13f, 10.5f); lineTo(13f, 17f); lineTo(11f, 17f); close()
            moveTo(11f, 6.8f); lineTo(13f, 6.8f); lineTo(13f, 8.9f); lineTo(11f, 8.9f); close()
        }
    }

    val start: ImageVector = icon("start") {
        fill(Color.White) {
            moveTo(4f, 4.6f); lineTo(10.4f, 3.7f); lineTo(10.4f, 10.6f); lineTo(4f, 10.6f); close()
            moveTo(11.6f, 3.5f); lineTo(20f, 2.2f); lineTo(20f, 10.6f); lineTo(11.6f, 10.6f); close()
            moveTo(4f, 11.8f); lineTo(10.4f, 11.8f); lineTo(10.4f, 20.3f); lineTo(4f, 19.4f); close()
            moveTo(11.6f, 11.8f); lineTo(20f, 11.8f); lineTo(20f, 21.8f); lineTo(11.6f, 20.4f); close()
        }
    }

    val close: ImageVector = icon("close") {
        stroke(Color.White, 1.8f) {
            moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }

    val minimize: ImageVector = icon("minimize") {
        stroke(Color.White, 1.8f) { moveTo(5.5f, 12f); lineTo(18.5f, 12f) }
    }

    val maximize: ImageVector = icon("maximize") {
        stroke(Color.White, 1.7f) {
            moveTo(5.5f, 5.5f); lineTo(18.5f, 5.5f); lineTo(18.5f, 18.5f); lineTo(5.5f, 18.5f); close()
        }
    }

    val restore: ImageVector = icon("restore") {
        stroke(Color.White, 1.6f) {
            moveTo(4.5f, 8.5f); lineTo(15.5f, 8.5f); lineTo(15.5f, 19.5f); lineTo(4.5f, 19.5f); close()
            moveTo(8.5f, 8.5f); lineTo(8.5f, 4.5f); lineTo(19.5f, 4.5f); lineTo(19.5f, 15.5f); lineTo(15.5f, 15.5f)
        }
    }

    val search: ImageVector = icon("search") {
        stroke(Color.White, 1.8f) {
            moveTo(16.2f, 10.6f); curveTo(16.2f, 13.69f, 13.69f, 16.2f, 10.6f, 16.2f)
            curveTo(7.51f, 16.2f, 5f, 13.69f, 5f, 10.6f)
            curveTo(5f, 7.51f, 7.51f, 5f, 10.6f, 5f)
            curveTo(13.69f, 5f, 16.2f, 7.51f, 16.2f, 10.6f); close()
            moveTo(14.6f, 14.6f); lineTo(19.5f, 19.5f)
        }
    }

    val power: ImageVector = icon("power") {
        stroke(Color.White, 1.8f) {
            moveTo(12f, 3.5f); lineTo(12f, 11f)
            moveTo(7.2f, 6.2f); curveTo(4.9f, 7.9f, 3.6f, 10.5f, 4.1f, 13.6f)
            curveTo(4.7f, 17.1f, 7.9f, 19.6f, 11.5f, 19.6f)
            curveTo(15.1f, 19.6f, 18.3f, 17.1f, 18.9f, 13.6f)
            curveTo(19.4f, 10.5f, 18.1f, 7.9f, 15.8f, 6.2f)
        }
    }

    val chevronUp: ImageVector = icon("chevronUp") {
        stroke(Color.White, 2f) { moveTo(6f, 15f); lineTo(12f, 9f); lineTo(18f, 15f) }
    }

    /** Resolves an [AppDefinition.iconKey] to its vector. */
    fun forKey(key: String): ImageVector = when (key) {
        "folder" -> folder
        "terminal" -> terminal
        "editor" -> editor
        "calculator" -> calculator
        "taskmanager" -> taskmanager
        "monitor" -> monitor
        "gpu" -> gpu
        "settings" -> settings
        "info" -> info
        else -> info
    }
}
