# Forexanaly — Forex & Crypto Technical Analyst

A modern Android application built with **Kotlin** and **Jetpack Compose** for foreign exchange (Forex) and cryptocurrency technical analysis, interactive candlestick charting, AI-assisted market structure breakdowns, risk management, strategy backtesting, and beginner-friendly trading education.

---

## Key Features

### Interactive Candlestick Chart Terminal
- **Smooth Canvas Engine:** Pinch-to-zoom and pan across historical price action with mobile-friendly touch hit tolerance (`44px`).
- **Technical Indicators:** EMA (20, 50, 200), Bollinger Bands (20, 2.0), Auto Support & Resistance, MACD, and 14-period RSI subchart.
- **Drawing Tools:** Crosshair with OHLC snapping, horizontal S&R lines, trendlines, and Fibonacci retracements (0% to 100%).
- **Chart Snapshot:** Capture and share high-resolution chart images.

### Beginner-Friendly Learning & Guardrails
- **Practice Mode Banner & Glossary:** Guided `$10,000` virtual practice banner with an in-app **Beginner Trading Glossary** (*Candlestick*, *Support & Resistance*, *Fibonacci Retracement*, *Supply & Demand*, *Market Structure*).
- **Tap-to-Learn Indicator Tooltips:** Info (`i`) buttons next to each indicator in the config panel explain what it measures and how to read it.
- **1%–2% Risk Rule Guardrails:** Color-coded risk feedback in the Position Size Calculator (`<= 2%` Safe, `2%–5%` Elevated, `> 5%` High Risk Warning) plus a collapsible Pips & Lots guide.
- **"Why This Signal?" Breakdown:** Expandable plain-language rationale on every signal card.
- **High-Impact News Warnings:** Volatility alerts on `HIGH` impact Economic Calendar events advising beginners to avoid trading 15 minutes before/after releases.

### AI Technical Analyst
- **OpenRouter Integration & Local Fallback:** Connects to OpenRouter (`google/gemini-2.5-flash` / `mistralai/mistral-large`) when configured, or falls back to `DynamicAiResponseEngine` for deterministic, candle-grounded analysis.
- **Beginner Mode Chip:** Toggle **Beginner** to request plain-language explanations without heavy jargon.
- **Structure Analysis Chip:** One-tap **Structure** button sends recent 30-bar swing high, swing low, and close context to the AI.

### Live Trade Signals, Backtester & Watchlist
- **Trade Signals:** Real-time ATR-based Stop Loss / Take Profit setups with one-tap **"Inspect Targets on Live Chart"** and Room DB bookmarking.
- **Strategy Backtester:** Test strategies against historical candles with one-tap **Beginner Quick Presets** (*Safe Trend 1% Risk*, *RSI Bounce 1.5% Risk*).
- **Persistent Watchlist & Alerts:** Room-backed watchlist (`EUR/USD`, `GBP/USD`, `XAU/USD`, `BTC/USD`) and custom price/RSI alerts.
- **Technical Engine Helpers:** Includes `detectCrossUp`, `detectCrossDown`, and `projectHarmonicTarget` (Gartley `0.786`, Bat `0.886`, Butterfly `1.272`).

---

## Project Structure

```text
Forexanaly/
├── README.md                          # Repository overview & setup guide
├── LICENSE                            # License
├── Forex-Analyst-full-application.zip # Original application archive
└── Forex-Analyst/                     # Android Studio / Gradle project
    ├── .env.example                   # Template for local debug API keys
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/java/com/devcode940/ForexAnalyst/
    │       │   ├── data/              # AI engine, Room DB, APIs, security, validation
    │       │   ├── ui/                # Compose screens, components, theme, ViewModel
    │       │   └── worker/            # Background SignalWorker (WorkManager)
    │       └── test/                  # Unit & Robolectric tests
    ├── DEPLOYMENT.md                  # Deployment guide
    └── README.md                      # Detailed app documentation
```

---

## Getting Started

### Prerequisites
- **Android Studio** (Ladybug or newer recommended)
- **JDK 17**
- **Android SDK** (compile/target SDK configured in `Forex-Analyst/app/build.gradle.kts`)

### Build & Run
1. Open `Forex-Analyst/` in Android Studio, or build from the command line:
   ```bash
   cd Forex-Analyst
   ./gradlew assembleDebug
   ```
2. Run unit tests:
   ```bash
   cd Forex-Analyst
   ./gradlew testDebugUnitTest
   ```

---

## API Key Configuration

The app supports **Alpha Vantage** (`ALPHAVANTAGE_API_KEY`), **FCS API** (`FCS_API_KEY`), and **OpenRouter** (`OPENROUTER_API_KEY`).

### Local Debug Builds
Copy `Forex-Analyst/.env.example` to `Forex-Analyst/.env` (gitignored) and add your keys:
```env
FCS_API_KEY=your_real_fcs_key
OPENROUTER_API_KEY=your_real_openrouter_key
ALPHAVANTAGE_API_KEY=your_real_alphavantage_key
# GEMINI_API_KEY=your_real_gemini_key
```

### Release Builds
`BuildConfig` key fallback is **disabled in release builds** so API keys are never compiled into the APK. Users supply their own keys in-app, which are stored using **Android Keystore (`MasterKey` AES256-GCM)** and `EncryptedSharedPreferences` via `SecureApiKeyManager`.

---

## Architecture, Security & Tech Stack

- **Language:** 100% Kotlin
- **UI:** Jetpack Compose + Material Design 3 (Dynamic Light/Dark themes)
- **Architecture:** MVVM (`ForexViewModel` + `StateFlow` / `Flow`)
- **Database:** Room with KSP (`watchlist_items`, `saved_signals`, `custom_alerts`, `historical_candles`)
- **Networking & Resilience:** Retrofit 2, `MarketDataValidation` (OHLC & spike checks), `CircuitBreaker`, `RetryUtils`, and `WorkManager` (`SignalWorker`)
- **CI/CD:** GitHub Actions (`android_ci.yml`), CodeQL (`codeql.yml`), and Dependabot (`dependabot.yml`)
