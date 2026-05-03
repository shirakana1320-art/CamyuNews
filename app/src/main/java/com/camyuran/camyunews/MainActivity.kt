package com.camyuran.camyunews

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.camyuran.camyunews.presentation.AppNavigation
import com.camyuran.camyunews.presentation.shared.CamyuNewsTheme
import com.camyuran.camyunews.worker.NewsFetchWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var workManager: WorkManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 許可・拒否に関わらず通常起動 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        triggerStartupFetch()

        setContent {
            CamyuNewsTheme {
                AppNavigation()
            }
        }
    }

    private fun triggerStartupFetch() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<NewsFetchWorker>()
            .setConstraints(constraints)
            .addTag(NewsFetchWorker.TAG_FETCH)
            .build()
        workManager.enqueueUniqueWork("StartupFetch", ExistingWorkPolicy.KEEP, request)
    }
}
