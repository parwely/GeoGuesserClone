# 🌍 GeoGuessr Android App - Complete Development Guide

## Project Overview

**Target Specifications:**
- Android 8.0+ (API Level 26+)
- ca. 5 concurrent users maximum
- $0/month operational cost
- Hybrid offline/online functionality
- Direct APK distribution

## 🏗️ Architecture & Technology Stack

### Frontend (Android)
- **Language:** Kotlin
- **Architecture:** MVVM + Repository Pattern
- **Database:** Room (local caching)
- **Networking:** Retrofit2 + OkHttp3
- **Real-time:** Socket.IO client
- **Maps:** OpenStreetMap (osmdroid)
- **UI:** Material Design Components

### Backend (Node.js)
- **Runtime:** Node.js + Express.js
- **Database:** Neon PostgreSQL with PostGIS extension
- **Real-time:** Socket.IO server
- **Authentication:** JWT + bcrypt
- **API Design:** RESTful + WebSocket

### Map and StreetView SOlution
-  **Primary** Google Maps SDK and Metadata from localhost server by Googles APIs
- **Alternative**: Mapillary sem
- **Fallback**: Custom Bitmap-Weltkarte

### Image Sources
- **Mapillary API**: Street-level Imagery (kostenlos)
- **Custom Collection**: Eigene GPS-markierte Fotos

### UI/UX Libraries
- **Material Design**: Android Material Components
- **Image Loading**: Glide

