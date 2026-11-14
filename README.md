# Rajorshi AI — Android app (Version 1.1)

A student-friendly Android AI study companion with a distinctive purple-violet and mint Rajorshi identity.

## New in Version 1.1
- Unique Rajorshi home screen, logo treatment, colors and navigation
- Home, Chat, Learn and Profile tabs
- Quick tools for maths, science, projects and English practice
- Bangla / English interface switch
- Voice question input using Android speech recognition
- Spoken AI answers using Android Text-to-Speech
- Image picker for photographed or saved questions; image is sent to the AI backend for vision-based solving
- Improved student-focused AI tutor instructions

## Important status
This is a coded Android Studio starter project, not a signed APK. It has not been compiled or tested on a physical device in this environment. Real AI responses and image-question solving require the backend setup below. A working internet connection is required.

## Requirements
- Android Studio with Android Gradle Plugin 8.7.3 support
- JDK 17
- Android SDK 35
- Node.js 18+
- An OpenAI API key

## 1. Configure the backend
```bash
cd backend
npm install
cp .env.example .env
```
Put your API key in `backend/.env`:
```env
OPENAI_API_KEY=your_real_key_here
PORT=3000
```
Keep the key on the server. Never put it in Android source code or an APK.

Run locally:
```bash
npm start
```
Deploy the backend to an HTTPS host for phone use.

## 2. Connect Android app to backend
Open the project root in Android Studio. In `app/build.gradle.kts`, replace:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://YOUR_SERVER_URL\"")
```
with your HTTPS backend address:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://your-backend.example\"")
```
Sync Gradle, then run on an Android device or emulator.

For local Android emulator testing only, the host computer is usually reachable at `http://10.0.2.2:3000`; cleartext HTTP is disabled in this template, so use HTTPS or explicitly configure a debug-only cleartext exception. Do not enable cleartext traffic in a production build.

## 3. Build an APK
In Android Studio select **Build > Build Bundle(s) / APK(s) > Build APK(s)**. For release distribution, create a signed release APK or App Bundle.

## Privacy and student safety
- The image and text submitted in chat are sent to the configured backend and AI provider.
- Add a clear privacy notice, parental/school consent flow where required, rate limits, authentication, abuse protection and spending limits before public release.
- Do not collect student passwords, addresses or unnecessary personal information.
- Android speech recognition may rely on device services; supported languages vary by device.
- Do not publish the API key in the app.
