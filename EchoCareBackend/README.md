# Voice Alarm Backend Deployment Guide (GCP & Firebase)

This directory contains the production-ready Node.js & Express API backend for the **Voice Alarm** (formerly EchoCare) mobile application. It uses **Google Firestore** for real-time relational and reminder synchronization, and **Firebase Cloud Messaging (FCM)** to dispatch voice reminders to target devices.

---

## 1. Google Cloud Platform & Firebase Setup

### Step 1: Create a Project
1. Go to [Firebase Console](https://console.firebase.google.com/) or [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project named **Voice Alarm** (or link an existing one).

### Step 2: Initialize Firestore Database
1. In the Firebase Console, navigate to **Firestore Database** in the build panel.
2. Click **Create Database**, choose **Production Mode**, and set a region (e.g. `us-central1`).

### Step 3: Enable Firebase Cloud Messaging (FCM)
FCM is enabled by default with Firebase setups, allowing the server to push new alerts.

---

## 2. GitHub Secrets Configuration

To run the automated deployment pipeline in `.github/workflows/deploy-backend.yml`, go to your GitHub repository -> **Settings** -> **Secrets and variables** -> **Actions** and add these secrets:

| Secret Name | Value Description |
| :--- | :--- |
| `GCP_PROJECT_ID` | Your Google Cloud Project ID (e.g. `voice-alarm-12345`). |
| `GCP_SA_KEY` | The JSON key content of a GCP Service Account with permissions: `Cloud Run Admin`, `Artifact Registry Writer`, and `Storage Admin`. |

---

## 3. Android signing configuration

For automated signed Android compilations in `.github/workflows/build-android.yml`, add these secrets:

| Secret Name | Value Description |
| :--- | :--- |
| `ANDROID_KEYSTORE_BASE64` | The entire release keystore file `release-key.jks` converted to a Base64 string (`certutil -encode` on Windows or `base64` on Linux). |
| `KEYSTORE_PASSWORD` | Password for your release keystore. |
| `KEY_ALIAS` | Alias name for the signing key. |
| `KEY_PASSWORD` | Password for the signing key alias. |
