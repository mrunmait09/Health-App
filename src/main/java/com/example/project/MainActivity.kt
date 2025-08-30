package com.example.project

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.project.ui.theme.ProjectTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        // Google Sign-In config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.defult_webid))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Launcher to open Google account chooser and receive result
        val signInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: Exception) {
                    Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

        setContent {
            ProjectTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        onGoogleSignIn = {
                            val intent = googleSignInClient.signInIntent
                            signInLauncher.launch(intent) // 👉 opens Google sign-in
                        },
                        auth = firebaseAuth
                    )
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    Toast.makeText(this, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

// ---------- UI (Compose) ----------

@Composable
fun MainScreen(
    onGoogleSignIn: () -> Unit,
    auth: FirebaseAuth
) {
    var steps by remember { mutableStateOf(0) }
    var advice by remember { mutableStateOf("") }
    var saveStatus by remember { mutableStateOf("") }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onGoogleSignIn) { Text("Sign in with Google") }

        Spacer(Modifier.height(12.dp))

        Button(onClick = {
            // तुमचा steps fetcher (किंवा Google Fit नंतर)
            steps = (1000..5000).random()
        }) { Text("Fetch Steps") }

        Text("Steps: $steps")

        Spacer(Modifier.height(12.dp))

        Button(onClick = {
            // TODO: तुमचा Gemini API key इथे द्या
            advice = if (steps > 0) "Walked $steps. Keep hydrated and stretch!" else "Fetch steps first."
        }) { Text("Get Gemini Advice") }

        Text(advice)

        Spacer(Modifier.height(12.dp))

        Button(onClick = {
            val user = auth.currentUser
            if (user != null) {
                val log = mapOf(
                    "steps" to steps,
                    "advice" to advice,
                    "date" to todayId()
                )
                db.collection("users")
                    .document(user.uid)
                    .collection("health_logs")
                    .document(todayId())
                    .set(log)
                    .addOnSuccessListener { saveStatus = "✅ Saved to Firestore" }
                    .addOnFailureListener { e -> saveStatus = "❌ Failed: ${e.message}" }
            } else {
                saveStatus = "⚠ Please sign in first"
            }
        }) { Text("Save to Firestore") }

        Text(saveStatus)
    }
}

// Helper
fun todayId(): String {
    val cal = java.util.Calendar.getInstance()
    val y = cal.get(java.util.Calendar.YEAR)
    val m = cal.get(java.util.Calendar.MONTH) + 1
    val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
    return "%04d-%02d-%02d".format(y, m, d)
}