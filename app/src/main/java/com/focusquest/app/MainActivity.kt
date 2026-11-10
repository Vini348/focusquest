package com.focusquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusquest.app.ui.FocusQuestApp
import com.focusquest.app.ui.FocusQuestTheme
import com.focusquest.app.viewmodel.FocusViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusQuestTheme {
                val vm: FocusViewModel = viewModel()
                FocusQuestApp(vm)
            }
        }
    }
}
