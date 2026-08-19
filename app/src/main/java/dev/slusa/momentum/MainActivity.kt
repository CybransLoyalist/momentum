package dev.slusa.momentum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.slusa.momentum.ui.MomentumShell
import dev.slusa.momentum.ui.MomentumViewModel
import dev.slusa.momentum.ui.theme.MomentumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repository = (application as MomentumApp).repository

        setContent {
            MomentumTheme {
                val vm: MomentumViewModel = viewModel(factory = MomentumViewModel.factory(repository))
                MomentumShell(vm)
            }
        }
    }
}
