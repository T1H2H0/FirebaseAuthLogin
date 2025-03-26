package com.uwonham.firebaselogin

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Login : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            val showSignInDialog = remember { mutableStateOf(true) }
//            Button(
//                onClick = { showSignInDialog. value = !showSignInDialog. value }) {
//                Text(text = "Login")
//            }
//        }  if (showSignInDialog. value) {
//            FirebaseSignInDialog(
//                auth = auth,
//                image = imageBitmap,
//                onDismiss = { showSignInDialog. value = false },
//                onSignInSuccess = { user ->
//                Log. d(TAG, "Login successful: $user")
//                viewModel. userLoggedIn(user)         }     )
        }
        }

}