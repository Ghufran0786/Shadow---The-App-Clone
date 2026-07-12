package org.waxmoon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.waxmoon.ui.theme.ShadowTheme

class ShadowCloneActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShadowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background,
                ) {
                    CloneSmartQScreen()
                }
            }
        }
    }
}

private sealed class CloneUiState {
    object Idle : CloneUiState()
    object Loading : CloneUiState()
    data class Done(val message: String, val isError: Boolean) : CloneUiState()
}

@Composable
private fun CloneSmartQScreen() {
    var uiState by remember { mutableStateOf<CloneUiState>(CloneUiState.Idle) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Shadow — Phase 1",
            style = MaterialTheme.typography.h5,
            color = MaterialTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Validate split-APK clone + launch",
            style = MaterialTheme.typography.body2,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))

        when (uiState) {
            is CloneUiState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Installing…", color = MaterialTheme.colors.onBackground)
            }
            is CloneUiState.Done -> {
                val done = uiState as CloneUiState.Done
                Text(
                    text = done.message,
                    color = if (done.isError) Color(0xFFB00020) else MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { uiState = CloneUiState.Idle }) {
                    Text("Try again")
                }
            }
            is CloneUiState.Idle -> {
                Button(
                    onClick = {
                        uiState = CloneUiState.Loading
                        scope.launch {
                            val result = try {
                                withTimeout(60_000L) {
                                    withContext(Dispatchers.IO) {
                                        SmartQCloner.cloneAndLaunch(MoonApplication.INSTANCE())
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e(SmartQCloner.TAG, "Clone timed out or failed", e)
                                SmartQCloner.CloneResult(
                                    success = false,
                                    message = when (e) {
                                        is TimeoutCancellationException ->
                                            "Timed out after 60 seconds"
                                        else -> e.message ?: e.javaClass.simpleName
                                    },
                                )
                            }
                            uiState = CloneUiState.Done(
                                message = result.message,
                                isError = !result.success,
                            )
                        }
                    },
                ) {
                    Text("Clone SmartQ")
                }
            }
        }
    }
}
