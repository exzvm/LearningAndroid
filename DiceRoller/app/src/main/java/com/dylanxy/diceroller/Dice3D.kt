package com.dylanxy.diceroller

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** 立方体一个面的点数、外法线方向、四个顶点索引。 */
private class DieFace(
    val value: Int,
    val normal: FloatArray,
    val corners: IntArray,
)

private val DIE_FACES = listOf(
    DieFace(1, floatArrayOf(0f, 0f, 1f), intArrayOf(4, 5, 6, 7)),   // 前
    DieFace(6, floatArrayOf(0f, 0f, -1f), intArrayOf(1, 0, 3, 2)),  // 后
    DieFace(3, floatArrayOf(1f, 0f, 0f), intArrayOf(5, 1, 2, 6)),   // 右
    DieFace(4, floatArrayOf(-1f, 0f, 0f), intArrayOf(0, 4, 7, 3)),  // 左
    DieFace(2, floatArrayOf(0f, -1f, 0f), intArrayOf(0, 1, 5, 4)),  // 上（屏幕坐标 y 向下）
    DieFace(5, floatArrayOf(0f, 1f, 0f), intArrayOf(3, 2, 6, 7)),   // 下
)

/** 单位立方体的 8 个顶点。 */
private val CUBE_VERTICES = arrayOf(
    floatArrayOf(-1f, -1f, -1f),
    floatArrayOf(1f, -1f, -1f),
    floatArrayOf(1f, 1f, -1f),
    floatArrayOf(-1f, 1f, -1f),
    floatArrayOf(-1f, -1f, 1f),
    floatArrayOf(1f, -1f, 1f),
    floatArrayOf(1f, 1f, 1f),
    floatArrayOf(-1f, 1f, 1f),
)

/** 各点数在面内的相对位置（u, v 均在 0..1）。 */
private val PIP_LAYOUTS = mapOf(
    1 to listOf(0.5f to 0.5f),
    2 to listOf(0.28f to 0.28f, 0.72f to 0.72f),
    3 to listOf(0.26f to 0.26f, 0.5f to 0.5f, 0.74f to 0.74f),
    4 to listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f),
    5 to listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.5f to 0.5f, 0.28f to 0.72f, 0.72f to 0.72f),
    6 to listOf(
        0.28f to 0.22f, 0.72f to 0.22f,
        0.28f to 0.5f, 0.72f to 0.5f,
        0.28f to 0.78f, 0.72f to 0.78f,
    ),
)

private val BASE_FACE_COLOR = Color(0xFFE1BEE7)
private val PIP_COLOR = Color(0xFF1F1F24)
private val BEVEL_COLOR = Color(0xFF6B5773)
private val LIGHT = normalize(-0.35f, -0.75f, 0.55f)

/** 固定的观察角度：让骰子始终略微侧倾，能看到顶面和侧面，而不是正对一个平面。 */
private const val VIEW_TILT_X = -20f
private const val VIEW_TILT_Y = -24f

/**
 * 用 Compose Canvas 画的真正 3D 骰子：6 个面、透视投影、背面剔除、逐面光照。
 *
 * @param rotationX 绕 X 轴旋转的角度（度）
 * @param rotationY 绕 Y 轴旋转的角度（度）
 * @param rotationZ 绕 Z 轴旋转的角度（度）
 * @param tossProgress 抛掷进度 0..1，用于计算腾空高度与缩放
 * @param bounceProgress 落地余弹进度 0..1
 */
@Composable
fun Dice3D(
    rotationX: Float,
    rotationY: Float,
    rotationZ: Float,
    tossProgress: Float,
    bounceProgress: Float,
    description: String,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.semantics { this.contentDescription = description }
    ) {
        drawDice(rotationX, rotationY, rotationZ, tossProgress, bounceProgress)
    }
}

