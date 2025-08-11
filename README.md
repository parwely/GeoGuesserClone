# 🌍 GeoGuessr Android App - Complete Development Guide

## Project Overview

**Target Specifications:**
- Android 8.0+ (API Level 26+)
- 5 concurrent users maximum
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
- **Database:** PostgreSQL with PostGIS extension
- **Real-time:** Socket.IO server
- **Authentication:** JWT + bcrypt
- **API Design:** RESTful + WebSocket

### Infrastructure (Free Tier)
- **Hosting:** Vercel (Serverless Functions)
- **Database:** Neon PostgreSQL (500MB free tier)
- **Storage:** Vercel Blob for images
- **Domain:** Vercel subdomain (.vercel.app)

### Map Solutions?
- **Primary**: OpenStreetMap mit Leaflet WebView
- **Alternative**: Mapbox SDK (50k kostenlose Map Loads)
- **Fallback**: Custom Bitmap-Weltkarte
- **Tiles**: Vorgeladene OSM-Tiles in Assets

### Image Sources
- **Wikimedia Commons**: Geotagged Photos
- **Mapillary API**: Street-level Imagery (kostenlos)
- **Custom Collection**: Eigene GPS-markierte Fotos
- **Asset Bundle**: Vorinstallierte Bildsammlung

### UI/UX Libraries
- **Material Design**: Android Material Components
- **Image Loading**: Glide
- **Charts & Statistics**: MPAndroidChart
- **Maps**: OSMDroid oder WebView mit Leaflet

### Machine Learning (Optional)
- **TensorFlow Lite**: Offline Objekterkennung
- **ML Kit**: Text/Landmark Recognition
