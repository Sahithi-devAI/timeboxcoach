# TimeBox Coach

An Android app that blocks distracting apps outside a set time window — and makes you explain yourself before it lets you in.

Not a passive timer. A mirror.

---

## What it does

You pick an app to block (Instagram, YouTube, LinkedIn, anything) and set a narrow allowed window (e.g. 5:00–5:30 PM). Outside that window, opening the app shows a full-screen overlay asking:

> **Why can't this wait?**

You type a reason. The app logs it. Access stays blocked.

That's it. No approval path. No negotiation. Just a record of what you told yourself every time the impulse hit.

---

## Why it works this way

This app went through three stages before landing here:

- **Stage 1** — Always denied, any reason accepted. Designed to collect honest thoughts. What it actually collected: three days of genuine reflection, then 300 entries of `gdhsj` and `fr`. Lesson: if the outcome doesn't change, people stop thinking.
- **Stage 2** — Keyword evaluator. Long-term signals like `deadline`, `health`, `urgent` unlocked 5 minutes of access. Within a week the app was getting reasons like *"for job search which is crucial"* typed by someone who wanted to watch YouTube. Lesson: asking "why do you want in?" has infinite acceptable answers.
- **Stage 3 (current)** — No approval. The question changed to *"why can't this wait until you're at your computer?"* There's nowhere to negotiate with that. The logs are honest because there's no reward for gaming them.


---

## Requirements

- Android 8.1+ (API 27)
- **Two permissions** that must be manually granted after install (see Setup below)

---

## Setup

### Option A — Build from source (recommended)

1. Clone the repo
   ```bash
   git clone https://github.com/Sahithi-devAI/timeboxcoach.git
   ```

2. Open in Android Studio (Hedgehog or newer)

3. Connect your phone via USB with USB debugging enabled

4. Run → **Run 'app'**

   Or from the command line:
   ```bash
   ./gradlew installDebug
   ```

### Option B — Download APK

Download the latest APK from [Releases](https://github.com/Sahithi-devAI/timeboxcoach/releases), enable **Install from unknown sources** on your phone, and install it.

---

## Granting permissions (required)

After installing, the app needs two permissions that Android won't grant automatically:

**1. Display over other apps**

Settings → Apps → TimeBox Coach → Display over other apps → Allow

**2. Accessibility Service**

Settings → Accessibility → Installed apps → TimeBox Coach → Enable

The app will not block anything until both are granted.

---

## Adding a blocked app

1. Open TimeBox Coach
2. Tap **+**
3. Search for and select the app you want to block
4. Set the allowed time window (the one slot per day when the app is freely accessible)
5. Tap **Save Rule**

---

## Viewing your patterns

Tap **Stats** in the top bar to see:
- This week's impulse opens vs last week
- Which apps you're reaching for
- What time of day the impulse hits hardest
- Every reason you've typed this week

---

## Known limitations

**Work profile / Work Mode**

If your employer has enabled a work profile on your device, TimeBox Coach cannot monitor apps running inside that profile. Android's work profile is a separate sandbox — the accessibility service in your personal profile has no visibility into it. There is no workaround for MDM-managed devices.

**The app requires sideloading**

Apps using Android's Accessibility Service face strict Play Store review. This app is distributed via direct install.

---

## Tech

- Kotlin, Jetpack Compose, Material 3
- Android Accessibility Service API (`TYPE_WINDOW_STATE_CHANGED`)
- `TYPE_APPLICATION_OVERLAY` window for the block overlay
- WorkManager-free — no background scheduling, no network calls, all on-device
- Logs stored locally at `files/reason_logs.json` in app-private storage

---

## Pulling your logs

If you want to analyse your own data:

```bash
adb shell "run-as com.example.timeboxcoach cat /data/data/com.example.timeboxcoach/files/reason_logs.json" > reason_logs.json
```

---

## Contributing

This is a personal tool that I'm building in the open. If you're using it and hit something broken, open an issue. If you want to adapt it, fork away.

---

*Built with [Claude Code](https://claude.ai/code)*
