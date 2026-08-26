package com.rudra.voidlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoidLiquidLauncher()
        }
    }
}

@Composable
fun VoidLiquidLauncher() {
    val context = LocalContext.current
    val pm = context.packageManager

    var isDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var roll by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }

    // জাইরোস্কোপ ও লাইভ সেন্সর লিসেনার (১২০ FPS অপটিমাইজড)
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    roll = (orientation[2] * 35f).coerceIn(-40f, 40f)
                    pitch = (orientation[1] * 20f).coerceIn(-25f, 25f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val animatedRoll by animateFloatAsState(targetValue = roll, animationSpec = tween(60), label = "roll")
    val animatedPitch by animateFloatAsState(targetValue = pitch, animationSpec = tween(60), label = "pitch")

    // ফোনের সব ইনস্টল করা অ্যাপ ফেচ করা
    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(intent, 0).sortedBy { it.loadLabel(pm).toString().lowercase() }
    }

    val filteredApps = apps.filter {
        it.loadLabel(pm).toString().contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -25) isDrawerOpen = true
                    if (dragAmount > 25) isDrawerOpen = false
                }
            }
    ) {
        // --- ১. মিনিমাল হোম স্ক্রিন উইজেট ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VOID GLASS",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 24.sp,
                letterSpacing = 4.sp
            )
            Text(
                text = "Swipe up for apps",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // --- ২. রিয়েল লিকুইড গ্লাস ডক (জাইরোস্কোপ রিফ্লেকশন সহ) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp, vertical = 28.dp)
                .fillMaxWidth()
                .height(86.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(38.dp),
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(38.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(38.dp)
                )
        ) {
            // ডকের ওপর সেন্সর অনুযায়ী আলোর নড়াচড়া
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = animatedRoll.dp, y = animatedPitch.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            radius = 220f
                        )
                    )
            )

            // ফেভারিট ৪টি ডক অ্যাপ
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                apps.take(4).forEach { app ->
                    AppIconTile(app = app, pm = pm, showLabel = false) {
                        launchApp(context, pm, app)
                    }
                }
            }
        }

        // --- ৩. ফ্রস্টেড অ্যাপ ড্রয়ার ---
        if (isDrawerOpen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color.White.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp)
                ) {
                    items(filteredApps) { app ->
                        AppIconTile(app = app, pm = pm, showLabel = true) {
                            launchApp(context, pm, app)
                            isDrawerOpen = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconTile(app: ResolveInfo, pm: PackageManager, showLabel: Boolean, onClick: () -> Unit) {
    val iconBitmap = remember(app) {
        app.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
    }

    Column(
        modifier = Modifier
            .padding(6.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = iconBitmap,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        if (showLabel) {
            Text(
                text = app.loadLabel(pm).toString(),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

fun launchApp(context: Context, pm: PackageManager, app: ResolveInfo) {
    val intent = pm.getLaunchIntentForPackage(app.activityInfo.packageName)
    intent?.let { context.startActivity(it) }
}
