const qrcode = require('qrcode-terminal');
const os = require('os');

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

function showQRCode() {
  const localIP = getLocalIP();
  const mobileURL = `http://${localIP}:3000/mobile`;
  
  console.log('\n' + '='.repeat(50));
  console.log('📱 BLACK BATTLES MOBILE QR CODE');
  console.log('='.repeat(50));
  console.log(`\n📲 Mobile URL: ${mobileURL}`);
  console.log(`💻 Desktop URL: http://localhost:3000/`);
  console.log('\n📱 Scan this QR code with your phone:\n');
  
  // Generate QR code
  qrcode.generate(mobileURL, { small: true });
  
  console.log('\n💡 Tips:');
  console.log('   • Make sure phone and computer are on same WiFi');
  console.log('   • Server will auto-reload when you edit files');
  console.log('   • Use /mobile for forced mobile view');
  console.log('   • Add ?force=mobile to any URL for mobile view');
  console.log('\n' + '='.repeat(50) + '\n');
}

// Show QR code immediately
showQRCode();

// Also show QR code every 30 seconds in case you miss it
setInterval(showQRCode, 30000);

console.log('🔄 QR code will refresh every 30 seconds');
console.log('📝 Press Ctrl+C to exit\n');
