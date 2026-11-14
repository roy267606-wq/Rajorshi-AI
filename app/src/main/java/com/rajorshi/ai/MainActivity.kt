package com.rajorshi.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val Ink = Color(0xFF18213D)
private val Purple = Color(0xFF6655D9)
private val Violet = Color(0xFF9279F5)
private val Mint = Color(0xFF31BFA7)
private val Canvas = Color(0xFFF6F5FB)
private val Muted = Color(0xFF7C8298)

data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val imageUri: Uri? = null
)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale("bn", "BD")
        }
        setContent { RajorshiRoot(::speak) }
    }
    private fun speak(text: String) {
        val locale = if (text.any { it.code in 0x0980..0x09FF }) Locale("bn", "BD") else Locale.US
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rajorshi_speech")
    }
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun RajorshiRoot(onSpeak: (String) -> Unit) {
    var page by remember { mutableStateOf("home") }
    var bangla by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            ChatMessage("স্বাগতম! আমি Rajorshi — তোমার শেখার সঙ্গী। অঙ্ক, বিজ্ঞান, ইংরেজি বা প্রজেক্ট নিয়ে প্রশ্ন করো।")
        )
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingImage = uri
        if (uri != null) {
            page = "chat"
            if (draft.isBlank()) draft = if (bangla) "ছবির প্রশ্নটি ধাপে ধাপে সমাধান করো।" else "Please solve the question in this picture step by step."
        }
    }
    val voicePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) draft = spoken
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple, secondary = Mint, background = Canvas,
            surface = Color.White, onSurface = Ink
        )
    ) {
        Scaffold(
            containerColor = Canvas,
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 10.dp) {
                    NavigationBarItem(
                        selected = page == "home", onClick = { page = "home" },
                        icon = { Icon(Icons.Default.Home, null) }, label = { Text(if (bangla) "হোম" else "Home") }
                    )
                    NavigationBarItem(
                        selected = page == "chat", onClick = { page = "chat" },
                        icon = { Icon(Icons.Default.AutoAwesome, null) }, label = { Text(if (bangla) "চ্যাট" else "Chat") }
                    )
                    NavigationBarItem(
                        selected = page == "learn", onClick = { page = "learn" },
                        icon = { Icon(Icons.Default.School, null) }, label = { Text(if (bangla) "শিখি" else "Learn") }
                    )
                    NavigationBarItem(
                        selected = page == "profile", onClick = { page = "profile" },
                        icon = { Icon(Icons.Default.Person, null) }, label = { Text(if (bangla) "প্রোফাইল" else "Profile") }
                    )
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Header(bangla, onLanguage = { bangla = !bangla })
                when (page) {
                    "home" -> HomeScreen(
                        bangla = bangla,
                        onAsk = { prompt -> draft = prompt; page = "chat" },
                        onOpenChat = { page = "chat" }
                    )
                    "chat" -> ChatScreen(
                        bangla = bangla,
                        messages = messages,
                        draft = draft,
                        onDraft = { draft = it },
                        imageUri = pendingImage,
                        onRemoveImage = { pendingImage = null },
                        busy = busy,
                        error = error,
                        onVoice = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (bangla) "bn-BD" else "en-US")
                            }
                            try { voicePicker.launch(intent) } catch (_: Exception) {
                                error = if (bangla) "ভয়েস ইনপুট চালু করা যায়নি।" else "Voice input is unavailable."
                            }
                        },
                        onImage = { imagePicker.launch("image/*") },
                        onSend = {
                            val prompt = draft.trim()
                            val image = pendingImage
                            if (prompt.isNotBlank() || image != null) {
                                messages.add(ChatMessage(prompt.ifBlank { if (bangla) "এই ছবিটি বুঝিয়ে দাও।" else "Explain this image." }, true, image))
                                draft = ""
                                pendingImage = null
                                busy = true
                                error = ""
                                scope.launch {
                                    val imageData = if (image != null) encodeImage(context, image) else null
                                    val reply = askRajorshi(
                                        prompt.ifBlank { "Explain and solve the question in this image step by step." },
                                        if (bangla) "Bangla" else "English",
                                        imageData
                                    )
                                    messages.add(ChatMessage(reply, false))
                                    busy = false
                                }
                            }
                        },
                        onSpeak = onSpeak
                    )
                    "learn" -> LearnScreen(bangla) { prompt -> draft = prompt; page = "chat" }
                    else -> ProfileScreen(bangla, onLanguage = { bangla = !bangla })
                }
            }
        }
    }
}

