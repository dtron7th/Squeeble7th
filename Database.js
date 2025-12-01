const fs = require('fs').promises;
const path = require('path');
const crypto = require('crypto');

// /c:/Users/Vinniejoe/Documents/GitHub/Squeeble7th/Database.js
//
// Lightweight file-based authentication/identity module.
// - No external deps
// - Passwords hashed with scrypt + salt
// - HMAC-signed tokens (JWT-like)
// - Refresh tokens & reset tokens persisted in JSON DB
//
// Usage (examples):
//   const db = require('./Database');
//   await db.init();
//   await db.register({ username: 'vin', email: 'v@example.com', password: 'secret' });
//   const { accessToken, refreshToken } = await db.authenticate('vin', 'secret');
//   const user = db.verifyAccessToken(accessToken);


const DB_PATH = path.join(__dirname, 'squeeble_db.json');
const SECRET = process.env.AUTH_SECRET || crypto.randomBytes(64).toString('hex');

const ACCESS_TOKEN_EXP = 15 * 60; // seconds
const REFRESH_TOKEN_EXP = 7 * 24 * 3600; // seconds
const RESET_TOKEN_EXP = 60 * 60; // seconds

function base64UrlEncode(buf) {
    return Buffer.from(buf)
        .toString('base64')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}
function base64UrlDecode(str) {
    str = str.replace(/-/g, '+').replace(/_/g, '/');
    while (str.length % 4) str += '=';
    return Buffer.from(str, 'base64');
}

function sign(data) {
    return crypto.createHmac('sha256', SECRET).update(data).digest();
}

function nowSeconds() {
    return Math.floor(Date.now() / 1000);
}

class Database {
    constructor() {
        this.dbPath = DB_PATH;
        this._db = null; // cached in-memory
    }

    async init() {
        try {
            await fs.access(this.dbPath);
        } catch {
            const initial = { users: [], refreshTokens: [], resetTokens: [] };
            await fs.writeFile(this.dbPath, JSON.stringify(initial, null, 2), 'utf8');
        }
        await this._load();
    }

    async _load() {
        const text = await fs.readFile(this.dbPath, 'utf8');
        this._db = JSON.parse(text);
    }

    async _save() {
        await fs.writeFile(this.dbPath, JSON.stringify(this._db, null, 2), 'utf8');
    }

    // Password helpers
    _hashPassword(password) {
        const salt = crypto.randomBytes(16).toString('hex');
        const derived = crypto.scryptSync(password, salt, 64).toString('hex');
        return `${salt}$${derived}`;
    }

    _verifyPassword(stored, supplied) {
        if (!stored || !supplied) return false;
        const [salt, derivedHex] = stored.split('$');
        if (!salt || !derivedHex) return false;
        const derivedSupplied = crypto.scryptSync(supplied, salt, 64);
        const storedBuf = Buffer.from(derivedHex, 'hex');
        const ok = crypto.timingSafeEqual(storedBuf, derivedSupplied);
        return ok;
    }

    // Token creation/verification (simple JWT-like)
    _createToken(payloadObj, expiresInSeconds = ACCESS_TOKEN_EXP) {
        const header = { alg: 'HS256', typ: 'JWT' };
        const payload = Object.assign({}, payloadObj, { iat: nowSeconds(), exp: nowSeconds() + expiresInSeconds });
        const headerB = base64UrlEncode(JSON.stringify(header));
        const payloadB = base64UrlEncode(JSON.stringify(payload));
        const signature = base64UrlEncode(sign(`${headerB}.${payloadB}`));
        return `${headerB}.${payloadB}.${signature}`;
    }

    _verifyToken(token) {
        try {
            const [h, p, s] = token.split('.');
            if (!h || !p || !s) return null;
            const expectedSig = base64UrlEncode(sign(`${h}.${p}`));
            const sigBuf = base64UrlDecode(s);
            const expectedBuf = base64UrlDecode(expectedSig);
            if (!crypto.timingSafeEqual(sigBuf, expectedBuf)) return null;
            const payload = JSON.parse(base64UrlDecode(p).toString('utf8'));
            if (payload.exp && nowSeconds() > payload.exp) return null;
            return payload;
        } catch {
            return null;
        }
    }

    // User operations
    async register({ username, email, password }) {
        if (!username || !email || !password) throw new Error('username, email and password required');
        username = username.toLowerCase();
        email = email.toLowerCase();
        await this._load();
        if (this._db.users.find(u => u.username === username)) throw new Error('username_taken');
        if (this._db.users.find(u => u.email === email)) throw new Error('email_taken');

        const id = crypto.randomUUID ? crypto.randomUUID() : crypto.randomBytes(16).toString('hex');
        const hashed = this._hashPassword(password);
        const user = { id, username, email, password: hashed, createdAt: nowSeconds() };
        this._db.users.push(user);
        await this._save();
        return { id, username, email };
    }

