package com.arkus.shoppyjuan.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.arkus.shoppyjuan.presentation.components.BottomNavigationBar
import com.arkus.shoppyjuan.presentation.navigation.NavGraph
import com.arkus.shoppyjuan.presentation.theme.ShoppyJuanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoppyJuanTheme {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigationBar(navController = navController)
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        startDestination = com.arkus.shoppyjuan.presentation.navigation.Screen.Home.route
                    )
                }
            }
        }
    }
}
