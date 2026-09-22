package za.ac.personalshelf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("shelf_cloud_sync", ExistingPeriodicWorkPolicy.KEEP, request)
        enableEdgeToEdge()
        setContent { PersonalShelfApp() }
    }
}

@Composable
private fun PersonalShelfApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { ShelfRepository(context) }
    val scope = rememberCoroutineScope()
    // A local demo mode remains usable before Firebase is connected in Android Studio.
    var signedIn by remember { mutableStateOf(runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false)) }
    var afrikaans by remember { mutableStateOf(false) }
    val allItems by repo.items.collectAsStateWithLifecycle(emptyList())
    val googleSignIn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch { runCatching { GoogleSignIn.getSignedInAccountFromIntent(result.data).result.idToken?.let { AuthRepository().signInWithGoogle(it) } ?: error("No Google ID token") }.onSuccess { signedIn = true } }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF255F38))) {
        Surface(Modifier.fillMaxSize()) {
            if (!signedIn) AuthScreen(onSuccess = { signedIn = true }, onGoogle = {
                val webClientId = context.getString(R.string.default_web_client_id)
                if (webClientId.isNotBlank()) {
                    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(webClientId).requestEmail().build()
                    googleSignIn.launch(GoogleSignIn.getClient(context, options).signInIntent)
                }
            })
            else ShelfScreen(allItems, afrikaans, onLanguage = { afrikaans = it }, onSignOut = {
                runCatching { AuthRepository().signOut() }; signedIn = false
            }, onSave = { scope.launch { repo.save(it) } }, onSync = {
                scope.launch { repo.syncToCloud(allItems) }
            })
        }
    }
}

@Composable
private fun AuthScreen(onSuccess: () -> Unit, onGoogle: () -> Unit) {
    val scope = rememberCoroutineScope(); var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var register by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(28.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("Personal Shelf", style = MaterialTheme.typography.displaySmall)
        Text(if (register) "Create your secure library account" else "Your books, comics and documents in one place")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (email.isBlank() || password.length < 6) { message = "Enter an email and a password of at least 6 characters."; return@Button }
            scope.launch {
                runCatching { if (register) AuthRepository().register(email, password) else AuthRepository().signIn(email, password) }
                    .onSuccess { onSuccess() }.onFailure { message = it.message ?: "Sign-in failed. Check Firebase setup and your connection." }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (register) "Register" else "Log in") }
        TextButton({ register = !register }) { Text(if (register) "Already registered? Log in" else "New here? Register") }
        TextButton({ scope.launch { runCatching { AuthRepository().resetPassword(email) }.onSuccess { message = "Password-reset email sent." }.onFailure { message = "Enter your registered email first." } } }) { Text("Forgot password?") }
        OutlinedButton(onClick = onSuccess, modifier = Modifier.fillMaxWidth()) { Text("Explore offline demo") }
        HorizontalDivider(); Text("Google single sign-on", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
        OutlinedButton(onClick = onGoogle, modifier = Modifier.fillMaxWidth()) { Text("Continue with Google") }
        Text("Enable the Google provider in Firebase Authentication and add your Web client ID to use Google sign-in.", style = MaterialTheme.typography.bodySmall)
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelfScreen(initialItems: List<ShelfItem>, afrikaans: Boolean, onLanguage: (Boolean) -> Unit, onSignOut: () -> Unit, onSave: (List<ShelfItem>) -> Unit, onSync: () -> Unit) {
    var query by remember { mutableStateOf("") }; var category by remember { mutableStateOf<Category?>(null) }
    var favouritesOnly by remember { mutableStateOf(false) }; var showAdd by remember { mutableStateOf(false) }; var settings by remember { mutableStateOf(false) }
    val visible = initialItems.filter { (category == null || it.category == category) && (!favouritesOnly || it.favourite) && (it.title.contains(query, true) || it.author.contains(query, true)) }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) showAdd = true }
    Scaffold(topBar = { TopAppBar(title = { Text(if (afrikaans) "Persoonlike Rak" else "Personal Shelf") }, actions = {
        IconButton(onClick = onSync) { Icon(Icons.Default.Sync, "Sync cloud changes") }
        IconButton(onClick = { settings = true }) { Icon(Icons.Default.Settings, "Settings") }
    }) }, floatingActionButton = { ExtendedFloatingActionButton(onClick = { pickFile.launch(arrayOf("application/pdf", "application/epub+zip", "text/plain")) }, icon = { Icon(Icons.Default.UploadFile, null) }, text = { Text(if (afrikaans) "Laai op" else "Upload") }) }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(if (afrikaans) "Soek titel of outeur" else "Search title or author") }, leadingIcon = { Icon(Icons.Default.Search, null) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
                Spacer(Modifier.width(6.dp)); FilterChip(selected = favouritesOnly, onClick = { favouritesOnly = !favouritesOnly }, label = { Text("★") })
                Category.entries.forEach { c -> Spacer(Modifier.width(4.dp)); AssistChip(onClick = { category = if (category == c) null else c }, label = { Text(c.name) }) }
            }
            if (visible.isEmpty()) EmptyShelf(afrikaans) else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.id }) { item -> ItemCard(item, onUpdate = { changed -> onSave(initialItems.map { if (it.id == changed.id) changed else it }) }, onDelete = { onSave(initialItems.filterNot { it.id == item.id }) }) }
            }
        }
    }
    if (showAdd) AddItemDialog(onDismiss = { showAdd = false }, onAdd = { item -> onSave(initialItems + item); showAdd = false })
    if (settings) SettingsDialog(afrikaans, { onLanguage(it) }, onSignOut, { settings = false })
}

