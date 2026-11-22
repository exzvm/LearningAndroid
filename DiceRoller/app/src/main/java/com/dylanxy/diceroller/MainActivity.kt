package com.dylanxy.diceroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dylanxy.diceroller.ui.theme.DiceRollerTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.floor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiceRollerTheme {
                DiceRollerApp()
            }
        }
    }
}

/**
 * 每个点数对应的“停稳姿态”：
 * 先绕 X 轴、再绕 Y 轴旋转，使该点数的面朝向观察者。
 * 绕 Z 轴的角度只决定面内朝向，不影响朝前的是哪一面。
 */
private val FACE_ORIENTATIONS = mapOf(
    1 to (0f to 0f),
    2 to (270f to 0f),
    3 to (0f to 270f),
    4 to (0f to 90f),
    5 to (90f to 0f),
    6 to (180f to 0f),
)

@Composable
fun DiceWithButtonAndImage(modifier: Modifier = Modifier) {
    var result by remember { mutableStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }

    // 三个轴各自独立旋转，组合起来就是翻跟头式的翻滚
    val rotationX = remember { Animatable(0f) }
    val rotationY = remember { Animatable(0f) }
    val rotationZ = remember { Animatable(0f) }
    // 0..1：控制腾空高度与透视缩放
    val toss = remember { Animatable(0f) }
    // 0..1：落地后的余弹
    val bounce = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Dice3D(
            rotationX = rotationX.value,
            rotationY = rotationY.value,
            rotationZ = rotationZ.value,
            tossProgress = toss.value,
            bounceProgress = bounce.value,
            description = if (isRolling) "骰子滚动中" else "骰子：$result 点",
            modifier = Modifier.size(320.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            enabled = !isRolling,
            onClick = {
                if (!isRolling) {
                    isRolling = true
                    scope.launch {
                        // 先决定结果，再算出该点数朝前所需的最终姿态
                        val value = (1..6).random()
                        val (baseX, baseY) = FACE_ORIENTATIONS.getValue(value)
                        val targetX = rotationX.value.spinTo(baseX, fullTurns = 2)
                        val targetY = rotationY.value.spinTo(baseY, fullTurns = 3)
                        val targetZ = rotationZ.value + 360f + 90f * (0..3).random()

                        coroutineScope {
                            launch {
                                rotationX.animateTo(
                                    targetValue = targetX,
                                    animationSpec = tween(1100, easing = FastOutSlowInEasing),
                                )
                            }
                            launch {
                                rotationY.animateTo(
                                    targetValue = targetY,
                                    animationSpec = tween(1100, easing = FastOutSlowInEasing),
                                )
                            }
                            launch {
                                rotationZ.animateTo(
                                    targetValue = targetZ,
                                    animationSpec = tween(1100, easing = FastOutSlowInEasing),
                                )
                            }
                            launch {
                                toss.snapTo(0f)
                                toss.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(1100, easing = LinearEasing),
                                )
                            }
                        }

                        // 归一化，避免角度无限增大；视觉上等价
                        rotationX.snapTo(rotationX.value % 360f)
                        rotationY.snapTo(rotationY.value % 360f)
                        rotationZ.snapTo(rotationZ.value % 360f)
                        toss.snapTo(0f)
                        result = value

                        // 落地后的余弹
                        bounce.snapTo(0f)
                        bounce.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(320, easing = LinearEasing),
                        )
                        bounce.snapTo(0f)

                        isRolling = false
                    }
                }
            }
        ) {
            Text(stringResource(R.string.roll))
        }
    }
}

/**
 * 在至少 [fullTurns] 整圈的基础上，停到 [base] 角度。
 * 这样无论当前角度是多少，都会朝正方向转足够多圈再停稳。
 */
private fun Float.spinTo(base: Float, fullTurns: Int): Float {
    val completedTurns = floor(this / 360f) * 360f
    return completedTurns + base + 360f * fullTurns
}

@Preview
@Composable
fun DiceRollerApp() {
    DiceWithButtonAndImage(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    )
}
