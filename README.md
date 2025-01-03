# AirClicker

AirClicker is a cross-platform application developed using **Kotlin Multiplatform**, to control desktop mouse using the motion sensors of mobile devices (Android/iOS). The mobile app communicates with a desktop server through *WebSocket* for real-time control. 


<img src="https://github.com/user-attachments/assets/cf9b8583-dea1-4629-bf5c-45e624abc176" alt="AirClicker" style="height: 400px; max-width: auto;">

## Overview

- **Kotlin Multiplatform**: Shared codebase for Android, iOS, and Desktop apps.
- **Desktop App**: Acts as a WebSocket server. It listens for incoming connections from the mobile device and translates the received data into mouse movements. **Ktor** library is used for network communication.
- **Mobile Apps**: 
   - **Android/iOS**: The Android and iOS apps uses built-in sensors (Android rotation vector sensor and Native iOS motion APIs) to control the mouse cursor based on device movement. Sends sensor data (yaw, pitch) over WebSocket to control the desktop mouse. 
   - **Touch Input**: Allows for manual mouse movement when the sensor control is turned off.

## How to Use

1. **Setup**:
   - Run the desktop app, and click **Start Server** to start the WebSocket server. Run below command to run Desktop App: 
      ```bash 
      ./gradlew desktopRun -DmainClass=com.mmk.airclicker.MainKt --quiet
      ``` 
   - Run the mobile app and connect to the desktop app over the same network by entering **HOST** and **PORT** values.

2. **Choose Your Control**:
   - **Motion Control**: Enable Sensor Control, and use your phone's motion sensors for cursor movement. Tilting back and forward will move mouse up and down. Rotating phone left and right will move mouse cursor left and right.
   - **Manual Input**: Switch to touch for precise manual control.

## Requirements

- A device with motion sensors (e.g., gyroscope, rotation vector sensor for Android).
- Desktop and phone on the same network.
- Desktop app is running to receive WebSocket data. And server is started