@Composable private fun EmptyShelf(afrikaans: Boolean) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (afrikaans) "Jou rak is leeg. Laai jou eerste leesstuk op." else "Your shelf is empty. Upload your first reading item.") }

@Composable
private fun ItemCard(item: ShelfItem, onUpdate: (ShelfItem) -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text("${item.author} • ${item.category}") }; IconButton({ onUpdate(item.copy(favourite = !item.favourite, pendingSync = true)) }) { Icon(if (item.favourite) Icons.Default.Star else Icons.Default.StarBorder, "Favourite") }; IconButton(onDelete) { Icon(Icons.Default.Delete, "Delete") } }
        LinearProgressIndicator(progress = { item.progress / 100f }, Modifier.fillMaxWidth().padding(top = 8.dp)); Text("Reading progress: ${item.progress}%")
        Row { AssistChip(onClick = { onUpdate(item.copy(progress = (item.progress + 10).coerceAtMost(100), pendingSync = true)) }, label = { Text("Read +10%") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = item.offlineReady, onClick = { onUpdate(item.copy(offlineReady = !item.offlineReady)) }, label = { Text(if (item.offlineReady) "Available offline" else "Make offline") }); if (item.pendingSync) Text("Pending sync", Modifier.padding(8.dp), color = MaterialTheme.colorScheme.tertiary) }
    } }
}

@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onAdd: (ShelfItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.Books) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to shelf") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Title") })
                OutlinedTextField(author, { author = it }, label = { Text("Author / creator") })
                OutlinedTextField(description, { description = it }, label = { Text("Description") })
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text("Category: ${category.name}") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        Category.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = { category = option; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) onAdd(ShelfItem(title = title, author = author.ifBlank { "Unknown" }, category = category, description = description))
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SettingsDialog(afrikaans: Boolean, onLanguage: (Boolean) -> Unit, onSignOut: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Text("Language / Taal")
                Row {
                    FilterChip(selected = !afrikaans, onClick = { onLanguage(false) }, label = { Text("English") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = afrikaans, onClick = { onLanguage(true) }, label = { Text("Afrikaans") })
                }
                TextButton(onClick = onSignOut) { Text("Log out") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}


