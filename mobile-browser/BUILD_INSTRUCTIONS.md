# Build Instructions for Vista-D-NET Mobile Browser

## Android APK Build

### Option 1: Using Android Studio (Recommended)
1. Install Android Studio from https://developer.android.com/studio
2. Open Android Studio
3. Click "Open an existing project"
4. Navigate to: `c:\Users\dtron\OneDrive\Documents\GitHub\Squeeble7th\mobile-browser\android`
5. Wait for Gradle sync to complete
6. Go to Build → Build Bundle(s)/APK(s) → Build APK(s)
7. APK will be in: `android/app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Command Line Build
```bash
# Navigate to android folder
cd "c:\Users\dtron\OneDrive\Documents\GitHub\Squeeble7th\mobile-browser\android"

# Initialize gradle wrapper (if needed)
gradle wrapper

# Build debug APK
gradlew.bat assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

## iOS App Build

### Requirements
- Mac computer with Xcode 14+
- Apple Developer Account ($99/year)
- iOS device for testing

### Build Steps
1. Install Xcode from Mac App Store
2. Open Xcode
3. Open project: `mobile-browser/ios/VistaDNetBrowser.xcodeproj`
4. Connect iOS device via USB
5. Select your device from target dropdown
6. Product → Run (for testing)
7. Product → Archive (for distribution)

### Distribution Options
- **TestFlight**: Free beta testing with up to 10,000 testers
- **App Store**: Public distribution (requires Apple approval)
- **Enterprise**: Internal distribution (requires $299/year)

## Alternative: Use Online Build Services

### Android
- **AppGyver**: Build APK online
- **AppYet**: Free Android app creator
- **AppsGeyser**: Simple web-to-APK converter

### iOS
- **AppGyver**: iOS builds available
- **Ionic Appflow**: Cloud builds
- **PhoneGap Build**: Cross-platform builds

## Quick Test APK

I can create a simple web-to-APK converter for immediate testing:

1. Go to https://www.appsgeyser.com/
2. Enter URL: https://vista-d-net.world/
3. Customize app name and icon
4. Download APK immediately

This will give you a working APK in minutes, though with less customization than our native app.

## Production Deployment

### Android
- **Google Play Store**: $25 one-time fee
- **Amazon Appstore**: Free
- **Direct APK**: Host on your website

### iOS
- **App Store**: $99/year
- **TestFlight**: Free beta testing
- **Enterprise**: $299/year for internal use
