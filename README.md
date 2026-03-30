# Smart Assistive Stick System

An end-to-end assistive safety platform that integrates embedded hardware, Android applications, and cloud services to support navigation and safety for visually impaired users.

---

## Overview

This project combines:

* Arduino-based hardware for environment sensing
* A user Android application for real-time assistance
* A caregiver Android application for remote monitoring
* Firebase for real-time data synchronization

The system enables obstacle detection, voice guidance, live location tracking, and emergency response.

---

## System Architecture

The system consists of three layers:

### 1. Hardware Layer

A smart stick built using Arduino and sensors to detect obstacles and trigger events.

### 2. Mobile Applications

* SmartAssistiveStickApp (user-side application)
* Caregiver application (monitoring-side application)

### 3. Cloud Layer

* Firebase Realtime Database for real-time data exchange

---

## Repository Structure

```text
SmartAssistiveStick/
|- SmartAssistiveStickApp/
|  |- app/
|  |- gradle/
|  |- build.gradle.kts
|  |- settings.gradle.kts
|  `- gradle.properties
|- Caregiver/
|  |- app/
|  |- gradle/
|  |- build.gradle.kts
|  |- settings.gradle.kts
|  `- gradle.properties
`- README.md
```

---

## Features

### SmartAssistiveStickApp (User Application)

* Bluetooth communication with HC-05 module
* Real-time voice alerts using Text-to-Speech
* Indoor and outdoor navigation modes
* Live location tracking using Google Maps
* SOS emergency alert system
* Background services for continuous operation

---

### Caregiver Application

* Real-time location tracking using Google Maps
* Live monitoring of user status
* SOS alert notifications
* Secure connection using unique access key

---

## Hardware Setup

### Ultrasonic Sensors

| Sensor | TRIG Pin | ECHO Pin |
| ------ | -------- | -------- |
| Front  | D9       | D8       |
| Left   | D7       | D6       |
| Right  | D5       | D4       |

---

### Other Components

| Component           | Arduino Pin |
| ------------------- | ----------- |
| Mode Switch (3-pin) | D2          |
| SOS Button          | D12         |
| Buzzer              | D3          |
| Bluetooth TX        | D10         |
| Bluetooth RX        | D11         |

---

### Power Connections

* All sensors connected to 5V and GND
* Bluetooth module connected to 3.3V and GND
* Common ground shared across all components

---

## Hardware Logic

### Distance Measurement

Ultrasonic sensors measure distance using echo timing:

distance = duration × 0.034 / 2

---

### Obstacle Detection Logic

| Code | Description             |
| ---- | ----------------------- |
| F1   | Obstacle ahead          |
| L1   | Obstacle on left        |
| R1   | Obstacle on right       |
| B1   | Obstacles on both sides |
| FL   | Obstacle ahead-left     |
| FR   | Obstacle ahead-right    |
| FB   | Path blocked            |
| SAFE | Path is clear           |

---

### Mode Handling

* Controlled via 3-pin switch on D2
* Indoor mode threshold: 20 cm ( we can change )
* Outdoor mode threshold: 40 cm ( we can change )

---

### Stabilization and Control

* Multiple readings required before confirming alert
* Delay between sensor reads to avoid interference
* Cooldown between alerts to prevent repetition

---

### SOS Functionality

* SOS button connected to D12
* Sends "SOS" signal via Bluetooth
* Includes cooldown to avoid repeated triggers

---

### Bluetooth Communication

* HC-05 module used
* TX connected to D10
* RX connected to D11

Arduino sends short codes such as F1, L1, SAFE, MODE_INDOOR, SOS.

---

## User Application Logic

1. Receives Bluetooth messages from Arduino
2. Converts codes into readable alerts
3. Provides voice output using Text-to-Speech
4. Avoids repeating duplicate alerts
5. Maintains priority handling:

   * SOS (highest priority)
   * Mode changes
   * Normal alerts

---

### Location and Emergency Handling

* Retrieves GPS location
* Updates Firebase in real-time
* Sends emergency alerts with location

---

## Caregiver Application Logic

### Connection

* Uses a unique access key to link with user

---

### Functionality

* Displays real-time user location on map
* Shows user status updates
* Receives SOS alerts
* Allows navigation to user location

---

## Firebase Backend

### Data Structure

```text
users/
  userId/
    latitude
    longitude
    mode
    sosActive
    accessKey

caregivers/
  caregiverId/
    linkedUserId
```

---

### Working

* User application updates data
* Caregiver application listens in real-time
* Changes are reflected instantly

---

## Complete Working Flow

1. Sensors detect obstacles
2. Arduino processes input
3. Sends message via Bluetooth
4. User app receives and announces alert
5. App updates Firebase
6. Caregiver app receives updates
7. SOS triggers emergency notification and location sharing

---

## Setup Instructions

### Requirements

* Android Studio
* Firebase project
* Google Maps API key

---

### Steps

1. Clone the repository:

```bash
git clone https://github.com/satish05112003/SmartAssistiveStick.git
```

2. Add API key in local.properties:

```properties
MAPS_API_KEY=YOUR_API_KEY
```

3. Add Firebase configuration:
   Place google-services.json inside each app module.

4. Build and run both applications.

---

## Security Practices

* API keys are not stored in source code
* local.properties is excluded from version control
* google-services.json is excluded from version control
* Build files and IDE files are ignored

---

## Future Improvements

* Advanced obstacle detection using AI
* Voice-based navigation with destination input
* Multi-language voice support
* Wearable integration
* Offline functionality

---

## Summary

This project is a complete assistive system where embedded hardware, mobile applications, and cloud services work together to provide real-time navigation assistance and emergency support for visually impaired users.
