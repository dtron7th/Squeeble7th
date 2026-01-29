const fs = require('fs');
const path = require('path');

console.log('🧪 Testing live reload functionality...\n');

const indexPath = path.join(__dirname, 'Index.html');

// Read current file
fs.readFile(indexPath, 'utf8', (err, data) => {
  if (err) {
    console.error('❌ Could not read Index.html:', err);
    return;
  }

  // Add a test comment with timestamp
  const timestamp = new Date().toISOString();
  const testComment = `<!-- Live reload test: ${timestamp} -->`;
  
  let newData;
  if (data.includes('<!-- Live reload test:')) {
    // Replace existing test comment
    newData = data.replace(/<!-- Live reload test:.*?-->/, testComment);
  } else {
    // Add new test comment at the beginning
    newData = testComment + '\n' + data;
  }

  // Write the file
  fs.writeFile(indexPath, newData, 'utf8', (err) => {
    if (err) {
      console.error('❌ Could not write to Index.html:', err);
      return;
    }

    console.log('✅ Test comment added to Index.html');
    console.log(`📝 Comment: ${testComment}`);
    console.log('\n🔄 Your phone should reload automatically within 1-2 seconds!');
    console.log('💻 Check your phone browser console for reload messages');
    
    // Remove the test comment after 5 seconds
    setTimeout(() => {
      fs.readFile(indexPath, 'utf8', (err, data) => {
        if (!err) {
          const cleanData = data.replace(/<!-- Live reload test:.*?-->\n?/, '');
          fs.writeFile(indexPath, cleanData, 'utf8', () => {
            console.log('\n🧹 Test comment removed - another reload should happen!');
          });
        }
      });
    }, 5000);
  });
});
