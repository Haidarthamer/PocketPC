package dev.pocketpc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pocketpc.system.capability.Availability
import dev.pocketpc.system.capability.CapabilityEvaluator
import dev.pocketpc.system.capability.CapabilityReport
import dev.pocketpc.system.capability.Feature
import dev.pocketpc.system.capability.HardwareSnapshot
import dev.pocketpc.system.capability.HardwareSource
import kotlin.concurrent.thread

/**
 * Phase 1 shell.
 *
 * Deliberately not yet the desktop: Phase 1's job is to prove the app runs on hardware
 * and that device detection is honest. The desktop/window manager arrives in Phase 2.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val source = HardwareSourceFactory.create(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DiagnosticsScreen(source)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsScreen(source: HardwareSource) {
    var report by remember { mutableStateOf<CapabilityReport?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var probing by remember { mutableStateOf(false) }

    fun runProbe() {
        probing = true
        failure = null
        thread(name = "capability-probe") {
            try {
                val snapshot = source.snapshot()
                val evaluated = CapabilityEvaluator.evaluate(snapshot)
                runOnUiThreadOnMain { report = evaluated; probing = false }
            } catch (t: Throwable) {
                val fallback = CapabilityEvaluator.evaluate(HardwareSnapshot.unprobed("Probe failed: ${t.message}"))
                runOnUiThreadOnMain { report = fallback; failure = t.toString(); probing = false }
            }
        }
    }

    if (report == null && !probing) runProbe()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pocket PC", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Phase 1 — hardware capability report",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        if (probing && report == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                Spacer(Modifier.height(8.dp))
                Text("  Probing device…")
            }
            return@Column
        }

        val r = report ?: return@Column
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { DeviceCard(r.snapshot) }
            item { ProfileCard(r) }
            items(Feature.entries.toList()) { feature ->
                FeatureCard(feature, r.features[feature] ?: Availability.Unknown("not evaluated"))
            }
            item {
                Button(onClick = { probing = false; report = null; runProbe() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Re-run probe")
                }
            }
            failure?.let { msg ->
                item {
                    Text(
                        "Probe error: $msg",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                    )
                }
            }
            if (r.snapshot.probeWarnings.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Probe warnings", fontWeight = FontWeight.Medium)
                            r.snapshot.probeWarnings.forEach { Text("• $it", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(s: HardwareSnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Device", fontWeight = FontWeight.SemiBold)
            mono("model", "${s.deviceManufacturer} ${s.deviceModel}")
            mono("android", "API ${s.sdkInt}")
            mono("abi", s.abi ?: "unknown")
            mono("cpu", "${s.cpuCores} cores @ ${s.cpuMaxFreqKHz / 1000} MHz")
            mono("ram", "${s.ramTotalMb} MiB")
            mono("storage free", "${s.storageFreeMb} MiB")
            mono("gpu", s.gpuRenderer ?: "not detectable")
            mono("gl es", s.glEsVersion ?: "unknown")
            mono("vulkan", s.vulkanApiVersion ?: (s.vulkanSupported?.toString() ?: "unprobed"))
            mono("avf", s.avfDeclared?.toString() ?: "unprobed")
            mono("kvm node", s.kvmPresent?.toString() ?: "unprobed")
            mono("thermal", s.thermalStatus?.toString() ?: "unknown")
        }
    }
}

@Composable
private fun ProfileCard(r: CapabilityReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Performance profile: ${r.profile.id.name}", fontWeight = FontWeight.SemiBold)
            r.profile.reasons.forEach { Text("• $it", fontSize = 12.sp) }
            mono("max guest ram", "${r.profile.maxGuestRamMb} MiB")
            mono("guests allowed", r.profile.allowGuests.toString())
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature, availability: Availability) {
    val (label, detail) = when (availability) {
        is Availability.Supported -> "SUPPORTED" to availability.basis
        is Availability.Degraded -> "LIMITED" to "${availability.basis} — ${availability.limitation}"
        is Availability.Unsupported -> "UNSUPPORTED" to availability.reason
        is Availability.Unknown -> "UNKNOWN" to availability.reason
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(feature.name.replace('_', ' '), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val notes = when (availability) {
                is Availability.Supported -> availability.notes
                is Availability.Degraded -> emptyList()
                else -> emptyList()
            }
            notes.forEach { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                "verified: ${availability.isVerified}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun mono(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
