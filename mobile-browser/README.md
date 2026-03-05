# Vista-D-NET Mobile Browser

Custom mobile browser apps that only load https://vista-d-net.world/ in fullscreen mode.

## Features
- ✅ **Single URL Lock**: Only loads vista-d-net.world
- ✅ **Fullscreen Mode**: No URL bar, no navigation controls
- ✅ **No External Links**: Blocks navigation to other sites
- ✅ **Cross-Platform**: Android & iOS versions
- ✅ **Bypass Browser Restrictions**: Custom WebView implementation

## Android Version

### Requirements
- Android Studio
- Android SDK 21+ (Android 5.0+)
- Java 8+

### Build Instructions
1. Open Android Studio
2. Import project from `mobile-browser/android/`
3. Sync Gradle files
4. Build → Build Bundle(s)/APK(s) → Build APK(s)

### Features
- WebView with JavaScript enabled
- Fullscreen immersive mode
- Blocks external URLs
- Portrait orientation only
- No back button functionality

## iOS Version

### Requirements
- Xcode 14+
- iOS 11.0+
- Swift 5+

### Build Instructions
1. Open Xcode
2. Open project from `mobile-browser/ios/VistaDNetBrowser.xcodeproj`
3. Select target device/simulator
4. Product → Run

### Features
- WKWebView with HTML5 support
- Status bar hidden
- Blocks external navigation
- Portrait orientation only
- Custom URL filtering

## Security Features
- **URL Whitelisting**: Only allows vista-d-net.world domains
- **Navigation Lock**: Prevents leaving the target site
- **No Address Bar**: Users cannot navigate to other sites
- **Fullscreen Immersion**: Hides system UI elements

## Deployment

### Android
```bash
# Generate signed APK
./gradlew assembleRelease
```

### iOS
- Use Xcode Archive
- Upload to App Store Connect
- Submit for review

## Benefits
- **No Chrome Restrictions**: Bypasses mobile browser limitations
- **Dedicated Experience**: Users only see your app
- **Professional Appearance**: Native app feel
- **Full Control**: Complete control over web content display