@Composable
private fun Header(bangla: Boolean, onLanguage: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(43.dp).clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(Purple, Violet))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Rajorshi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(if (bangla) "তোমার AI শেখার সঙ্গী" else "Your AI learning companion",
                style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        TextButton(onClick = onLanguage) {
            Text(if (bangla) "EN" else "বাংলা", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeScreen(bangla: Boolean, onAsk: (String) -> Unit, onOpenChat: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF5948C8), Color(0xFF9B78F4))))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (bangla) "আজ কী শিখবে?" else "What will you learn today?",
                        color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (bangla) "প্রশ্ন করো, বুঝে শেখো, আত্মবিশ্বাস বাড়াও।" else "Ask questions, understand more, grow confident.",
                        color = Color.White.copy(alpha = .9f))
                    Button(onClick = onOpenChat, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Purple)) {
                        Text(if (bangla) "Rajorshi-কে জিজ্ঞেস করো" else "Ask Rajorshi")
                        Spacer(Modifier.width(6.dp)); Icon(Icons.Default.ArrowForward, null)
                    }
                }
            }
        }
        item { Text(if (bangla) "তোমার পড়ার টুলস" else "Study tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                ToolCard(Icons.Default.Calculate, if (bangla) "অঙ্ক সমাধান" else "Math solver",
                    if (bangla) "ধাপে ধাপে অঙ্ক বুঝে নাও" else "Understand every step", Color(0xFFECE9FF)) {
                    onAsk(if (bangla) "এই অঙ্কটি ধাপে ধাপে সমাধান করো: " else "Solve this math problem step by step: ")
                }
                ToolCard(Icons.Default.Science, if (bangla) "বিজ্ঞান শেখা" else "Science tutor",
                    if (bangla) "সহজ ভাষায় বিজ্ঞান বুঝি" else "Science made simple", Color(0xFFE1F7F1)) {
                    onAsk(if (bangla) "এই বিজ্ঞান বিষয়টি সহজভাবে বুঝিয়ে দাও: " else "Explain this science topic simply: ")
                }
                ToolCard(Icons.Default.Description, if (bangla) "প্রজেক্ট তৈরি" else "Project maker",
                    if (bangla) "স্কুল প্রজেক্টের পরিকল্পনা ও ধাপ" else "Plan a school project", Color(0xFFFFF0E2)) {
                    onAsk(if (bangla) "আমার জন্য একটি স্কুল প্রজেক্ট তৈরি করো। বিষয়: " else "Help me create a school project about: ")
                }
                ToolCard(Icons.Default.Translate, if (bangla) "ইংরেজি চর্চা" else "English practice",
                    if (bangla) "শব্দ, ব্যাকরণ ও কথোপকথন" else "Vocabulary, grammar and speaking", Color(0xFFE8F0FF)) {
                    onAsk(if (bangla) "আমাকে সহজভাবে ইংরেজি শেখাও: " else "Help me practise English: ")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TipsAndUpdates, null, tint = Purple)
                Spacer(Modifier.width(8.dp))
                Text(if (bangla) "টিপস: শুধু উত্তর মুখস্থ নয়, কেন এমন হলো সেটাও জেনে নাও।" else "Tip: Learn why an answer works, not just the final answer.",
                    color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ToolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(20.dp), color = Color.White,
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(tint), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Ink, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Ink)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun ChatScreen(
    bangla: Boolean, messages: List<ChatMessage>, draft: String, onDraft: (String) -> Unit,
    imageUri: Uri?, onRemoveImage: () -> Unit, busy: Boolean, error: String,
    onVoice: () -> Unit, onImage: () -> Unit, onSend: () -> Unit, onSpeak: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(if (bangla) "নতুন কথোপকথন" else "New conversation",
                    color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (message.fromUser) Color(0xFFEAE6FF) else Color.White,
                        shape = RoundedCornerShape(19.dp), shadowElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 330.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            if (message.imageUri != null) {
                                Text(if (bangla) "ছবি সংযুক্ত করা হয়েছে" else "Image attached",
                                    color = Purple, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(message.text, color = Ink)
                            if (!message.fromUser) {
                                TextButton(onClick = { onSpeak(message.text) }, contentPadding = PaddingValues(0.dp)) {
                                    Icon(Icons.Default.VolumeUp, null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(5.dp)); Text(if (bangla) "শুনুন" else "Listen")
                                }
                            }
                        }
                    }
                }
            }
            if (busy) item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(9.dp))
                    Text(if (bangla) "Rajorshi উত্তর তৈরি করছে..." else "Rajorshi is thinking...", color = Muted)
                }
            }
            if (error.isNotBlank()) item { Text(error, color = Color.Red) }
        }
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (imageUri != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, tint = Purple)
                        Text(if (bangla) "ছবি প্রস্তুত" else "Image ready", modifier = Modifier.weight(1f), color = Purple)
                        IconButton(onClick = onRemoveImage) { Icon(Icons.Default.Close, null) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onImage, enabled = !busy) { Icon(Icons.Default.AddPhotoAlternate, null, tint = Purple) }
                    IconButton(onClick = onVoice, enabled = !busy) { Icon(Icons.Default.Mic, null, tint = Purple) }
                    OutlinedTextField(
                        value = draft, onValueChange = onDraft,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (bangla) "প্রশ্ন লিখো..." else "Message Rajorshi...") },
                        shape = RoundedCornerShape(22.dp), maxLines = 4
                    )
                    Spacer(Modifier.width(5.dp))
                    FilledIconButton(onClick = onSend, enabled = !busy && (draft.isNotBlank() || imageUri != null)) {
                        Icon(Icons.Default.Send, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnScreen(bangla: Boolean, onChoose: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (bangla) "শেখার বিষয় বেছে নাও" else "Choose a learning topic",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Text(if (bangla) "যে বিষয়টি বুঝতে চাও, সেটি বেছে নাও।" else "Pick a topic you want to understand.", color = Muted)
        listOf(
            (if (bangla) "গণিত" else "Mathematics") to "mathematics",
            (if (bangla) "সাধারণ বিজ্ঞান" else "General science") to "science",
            (if (bangla) "ইংরেজি ব্যাকরণ" else "English grammar") to "English grammar",
            (if (bangla) "বাংলা ব্যাকরণ" else "Bangla grammar"),
            (if (bangla) "পরিবেশ ও জলবায়ু" else "Environment and climate") to "environment",
            (if (bangla) "ইতিহাস ও সমাজবিজ্ঞান" else "History and social studies") to "history"
        ).forEach { pair ->
            ToolCard(Icons.Default.MenuBook, pair.first,
                if (bangla) "সহজ ব্যাখ্যা ও উদাহরণ" else "Simple explanations and examples", Color(0xFFECE9FF)) {
                onChoose(if (bangla) "${pair.first} বিষয়টি উদাহরণসহ বুঝিয়ে দাও।" else "Teach me ${pair.first} with examples.")
            }
        }
    }
}

@Composable
private fun ProfileScreen(bangla: Boolean, onLanguage: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(28.dp))
        Box(Modifier.size(86.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Purple, Violet))), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AutoAwesome, null, tint =