private fun DrawScope.drawDice(
    rotationXDeg: Float,
    rotationYDeg: Float,
    rotationZDeg: Float,
    tossProgress: Float,
    bounceProgress: Float,
) {
    val half = size.minDimension * 0.20f
    val center = Offset(size.width / 2f, size.height / 2f)
    val toss = sin(tossProgress * PI).toFloat()
    val bounce = sin(bounceProgress * PI).toFloat() * (1f - bounceProgress)
    val lift = (-toss * 50f - bounce * 10f).dp.toPx()
    val scale = 1f + toss * 0.08f
    val halfScaled = half * scale
    val camera = halfScaled * 6f

    val rx = Math.toRadians(rotationXDeg.toDouble()).toFloat()
    val ry = Math.toRadians(rotationYDeg.toDouble()).toFloat()
    val rz = Math.toRadians(rotationZDeg.toDouble()).toFloat()
    val viewRx = Math.toRadians(VIEW_TILT_X.toDouble()).toFloat()
    val viewRy = Math.toRadians(VIEW_TILT_Y.toDouble()).toFloat()

    // 先按骰子的逻辑角度旋转，再叠加固定的观察角度，最后做透视投影
    val projected = CUBE_VERTICES.map { v ->
        val p = rotateVec(v[0] * halfScaled, v[1] * halfScaled, v[2] * halfScaled, rx, ry, rz)
        val tilted = rotateVec(p[0], p[1], p[2], viewRx, viewRy, 0f)
        val perspective = camera / (camera - tilted[2])
        Offset(
            x = center.x + tilted[0] * perspective,
            y = center.y + tilted[1] * perspective + lift,
        )
    }

    // 地面阴影：骰子飞得越高，阴影越小越淡
    val shadowFactor = (1f - toss * 0.55f).coerceIn(0.45f, 1f)
    val shadowRadius = halfScaled * 0.68f * shadowFactor
    drawOval(
        color = Color.Black.copy(alpha = 0.13f * shadowFactor),
        topLeft = Offset(
            x = center.x - shadowRadius,
            y = center.y + halfScaled * 1.45f - shadowRadius * 0.18f,
        ),
        size = Size(shadowRadius * 2f, shadowRadius * 0.36f),
    )

    // 背面剔除：法线转到相机方向后才可见
    val visibleFaces = DIE_FACES.mapNotNull { face ->
        val n = rotateVec(face.normal[0], face.normal[1], face.normal[2], rx, ry, rz)
        val normal = rotateVec(n[0], n[1], n[2], viewRx, viewRy, 0f)
        if (normal[2] <= 0.001f) null else face to normal
    }

    // 先给每个可见面铺一层深色底板，形成倒角缝隙
    for ((face, _) in visibleFaces) {
        drawPath(
            path = polygonPath(face.corners.map { projected[it] }),
            color = BEVEL_COLOR,
        )
    }

    // 再画内缩的彩色面和点数
    for ((face, normal) in visibleFaces) {
        val quad = face.corners.map { projected[it] }
        val insetQuad = insetQuad(quad, 0.05f)
        val shade = 0.62f + 0.38f * max(0f, dot(normal, LIGHT))
        drawPath(
            path = polygonPath(insetQuad),
            color = faceColor(shade),
        )
        drawPips(face.value, insetQuad)
    }
}

/** 将点按 X→Y→Z 的顺序旋转。 */
private fun rotateVec(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float): FloatArray {
    val cx = cos(rx)
    val sx = sin(rx)
    val cy = cos(ry)
    val sy = sin(ry)
    val cz = cos(rz)
    val sz = sin(rz)

    val y1 = y * cx - z * sx
    val z1 = y * sx + z * cx
    val x1 = x

    val x2 = x1 * cy + z1 * sy
    val z2 = -x1 * sy + z1 * cy
    val y2 = y1

    val x3 = x2 * cz - y2 * sz
    val y3 = x2 * sz + y2 * cz

    return floatArrayOf(x3, y3, z2)
}

private fun polygonPath(points: List<Offset>): Path = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        lineTo(points[i].x, points[i].y)
    }
    close()
}

/** 把四个角朝中心收缩，留出倒角缝隙。 */
private fun insetQuad(quad: List<Offset>, amount: Float): List<Offset> {
    val cx = quad.sumOf { it.x.toDouble() }.toFloat() / quad.size
    val cy = quad.sumOf { it.y.toDouble() }.toFloat() / quad.size
    return quad.map { p ->
        Offset(
            x = cx + (p.x - cx) * (1f - amount),
            y = cy + (p.y - cy) * (1f - amount),
        )
    }
}

/** 在面内通过双线性插值放置点数。 */
private fun DrawScope.drawPips(value: Int, quad: List<Offset>) {
    val p0 = quad[0]
    val p1 = quad[1]
    val p2 = quad[2]
    val p3 = quad[3]

    val width = hypot(p1.x - p0.x, p1.y - p0.y)
    val height = hypot(p3.x - p0.x, p3.y - p0.y)
    val radius = min(width, height) * 0.066f

    PIP_LAYOUTS.getValue(value).forEach { (u, v) ->
        val top = lerpOffset(p0, p1, u)
        val bottom = lerpOffset(p3, p2, u)
        val center = lerpOffset(top, bottom, v)
        drawCircle(color = PIP_COLOR, radius = radius, center = center)
    }
}

private fun lerpOffset(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun faceColor(shade: Float): Color = Color(
    red = (BASE_FACE_COLOR.red * shade).coerceIn(0f, 1f),
    green = (BASE_FACE_COLOR.green * shade).coerceIn(0f, 1f),
    blue = (BASE_FACE_COLOR.blue * shade).coerceIn(0f, 1f),
    alpha = 1f,
)

private fun dot(a: FloatArray, b: FloatArray): Float =
    a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

private fun normalize(x: Float, y: Float, z: Float): FloatArray {
    val length = sqrt(x * x + y * y + z * z)
    return floatArrayOf(x / length, y / length, z / length)
}