    async authenticate(usernameOrEmail, password) {
        if (!usernameOrEmail || !password) throw new Error('credentials required');
        usernameOrEmail = usernameOrEmail.toLowerCase();
        await this._load();
        const user = this._db.users.find(u => u.username === usernameOrEmail || u.email === usernameOrEmail);
        if (!user) throw new Error('invalid_credentials');
        if (!this._verifyPassword(user.password, password)) throw new Error('invalid_credentials');

        const accessToken = this._createToken({ sub: user.id, type: 'access' }, ACCESS_TOKEN_EXP);
        const refreshToken = this._createToken({ sub: user.id, type: 'refresh' }, REFRESH_TOKEN_EXP);

        // persist refresh token
        this._db.refreshTokens.push({ token: refreshToken, userId: user.id, expiresAt: nowSeconds() + REFRESH_TOKEN_EXP });
        await this._save();

        return { accessToken, refreshToken, user: { id: user.id, username: user.username, email: user.email } };
    }

    verifyAccessToken(token) {
        const payload = this._verifyToken(token);
        if (!payload) return null;
        if (payload.type !== 'access') return null;
        return payload; // contains sub, iat, exp
    }

    async refreshAccessToken(refreshToken) {
        if (!refreshToken) throw new Error('missing_refresh_token');
        await this._load();
        const stored = this._db.refreshTokens.find(r => r.token === refreshToken);
        if (!stored) throw new Error('invalid_refresh_token');
        if (nowSeconds() > stored.expiresAt) {
            // remove expired
            this._db.refreshTokens = this._db.refreshTokens.filter(r => r.token !== refreshToken);
            await this._save();
            throw new Error('refresh_token_expired');
        }
        const payload = this._verifyToken(refreshToken);
        if (!payload || payload.type !== 'refresh') throw new Error('invalid_refresh_token');

        const accessToken = this._createToken({ sub: payload.sub, type: 'access' }, ACCESS_TOKEN_EXP);
        return { accessToken };
    }

    async revokeRefreshToken(refreshToken) {
        await this._load();
        this._db.refreshTokens = this._db.refreshTokens.filter(r => r.token !== refreshToken);
        await this._save();
    }

    async changePassword(userId, oldPassword, newPassword) {
        if (!userId || !oldPassword || !newPassword) throw new Error('missing_params');
        await this._load();
        const user = this._db.users.find(u => u.id === userId);
        if (!user) throw new Error('user_not_found');
        if (!this._verifyPassword(user.password, oldPassword)) throw new Error('invalid_current_password');
        user.password = this._hashPassword(newPassword);
        await this._save();
        return true;
    }

    async generateResetToken(email) {
        if (!email) throw new Error('email required');
        email = email.toLowerCase();
        await this._load();
        const user = this._db.users.find(u => u.email === email);
        if (!user) throw new Error('user_not_found');
        const rawToken = crypto.randomBytes(32).toString('hex');
        const hashed = crypto.createHmac('sha256', SECRET).update(rawToken).digest('hex');
        const expiresAt = nowSeconds() + RESET_TOKEN_EXP;
        this._db.resetTokens.push({ userId: user.id, tokenHash: hashed, expiresAt });
        await this._save();
        return { resetToken: rawToken, expiresAt };
    }

    async verifyResetToken(rawToken) {
        if (!rawToken) return null;
        await this._load();
        const hashed = crypto.createHmac('sha256', SECRET).update(rawToken).digest('hex');
        const rec = this._db.resetTokens.find(r => r.tokenHash === hashed);
        if (!rec) return null;
        if (nowSeconds() > rec.expiresAt) {
            this._db.resetTokens = this._db.resetTokens.filter(r => r.tokenHash !== hashed);
            await this._save();
            return null;
        }
        return rec.userId;
    }

    async consumeResetToken(rawToken, newPassword) {
        const userId = await this.verifyResetToken(rawToken);
        if (!userId) throw new Error('invalid_reset_token');
        await this.changePassword(userId, newPassword, newPassword); // bypass old password: overwrite
        // remove all matching reset tokens for user
        const hashed = crypto.createHmac('sha256', SECRET).update(rawToken).digest('hex');
        this._db.resetTokens = this._db.resetTokens.filter(r => r.tokenHash !== hashed && r.userId !== userId);
        await this._save();
        return true;
    }

    async getUserById(id) {
        await this._load();
        const u = this._db.users.find(x => x.id === id);
        if (!u) return null;
        return { id: u.id, username: u.username, email: u.email, createdAt: u.createdAt };
    }

    // convenience: cleanup expired tokens (can be called periodically)
    async cleanupExpired() {
        await this._load();
        const now = nowSeconds();
        this._db.refreshTokens = this._db.refreshTokens.filter(r => r.expiresAt > now);
        this._db.resetTokens = this._db.resetTokens.filter(r => r.expiresAt > now);
        await this._save();
    }
}

module.exports = new Database();