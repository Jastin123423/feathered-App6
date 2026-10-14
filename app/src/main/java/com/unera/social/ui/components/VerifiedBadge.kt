package com.unera.social.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser

private const val ROSETTE_PATH_DATA =
    "M 10.85 2.14 Q 12.00 1.00 13.15 2.14 Q 14.30 3.29 15.90 2.92 Q 17.50 2.54 17.92 4.10 Q 18.34 5.66 19.90 6.10 Q 21.46 6.54 21.08 8.10 Q 20.71 9.70 21.86 10.85 Q 23.00 12.00 21.86 13.15 Q 20.71 14.30 21.08 15.90 Q 21.46 17.50 19.90 17.92 Q 18.34 18.34 17.92 19.90 Q 17.50 21.46 15.90 21.08 Q 14.30 20.71 13.15 21.86 Q 12.00 23.00 10.85 21.86 Q 9.70 20.71 8.10 21.08 Q 6.54 21.46 6.10 19.90 Q 5.66 18.34 4.10 17.92 Q 2.54 17.50 2.92 15.90 Q 3.29 14.30 2.14 13.15 Q 1.00 12.00 2.14 10.85 Q 3.29 9.70 2.92 8.10 Q 2.54 6.54 4.10 6.10 Q 5.66 5.66 6.10 4.10 Q 6.54 2.54 8.10 2.92 Q 9.70 3.29 10.85 2.14 Z"

private const val CHECK_PATH_DATA =
    "M 7.3 12.3 L 10.4 15.4 L 16.7 8.7"

/**
 * Modern 12-point scalloped starburst verification badge.
 * Designed with smooth, professionally rounded tips and valleys
 * and a centered, clean white checkmark.
 * Sized to match user name typography.
 */
@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color(0xFF1877F2),
    title: String = "Verified Account"
) {
    val rosettePath = remember {
        PathParser.createPathFromPathData(ROSETTE_PATH_DATA).asComposePath()
    }
    val checkPath = remember {
        PathParser.createPathFromPathData(CHECK_PATH_DATA).asComposePath()
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = title },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 24f
            scale(scaleFactor, pivot = Offset.Zero) {
                // 12-Point Scalloped Starburst Rosette with rounded tips and valleys
                drawPath(
                    path = rosettePath,
                    color = color
                )
                // Precision Centered White Tick Mark
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
