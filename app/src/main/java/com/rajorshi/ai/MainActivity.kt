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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
    val apiKey = "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"
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            ChatMessage("স্বাগতম! আমি Rajorshi AI। এখন আমাকে বাংলা বা ইংরেজিতে যেকোনো প্রশ্ন জিজ্ঞাসা করতে পারেন।", false)
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
                if (isLoading) {
                    item {
                        Text(
                            text = "Rajorshi AI উত্তর তৈরি করছে...",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
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
                    placeholder = { Text("যেকোনো প্রশ্ন লিখুন...", color = Color.Gray) },
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
                        if (inputText.isNotBlank() && !isLoading) {
                            val userQuery = inputText.trim()
                            messages.add(ChatMessage(userQuery, true))
                            inputText = ""
                            isLoading = true
                            coroutineScope.launch {
                                val reply = askGroqAI(userQuery, apiKey)
                                messages.add(ChatMessage(reply, false))
                                isLoading = false
                            }
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

suspend fun askGroqAI(prompt: String, apiKey: String): String = withContext(Dispatchers.IO) {
    val cleanKey = apiKey.trim()
    if (cleanKey == "YOUR_GROQ_API_KEY" || cleanKey.isBlank()) {
        return@withContext "দয়া করে কোডের ভেতরে আপনার ফ্রি Groq API Key যুক্ত করুন।"
    }
    try {
        val url = URL("https://groq.com")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "RajorshiAI-Android")
        conn.setRequestProperty("Authorization", "Bearer $cleanKey")
        conn.doOutput = true
        conn.doInput = true

        val msgObj = JSONObject()
            .put("role", "user")
            .put("content", prompt)
        val bodyObj = JSONObject()
            .put("model", "llama-3.3-70b-versatile")
            .put("messages", JSONArray().put(msgObj))

        OutputStreamWriter(conn.outputStream, "UTF-8").use {
            it.write(bodyObj.toString())
            it.flush()
        }

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""

        if (responseCode in 200..299) {
            val json = JSONObject(responseText)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } else {
            "সার্ভার ত্রুটি ($responseCode): $responseText"
        }
    } catch (e: Exception) {
        "ত্রুটি হয়েছে: ${e.localizedMessage}"
    }
}
