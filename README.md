# Energy Usage Monitor

## GitHub Repository
[Energy Usage Monitor GitHub](https://github.com/Shashhank12/Energy-Usage-Monitor)

## Overview
Our product is an Android application called “Energy Use Monitor.” Its goal is to support users in lowering their personal energy use through a range of useful features and incentives. 

## List of Features
* Registration Page (Email verification and 2FA enrollment, PP+TOU links)
* Login Page (Google sign-in or registered account with 2FA)
* Navigation Drawer
* Energy Monitor Charts (Usage or cost segmented by monthly billing periods)
* Monthly Prediction Feature (Linear regression)
* Settings Page (UtilityAPI and PG&E Data Upload Support)
* Home Dashboard (Text, Pie Chart)
* Push Notifications/Snackbar Banners
* Profile Page (includes editable monthly budget and image upload option)
* User Household Comparison
* Energy Analysis

## Architecture
![Energy Usage Monitor Architecture](/documentation/block_architecture_diagram/block_architecture_diagram.png)

## Running the Application
1. Prerequisites
    * Android Device or Emulator
2. Download the `app-debug.apk` file from the latest release
    * [Energy Usage Monitor Latest Release](https://github.com/Shashhank12/Energy-Usage-Monitor/releases/latest)
3. Install the APK on your Android device or emulator
4. Open the application and follow the steps below:
    1. Click "START HERE" to begin
    2. Register a new account by pressing "Register Here" or use Google sign-in
        * Note: If you choose to register, you will need to verify your email once registration is complete. It will be sent to the email that was used to register. Additionally, the phone number verification code is 123456 for demo purposes.
    4. Connect Data Source
        * You can either use UtilityAPI Demo Data or upload your own PG&E data file. The "?" button near the PG&E data upload button provides detailed steps on how to obtain the PG&E data file. Note that this PG&E data files is private and only stored locally on the device. It is not uploaded to any server.
    5. Once data is connected, you will be redirected to the home screen. You can navigate through the app using the navigation drawer on the left side of the screen.
        * The following menu options are available:
            * Profile: Manage your account, edit monthly budget, and upload a profile picture.
            * Settings: Connect to UtilityAPI or upload PG&E data files.
            * Energy Monitor: View your energy usage and cost segmented by monthly billing periods. Additionally, recieve monthly predictions based on linear regression analysis.
            * Home Dashboard: Displays your energy usage as an overview and recieve a usage breakdown in a pie chart format.
            * Energy Analysis: Analyze your energy usage patterns using Gemini and receive many types of analysis include comparison between other bills.
            * Household Comparison: Compare your energy usage with estimated usage based on your household size and occupancy.
            * Logout: Log out of your account and return to the login screen.

## 📹 Demo Video

https://drive.google.com/file/d/1TAoa7Wp35mkENUo5cdfkJBnt-kPdLZo7/view?usp=share_link 
