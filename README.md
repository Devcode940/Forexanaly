# Forexanaly

A repository containing the **Forex & Crypto Technical Analyst** Android application (`Forex-Analyst/`), built with **Kotlin** and **Jetpack Compose**.

## Project Structure

- `Forex-Analyst/` — Full Android application source code, Gradle build configuration, tests, and documentation.
  - See [`Forex-Analyst/README.md`](Forex-Analyst/README.md) for full feature, architecture, and database documentation.
  - See [`Forex-Analyst/DEPLOYMENT.md`](Forex-Analyst/DEPLOYMENT.md) for deployment instructions.

## Quick Start

1. Open `Forex-Analyst/` in Android Studio (or build via Gradle):
   ```bash
   cd Forex-Analyst
   ./gradlew assembleDebug
   ```
2. Configure API keys for local debug builds by copying `Forex-Analyst/.env.example` to `Forex-Analyst/.env`:
   ```env
   FCS_API_KEY=your_real_fcs_key
   OPENROUTER_API_KEY=your_real_openrouter_key
   ALPHAVANTAGE_API_KEY=your_real_alphavantage_key
   ```
   > **Note:** In release builds, `BuildConfig` key fallback is disabled. Users enter their own API keys in-app, where they are stored using `EncryptedSharedPreferences` via `SecureApiKeyManager`.
