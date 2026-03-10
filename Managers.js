// Firebase configuration - Replace with your actual config
const firebaseConfig = {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.appspot.com",
    messagingSenderId: "YOUR_SENDER_ID",
    appId: "YOUR_APP_ID"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const googleProvider = new firebase.auth.GoogleAuthProvider();
const facebookProvider = new firebase.auth.FacebookAuthProvider();

class AuthManager {
    constructor() {
        this.initAuthStateListener();
    }

    // Listen for authentication state changes
    initAuthStateListener() {
        auth.onAuthStateChanged((user) => {
            if (user) {
                // User is signed in
                console.log('User is signed in:', user);
                this.showHomeSection(user);
                this.hideLoginSection();
            } else {
                // User is signed out
                console.log('User is signed out');
                this.showLoginSection();
                this.hideHomeSection();
            }
        });
    }

    // Google Sign In
    async signInWithGoogle() {
        try {
            const result = await auth.signInWithPopup(googleProvider);
            console.log('Google sign in successful:', result.user);
            return result.user;
        } catch (error) {
            console.error('Google sign in error:', error);
            this.showError(error.message);
        }
    }

    // Facebook Sign In
    async signInWithFacebook() {
        try {
            const result = await auth.signInWithPopup(facebookProvider);
            console.log('Facebook sign in successful:', result.user);
            return result.user;
        } catch (error) {
            console.error('Facebook sign in error:', error);
            this.showError(error.message);
        }
    }

    // Sign Out
    async signOut() {
        try {
            await auth.signOut();
            console.log('User signed out successfully');
        } catch (error) {
            console.error('Sign out error:', error);
            this.showError(error.message);
        }
    }

    // UI Management
    showLoginSection() {
        const loginSection = document.querySelector('.Login');
        if (loginSection) {
            loginSection.style.display = 'block';
        }
    }

    hideLoginSection() {
        const loginSection = document.querySelector('.Login');
        if (loginSection) {
            loginSection.style.display = 'none';
        }
    }

    showHomeSection(user) {
        const homeSection = document.querySelector('.Home');
        if (homeSection) {
            homeSection.style.display = 'block';
            homeSection.innerHTML = `
                <div class="user-info">
                    <h2>Welcome, ${user.displayName || user.email}!</h2>
                    <img src="${user.photoURL || 'https://via.placeholder.com/100'}" alt="User Avatar" style="border-radius: 50%; width: 100px; height: 100px;">
                    <p>Email: ${user.email}</p>
                    <button id="signOutBtn" style="padding: 10px 20px; background: #dc3545; color: white; border: none; border-radius: 5px; cursor: pointer;">Sign Out</button>
                </div>
            `;
            
            // Add sign out event listener
            const signOutBtn = document.getElementById('signOutBtn');
            if (signOutBtn) {
                signOutBtn.addEventListener('click', () => this.signOut());
            }
        }
    }

    hideHomeSection() {
        const homeSection = document.querySelector('.Home');
        if (homeSection) {
            homeSection.style.display = 'none';
        }
    }

    showError(message) {
        // Create or update error message
        let errorDiv = document.getElementById('error-message');
        if (!errorDiv) {
            errorDiv = document.createElement('div');
            errorDiv.id = 'error-message';
            errorDiv.style.cssText = 'color: red; margin-top: 10px; padding: 10px; background: #ffebee; border-radius: 5px;';
            const loginSection = document.querySelector('.Login');
            if (loginSection) {
                loginSection.appendChild(errorDiv);
            }
        }
        errorDiv.textContent = message;
        
        // Hide error after 5 seconds
        setTimeout(() => {
            errorDiv.textContent = '';
        }, 5000);
    }
}

// Initialize Auth Manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.authManager = new AuthManager();
    
    // Add event listeners to login buttons
    const googleBtn = document.getElementById('google-signin-btn');
    const facebookBtn = document.getElementById('facebook-signin-btn');
    
    if (googleBtn) {
        googleBtn.addEventListener('click', () => {
            window.authManager.signInWithGoogle();
        });
    }
    
    if (facebookBtn) {
        facebookBtn.addEventListener('click', () => {
            window.authManager.signInWithFacebook();
        });
    }
});
