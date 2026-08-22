package dev.ilamparithi.aournalpp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.ilamparithi.aournalpp.ui.LicensesScreen
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme

class LicensesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AournalTheme {
                LicensesScreen(onBack = { finish() })
            }
        }
    }
}
