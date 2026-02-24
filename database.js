// Vista-D-NET Hash Database System
// Stores only SHA-256 hashes - no user information
// Privacy-focused database implementation

class HashDatabase {
    constructor() {
        this.dbName = 'VistaDNET_HashDB';
        this.dbVersion = 1;
        this.db = null;
        this.objectStoreName = 'hashes';
    }

    // Initialize the database
    async init() {
        return new Promise((resolve, reject) => {
            console.log('🔍 Initializing hash database...');
            
            const request = indexedDB.open(this.dbName, this.dbVersion);
            
            request.onerror = (event) => {
                console.error('🔍 Database error:', event.target.error);
                reject(event.target.error);
            };
            
            request.onsuccess = (event) => {
                this.db = event.target.result;
                console.log('🔍 Database initialized successfully');
                resolve(this.db);
            };
            
            request.onupgradeneeded = (event) => {
                console.log('🔍 Creating database schema...');
                const db = event.target.result;
                
                // Create object store for hashes
                if (!db.objectStoreNames.contains(this.objectStoreName)) {
                    const objectStore = db.createObjectStore(this.objectStoreName, { 
                        keyPath: 'id', 
                        autoIncrement: true 
                    });
                    
                    // Create indexes for efficient querying
                    objectStore.createIndex('primaryHash', 'primaryHash', { unique: true });
                    objectStore.createIndex('backupHash', 'backupHash', { unique: true });
                    objectStore.createIndex('createdAt', 'createdAt', { unique: false });
                    objectStore.createIndex('source', 'source', { unique: false });
                    
                    console.log('🔍 Database schema created');
                }
            };
        });
    }

    // Store a new hash entry
    async storeHash(primaryHash, backupHash, source = 'signup') {
        console.log('🔍 Storing hash:', { primaryHash, backupHash, source });
        
        if (!this.db) {
            throw new Error('Database not initialized');
        }

        const hashEntry = {
            primaryHash: primaryHash,
            backupHash: backupHash,
            source: source, // 'signup' or 'login'
            createdAt: new Date().toISOString()
        };

        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.objectStoreName], 'readwrite');
            const objectStore = transaction.objectStore(this.objectStoreName);
            
            const request = objectStore.add(hashEntry);
            
            request.onsuccess = () => {
                console.log('🔍 Hash stored successfully with ID:', request.result);
                resolve(request.result);
            };
            
            request.onerror = (event) => {
                console.error('🔍 Error storing hash:', event.target.error);
                reject(event.target.error);
            };
        });
    }

    // Check if a hash already exists
    async hashExists(primaryHash) {
        console.log('🔍 Checking if hash exists:', primaryHash);
        
        if (!this.db) {
            throw new Error('Database not initialized');
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.objectStoreName], 'readonly');
            const objectStore = transaction.objectStore(this.objectStoreName);
            const index = objectStore.index('primaryHash');
            
            const request = index.get(primaryHash);
            
            request.onsuccess = () => {
                const exists = request.result !== undefined;
                console.log('🔍 Hash exists:', exists);
                resolve(exists);
            };
            
            request.onerror = (event) => {
                console.error('🔍 Error checking hash existence:', event.target.error);
                reject(event.target.error);
            };
        });
    }

    // Get all hashes (for admin/debug purposes)
    async getAllHashes() {
        console.log('🔍 Retrieving all hashes...');
        
        if (!this.db) {
            throw new Error('Database not initialized');
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.objectStoreName], 'readonly');
            const objectStore = transaction.objectStore(this.objectStoreName);
            
            const request = objectStore.getAll();
            
            request.onsuccess = () => {
                console.log('🔍 Retrieved hashes:', request.result.length);
                resolve(request.result);
            };
            
            request.onerror = (event) => {
                console.error('🔍 Error retrieving hashes:', event.target.error);
                reject(event.target.error);
            };
        });
    }

    // Get hash statistics
    async getStats() {
        console.log('🔍 Getting database statistics...');
        
        if (!this.db) {
            throw new Error('Database not initialized');
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.objectStoreName], 'readonly');
            const objectStore = transaction.objectStore(this.objectStoreName);
            
            const request = objectStore.count();
            
            request.onsuccess = () => {
                const stats = {
                    totalHashes: request.result,
                    databaseName: this.dbName,
                    version: this.dbVersion,
                    lastUpdated: new Date().toISOString()
                };
                console.log('🔍 Database stats:', stats);
                resolve(stats);
            };
            
            request.onerror = (event) => {
                console.error('🔍 Error getting stats:', event.target.error);
                reject(event.target.error);
            };
        });
    }

    // Clear all hashes (for testing/reset purposes)
    async clearAllHashes() {
        console.log('🔍 Clearing all hashes...');
        
        if (!this.db) {
            throw new Error('Database not initialized');
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.objectStoreName], 'readwrite');
            const objectStore = transaction.objectStore(this.objectStoreName);
            
            const request = objectStore.clear();
            
            request.onsuccess = () => {
                console.log('🔍 All hashes cleared');
                resolve();
            };
            
            request.onerror = (event) => {
                console.error('🔍 Error clearing hashes:', event.target.error);
                reject(event.target.error);
            };
        });
    }

    // Close database connection
    close() {
        if (this.db) {
            this.db.close();
            console.log('🔍 Database connection closed');
        }
    }
}

// Global database instance
let hashDatabase = null;

// Initialize database when page loads
document.addEventListener('DOMContentLoaded', async function() {
    try {
        hashDatabase = new HashDatabase();
        await hashDatabase.init();
        console.log('🔍 Hash database ready for use');
        
        // Make database globally available
        window.HashDatabase = hashDatabase;
        
    } catch (error) {
        console.error('🔍 Failed to initialize hash database:', error);
    }
});
