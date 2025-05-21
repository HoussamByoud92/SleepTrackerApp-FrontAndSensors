# 🌙 SleepTracker – AI-Powered Sleep Monitoring & Analysis App

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/platform-Android-green.svg)]()
[![Status: v1.0.0](https://img.shields.io/badge/version-1.0.0-blue.svg)]()

## 🧠 About SleepTracker

**SleepTracker** is an innovative Android application that leverages AI and mobile sensors to monitor and analyze your sleep patterns non-invasively. By using your smartphone's **accelerometer** and **microphone**, SleepTracker gives you detailed insights and recommendations to improve your sleep quality — without requiring wearable devices.

---

## 🚀 Features

✅ **AI-Powered Analysis**  
✅ **Sleep Stage Detection (REM, Deep, Light)**  
✅ **Smart Alarm System** (based on sleep cycle & debt)  
✅ **Movement & Sound Monitoring**  
✅ **Sleep Debt Calculation**  
✅ **Data Visualization & Trends**  
✅ **Secure Authentication System**

---

## 📱 Mobile App Architecture

```
app/
│
├── auth/                # Login & Register
├── home/                # Dashboard, History, Insights
├── sensor/              # Sensor handling (movement, sound)
├── analysis/            # Sleep analysis algorithms
├── alarm/               # Smart alarm logic
├── api/                 # Retrofit API client
├── model/               # Data classes (User, Session, Insight)
├── utils/               # Session management, helpers
└── layout/              # XML UI files
```

---

## 🌐 Backend Architecture

- **Language:** PHP 7.4+  
- **Database:** MySQL 5.7+  
- **API:** RESTful endpoints  
- **Security:** Bcrypt password hashing + Token authentication

### 📁 API Endpoints

```php
POST /register.php        // Register user  
POST /login.php           // Login user  
POST /add_sleep_session.php // Submit sleep session  
GET  /get_user_sessions.php // Get user sleep history  
```

---

## 📊 Screenshots

| Register | Dashboard | AI Insights |
|---------|-----------|-------------|
| ![Register](assets/register.png) | ![Dashboard](assets/home.png) | ![AI](assets/ai_insights.png) |

---

## 🧠 Smart Alarm Logic

- Calculates sleep debt
- Adjusts alarm within optimal wake window
- Uses last sleep sessions to compute best wake time

---

## 🛠️ Tech Stack

| Frontend       | Backend        | AI Analysis      |
|----------------|----------------|------------------|
| Java, XML      | PHP, MySQL     | Java (custom)    |
| Android SDK 21+| RESTful API    | Sensor algorithms|
| Retrofit       | Token Auth     | Sleep scoring    |

---

## 📦 Installation

### ✅ Prerequisites
- Android Studio (Arctic Fox or higher)
- PHP server (XAMPP, WAMP, etc.)
- MySQL database

### ⚙️ Setup Steps
```bash
# Clone the project
git clone https://github.com/HoussamByoud92/SleepTrackerApp-FrontAndSensors

# Import Android project in Android Studio
# Start backend server (Apache + MySQL)
```

---

## 🧑‍💻 Contributors

- **Houssam BYOUD** – [byoudhoussam@gmail.com](mailto:byoudhoussam@gmail.com)  
- **Nada JAMIM** – [nadajamim00@gmail.com](mailto:nadajamim00@gmail.com)  
- Supervisor: **Mr. Mohamed LACHGAR**

---

## 📚 License

This project is licensed under the [MIT License](LICENSE).

---

## 🌐 Useful Links

- 📂 **Project Repository:**  
  [https://github.com/HoussamByoud92/SleepTrackerApp-FrontAndSensors](https://github.com/HoussamByoud92/SleepTrackerApp-FrontAndSensors)

- 📄 **Live Documentation:**  
  [README.md](https://github.com/HoussamByoud92/SleepTrackerApp-FrontAndSensors/README.md)

---

> "Let AI wake you smarter and sleep you deeper." 💤