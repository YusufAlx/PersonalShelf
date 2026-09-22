package za.ac.personalshelf

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    suspend fun signIn(email: String, password: String) = auth.signInWithEmailAndPassword(email, password).await()
    suspend fun register(email: String, password: String) = auth.createUserWithEmailAndPassword(email, password).await()
    suspend fun resetPassword(email: String) = auth.sendPasswordResetEmail(email).await()
    suspend fun signInWithGoogle(idToken: String) = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    fun signOut() = auth.signOut()
}
