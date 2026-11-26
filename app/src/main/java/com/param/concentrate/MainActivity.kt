package com.param.concentrate

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ConcentrateScreen()
            }
        }
    }
}

private data class InternetApp(val label: String, val packageName: String)

@Composable
private fun ConcentrateScreen() {
    val context = LocalContext.current
    val running by FocusVpnService.running.collectAsState()
    val apps = remember { loadInternetApps(context) }
    var keepOnline by remember { mutableStateOf(setOf<String>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startFocus(context, keepOnline)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(text = "Concentrate", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (running) {
                    "Focus is on. Apps you didn't keep online are blocked."
                } else {
                    "Focus is off. Pick the apps that should stay online."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (running) {
                OutlinedButton(
                    onClick = { stopFocus(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop focus")
                }
            } else {
                Button(
                    onClick = {
                        val consent = VpnService.prepare(context)
                        if (consent == null) {
                            startFocus(context, keepOnline)
                        } else {
                            permissionLauncher.launch(consent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start focus")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Keep online", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName }) { app ->
                    val checked = app.packageName in keepOnline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !running) {
                                keepOnline = if (checked) {
                                    keepOnline - app.packageName
                                } else {
                                    keepOnline + app.packageName
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                            enabled = !running
                        )
                        Text(text = app.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

private fun startFocus(context: Context, keepOnline: Set<String>) {
    val intent = Intent(context, FocusVpnService::class.java)
        .setAction(FocusVpnService.ACTION_START)
        .putStringArrayListExtra(FocusVpnService.EXTRA_KEEP_ONLINE, ArrayList(keepOnline))
    context.startService(intent)
}

private fun stopFocus(context: Context) {
    val intent = Intent(context, FocusVpnService::class.java)
        .setAction(FocusVpnService.ACTION_STOP)
    context.startService(intent)
}

private fun loadInternetApps(context: Context): List<InternetApp> {
    val pm = context.packageManager
    return pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        .asSequence()
        .filter { it.packageName != context.packageName }
        .filter { it.requestedPermissions?.contains(Manifest.permission.INTERNET) == true }
        .mapNotNull { info ->
            val applicationInfo = info.applicationInfo ?: return@mapNotNull null
            InternetApp(pm.getApplicationLabel(applicationInfo).toString(), info.packageName)
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}
