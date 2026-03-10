# Firebase Login System

A complete login system using Firebase Authentication with Google and Facebook sign-in options.

## Setup Instructions

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" and follow the setup steps
3. Enable Authentication in your project

### 2. Configure Authentication Providers
1. In Firebase Console, go to Authentication → Sign-in method
2. Enable **Google**:
   - Toggle Google to "Enabled"
   - Provide your project's authorized domains (e.g., localhost, yourdomain.com)
3. Enable **Facebook**:
   - Toggle Facebook to "Enabled"
   - Add your Facebook App ID and App Secret
   - Add authorized OAuth redirect URI from Firebase

### 3. Get Firebase Configuration
1. In Firebase Console, go to Project Settings → General
2. Under "Your apps", click the web app icon (</>)
3. Copy the Firebase configuration object

### 4. Update Managers.js
Replace the placeholder configuration in `Managers.js` with your actual Firebase config:

```javascript
const firebaseConfig = {
    apiKey: "your-actual-api-key",
    authDomain: "your-project-id.firebaseapp.com",
    projectId: "your-project-id",
    storageBucket: "your-project-id.appspot.com",
    messagingSenderId: "your-sender-id",
    appId: "your-app-id"
};
```

### 5. Facebook App Setup (Optional)
If using Facebook login:
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create a new app
3. Add "Facebook Login" product
4. Configure OAuth redirect URI in Facebook app settings
5. Copy App ID and App Secret to Firebase Console

## Features

- ✅ Google Sign-In
- ✅ Facebook Sign-In
- ✅ User session persistence
- ✅ Error handling with user-friendly messages
- ✅ Responsive design
- ✅ User profile display
- ✅ Sign out functionality

## File Structure

```
Patton AI/
├── Index.html          # Main HTML file with login UI
├── Managers.js         # Firebase authentication logic
└── README.md          # This file
```

## Usage

1. Open `Index.html` in a web browser
2. Click "Sign in with Google" or "Sign in with Facebook"
3. Complete the authentication flow in the popup
4. You'll be redirected to the home screen showing your user information
5. Click "Sign Out" to log out

## Development Notes

- Uses Firebase Authentication SDK (compat version for ease of use)
- Implements popup-based authentication
- Automatically handles authentication state changes
- Includes error handling for common issues
- Responsive design works on mobile and desktop

## Troubleshooting

**"auth/network-request-failed" error:**
- Check your internet connection
- Verify Firebase configuration is correct

**"auth/popup-closed-by-user" error:**
- User closed the popup before completing authentication
- Try signing in again

**Facebook login not working:**
- Ensure Facebook App is properly configured
- Check OAuth redirect URI settings
- Verify App ID and App Secret in Firebase Console

**Google login not working:**
- Ensure Google provider is enabled in Firebase Console
- Check authorized domains configuration
