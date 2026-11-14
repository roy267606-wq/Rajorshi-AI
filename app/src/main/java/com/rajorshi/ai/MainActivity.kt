package com.rajorshi.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChatMessage(val text: String, val isFromUser: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                RajorshiAIScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RajorshiAIScreen() {
    var inputText by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMessage("স্বাগতম! আমি Rajorshi AI। আজকে আপনাকে কীভাবে সাহায্য করতে পারি?", false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rajorshi AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E2E),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF12121A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isFromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (msg.isFromUser) Color(0xFF3B82F6) else Color(0xFF27273A),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = Color.White,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E2E))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Rajorshi AI-কে কিছু জিজ্ঞাসা করুন...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val userQuery = inputText.trim()
                            messages.add(ChatMessage(userQuery, true))
                            inputText = ""
                            val reply = generateSmartReply(userQuery)
                            messages.add(ChatMessage(reply, false))
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Send", color = Color.White)
                }
            }
        }
    }
}

fun generateSmartReply(query: String): String {
    val q = query.lowercase()
    return when {
        q.contains("hi") || q.contains("hello") || q.contains("হ্যালো") || q.contains("কেমন আছ") ->
            "হ্যালো! আমি Rajorshi AI, একদম ভালো আছি। আপনার কী প্রশ্ন আছে বলুন?"
        q.contains("নাম") || q.contains("name") || q.contains("কে তুমি") ->
            "আমার নাম Rajorshi AI। আমি আপনার ব্যক্তিগত অ্যান্ড্রয়েড অ্যাসিস্ট্যান্ট।"
        q.contains("ধন্যবাদ") || q.contains("thanks") ->
            "আপনাকেও অসংখ্য ধন্যবাদ! যেকোনো প্রয়োজনে আমাকে জিজ্ঞাসা করতে পারেন।"
        else ->
            "আপনি লিখেছেন: \"$query\"\n\nRajorshi AI সফলভাবে আপনার ফোনে ইনস্টল হয়েছে! পরবর্তী ভার্সনে এখানে আপনার পছন্দমতো API যুক্ত করে আরও উন্নত উত্তর পেতে পারবেন।"
    }
}
