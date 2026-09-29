package com.example.attendanceapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.attendanceapp.AppContainer
import com.example.attendanceapp.data.repository.Role
import com.example.attendanceapp.ui.admin.AddStaffScreen
import com.example.attendanceapp.ui.admin.AddStaffViewModel
import com.example.attendanceapp.ui.admin.EnrolFaceScreen
import com.example.attendanceapp.ui.admin.EnrolFaceViewModel
import com.example.attendanceapp.ui.admin.StaffListScreen
import com.example.attendanceapp.ui.admin.StaffListViewModel
import com.example.attendanceapp.ui.admin.StaffProfileScreen
import com.example.attendanceapp.ui.admin.StaffProfileViewModel
import com.example.attendanceapp.ui.auth.LoginScreen
import com.example.attendanceapp.ui.auth.LoginViewModel
import com.example.attendanceapp.ui.common.ViewModelFactory
import com.example.attendanceapp.ui.staff.MarkAttendanceScreen
import com.example.attendanceapp.ui.staff.MarkAttendanceViewModel
import com.example.attendanceapp.ui.staff.StaffHomeScreen
import com.example.attendanceapp.ui.staff.StaffHomeViewModel

@Composable
fun AppNavGraph(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = viewModel(
                factory = ViewModelFactory {
                    LoginViewModel(container.authRepository)
                }
            )
            LoginScreen(
                viewModel = viewModel,
                onLoggedIn = { session ->
                    if (session.role == Role.ADMIN) {
                        navController.navigate(Routes.STAFF_LIST) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.staffHome(session.staffId!!, session.displayName)) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.STAFF_LIST) {
            val viewModel: StaffListViewModel = viewModel(
                factory = ViewModelFactory { StaffListViewModel(container.staffRepository) }
            )
            StaffListScreen(
                viewModel = viewModel,
                onAddStaff = { navController.navigate(Routes.ADD_STAFF) },
                onOpenStaff = { staffId -> navController.navigate(Routes.staffProfile(staffId)) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ADD_STAFF) {
            val viewModel: AddStaffViewModel = viewModel(
                factory = ViewModelFactory { AddStaffViewModel(container.staffRepository) }
            )
            AddStaffScreen(
                viewModel = viewModel,
                onStaffAdded = { staffId ->
                    navController.navigate(Routes.staffProfile(staffId)) {
                        popUpTo(Routes.STAFF_LIST)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.STAFF_PROFILE,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments!!.getLong("staffId")
            val viewModel: StaffProfileViewModel = viewModel(
                factory = ViewModelFactory {
                    StaffProfileViewModel(staffId, container.staffRepository, container.attendanceRepository)
                }
            )
            StaffProfileScreen(
                viewModel = viewModel,
                onEnrolFace = { navController.navigate(Routes.enrolFace(staffId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ENROL_FACE,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments!!.getLong("staffId")
            val viewModel: EnrolFaceViewModel = viewModel(
                factory = ViewModelFactory { EnrolFaceViewModel(staffId, container.staffRepository) }
            )
            EnrolFaceScreen(
                viewModel = viewModel,
                onDone = { navController.popBackStack(Routes.STAFF_LIST, inclusive = false) },
                onCancel = { navController.popBackStack(Routes.STAFF_LIST, inclusive = false) }
            )
        }

        composable(
            Routes.STAFF_HOME,
            arguments = listOf(
                navArgument("staffId") { type = NavType.LongType },
                navArgument("displayName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments!!.getLong("staffId")
            val displayName = Routes.decode(backStackEntry.arguments!!.getString("displayName") ?: "")
            val viewModel: StaffHomeViewModel = viewModel(
                factory = ViewModelFactory { StaffHomeViewModel(staffId, container.attendanceRepository) }
            )
            StaffHomeScreen(
                displayName = displayName,
                viewModel = viewModel,
                onMarkAttendance = { navController.navigate(Routes.markAttendance(staffId)) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.MARK_ATTENDANCE,
            arguments = listOf(navArgument("staffId") { type = NavType.LongType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments!!.getLong("staffId")
            val viewModel: MarkAttendanceViewModel = viewModel(
                factory = ViewModelFactory { MarkAttendanceViewModel(staffId, container.attendanceRepository) }
            )
            MarkAttendanceScreen(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() }
            )
        }
    }
}
