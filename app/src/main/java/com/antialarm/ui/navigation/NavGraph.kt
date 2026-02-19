package com.antialarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antialarm.ui.editor.AlarmEditorScreen
import com.antialarm.ui.list.AlarmListScreen

object Routes {
    const val ALARM_LIST = "alarm_list"
    const val ALARM_EDITOR = "alarm_editor?alarmId={alarmId}"

    fun alarmEditor(alarmId: Int = -1): String {
        return "alarm_editor?alarmId=$alarmId"
    }
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ALARM_LIST
    ) {
        composable(Routes.ALARM_LIST) {
            AlarmListScreen(
                onAddAlarm = {
                    navController.navigate(Routes.alarmEditor())
                },
                onEditAlarm = { alarmId ->
                    navController.navigate(Routes.alarmEditor(alarmId))
                }
            )
        }

        composable(
            route = Routes.ALARM_EDITOR,
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            AlarmEditorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
