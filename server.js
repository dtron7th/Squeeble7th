const express = require('express');
const path = require('path');
const os = require('os');
const qrcode = require('qrcode-terminal');
const fs = require('fs');
const app = express();
const port = 3000;

// Get local IP address for mobile access
function getLocalIP() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const interface of interfaces[name]) {
      if (interface.family === 'IPv4' && !interface.internal) {
        return interface.address;
      }
    }
  }
  return 'localhost';
}

// Middleware to detect mobile devices and force mobile view
app.use((req, res, next) => {
  const userAgent = req.headers['user-agent'] || '';
  const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent) ||
                   req.headers['user-agent']?.includes('Mobile') ||
                   req.query.force === 'mobile';
  
  // Add mobile detection to headers for the client-side code
  res.locals.isMobile = isMobile;
  
  // If mobile detected, force mobile view
  if (isMobile) {
    res.setHeader('X-Mobile-View', 'true');
  }
  
  next();
});

// Serve static files
app.use(express.static(__dirname));

// Track file modifications
let lastModified = Date.now();

// Watch for file changes
function watchFiles() {
  console.log('👀 Watching files for changes...');
  
  // Watch Index.html directly
  const indexPath = path.join(__dirname, 'Index.html');
  if (fs.existsSync(indexPath)) {
    console.log(`📁 Watching: Index.html`);
    fs.watchFile(indexPath, { interval: 1000 }, () => {
      lastModified = Date.now();
      console.log(`📝 Index.html modified! ${new Date().toLocaleTimeString()}`);
      console.log(`🔄 Mobile pages will reload automatically...`);
    });
  }
  
  // Watch CSS directory
  const cssPath = path.join(__dirname, 'CSS');
  if (fs.existsSync(cssPath)) {
    console.log(`📁 Watching: CSS directory`);
    fs.watch(cssPath, { recursive: true }, (eventType, filename) => {
      if (filename && filename.endsWith('.css')) {
        lastModified = Date.now();
        console.log(`📝 CSS changed: ${filename} ${new Date().toLocaleTimeString()}`);
        console.log(`🔄 Mobile pages will reload automatically...`);
      }
    });
  }
}

// Custom route for Index.html with live reload
app.get(['/', '/mobile'], (req, res) => {
  const indexPath = path.join(__dirname, 'Index.html');
  
  fs.readFile(indexPath, 'utf8', (err, data) => {
    if (err) {
      return res.status(500).send('Error loading page');
    }
    
    // Inject live reload script before closing body tag
    const liveReloadScript = `
    <script>
      (function() {
        let lastModified = ${lastModified};
        let checkCount = 0;
        
        function checkForUpdates() {
          checkCount++;
          
          fetch('/api/last-modified?t=' + Date.now())
            .then(response => response.json())
            .then(data => {
              if (data.lastModified > lastModified) {
                lastModified = data.lastModified;
                console.log('🔄 PAGE RELOADED! (Check #' + checkCount + ') at ' + new Date().toLocaleTimeString());
                window.location.reload();
              } else {
                if (checkCount % 10 === 0) { // Log every 10th check to reduce spam
                  console.log('✅ No changes (Check #' + checkCount + ')');
                }
              }
            })
            .catch(err => {
              console.log('❌ Check failed:', err);
            });
        }
        
        console.log('🔄 Live reload ACTIVE - checking every 1 second');
        console.log('📝 Try editing Index.html now!');
        
        // Check every 1 second
        setInterval(checkForUpdates, 1000);
        
        // Initial check after 1 second
        setTimeout(checkForUpdates, 1000);
      })();
    </script>`;
    
    // Inject script
    if (data.includes('</body>')) {
      data = data.replace('</body>', liveReloadScript + '</body>');
    } else {
      data += liveReloadScript;
    }
    
    res.send(data);
  });
});

// API endpoint for live reload
app.get('/api/last-modified', (req, res) => {
  res.json({ lastModified });
});

// Start server
const localIP = getLocalIP();
const mobileURL = `http://${localIP}:${port}/mobile`;

app.listen(port, '0.0.0.0', () => {
  console.log(`\n🚀 Black Battles Server Running!`);
  console.log(`\n📱 Mobile URL: ${mobileURL}`);
  console.log(`💻 Desktop URL: http://localhost:${port}/`);
  console.log(`🌐 Network URL: http://${localIP}:${port}/`);
  console.log(`\n📲 For Your Phone:`);
  console.log(`   1. Connect to same WiFi as computer`);
  console.log(`   2. Open browser and go to: ${mobileURL}`);
  console.log(`   3. Scan QR code below:\n`);
  
  // Generate QR code for mobile URL
  qrcode.generate(mobileURL, { small: true });
  
  console.log(`\n💡 Tips:`);
  console.log(`   - 🔄 Live reload is ACTIVE - edits auto-update phone!`);
  console.log(`   - Edit Index.html or CSS files to test`);
  console.log(`   - Check phone console for reload messages`);
  console.log(`   - Make sure firewall allows port ${port}\n`);
  
  // Start watching files for changes
  watchFiles();
});

// Handle graceful shutdown
process.on('SIGINT', () => {
  console.log('\n👋 Server shutting down gracefully...');
  process.exit(0);
});
