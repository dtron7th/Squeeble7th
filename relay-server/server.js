const express = require('express');
const { WebSocketServer } = require('ws');
const http = require('http');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(express.json());

// CORS - allow vista-d-net.world
app.use((req, res, next) => {
    const origin = req.headers.origin || '*';
    res.header('Access-Control-Allow-Origin', origin);
    res.header('Access-Control-Allow-Methods', 'POST, GET, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Content-Type');
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    next();
});

// In-memory job queue: deviceId -> [jobs]
const jobQueue = new Map();

// Connected desktops: deviceId -> ws
const connectedDesktops = new Map();

// Job TTL: 15 minutes
const JOB_TTL_MS = 15 * 60 * 1000;

// Clean up expired jobs every minute
setInterval(() => {
    const now = Date.now();
    for (const [deviceId, jobs] of jobQueue.entries()) {
        const fresh = jobs.filter(j => now - j.createdAt < JOB_TTL_MS);
        if (fresh.length === 0) {
            jobQueue.delete(deviceId);
        } else {
            jobQueue.set(deviceId, fresh);
        }
    }
}, 60_000);

// POST /relay/card-job
// Website sends hash-only card job here
app.post('/relay/card-job', (req, res) => {
    const { sha256Hash, deviceId = 'default-device', timestamp } = req.body || {};

    if (!sha256Hash || typeof sha256Hash !== 'string') {
        return res.status(400).json({ ok: false, error: 'sha256Hash required' });
    }

    const job = {
        jobId: uuidv4(),
        sha256Hash,
        deviceId,
        timestamp: timestamp || new Date().toISOString(),
        createdAt: Date.now()
    };

    console.log(`[relay] job queued: ${job.jobId} for device: ${deviceId}`);

    // If desktop is connected, push immediately
    const desktopWs = connectedDesktops.get(deviceId);
    if (desktopWs && desktopWs.readyState === 1 /* OPEN */) {
        desktopWs.send(JSON.stringify({ type: 'card-job', jobId: job.jobId, sha256Hash: job.sha256Hash }));
        console.log(`[relay] job pushed directly to desktop: ${deviceId}`);
    } else {
        // Queue for later delivery when desktop connects
        if (!jobQueue.has(deviceId)) jobQueue.set(deviceId, []);
        jobQueue.get(deviceId).push(job);
        console.log(`[relay] desktop offline, job queued (${jobQueue.get(deviceId).length} pending)`);
    }

    res.json({ ok: true, jobId: job.jobId });
});

// GET /relay/status
app.get('/relay/status', (req, res) => {
    res.json({
        ok: true,
        connectedDesktops: connectedDesktops.size,
        pendingJobs: [...jobQueue.values()].reduce((a, b) => a + b.length, 0)
    });
});

const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws/desktop' });

wss.on('connection', (ws, req) => {
    const params = new URL(req.url, 'http://localhost').searchParams;
    const deviceId = params.get('deviceId') || 'default-device';

    connectedDesktops.set(deviceId, ws);
    console.log(`[relay] desktop connected: ${deviceId}`);

    // Flush any queued jobs
    const pending = jobQueue.get(deviceId) || [];
    if (pending.length > 0) {
        console.log(`[relay] flushing ${pending.length} pending jobs to ${deviceId}`);
        for (const job of pending) {
            ws.send(JSON.stringify({ type: 'card-job', jobId: job.jobId, sha256Hash: job.sha256Hash }));
        }
        jobQueue.delete(deviceId);
    }

    ws.on('message', (data) => {
        try {
            const msg = JSON.parse(data.toString());
            if (msg.type === 'card-job-ack') {
                console.log(`[relay] ack received for job: ${msg.jobId}`);
            }
        } catch { /* ignore malformed messages */ }
    });

    ws.on('close', () => {
        connectedDesktops.delete(deviceId);
        console.log(`[relay] desktop disconnected: ${deviceId}`);
    });

    ws.on('error', (err) => {
        console.error(`[relay] ws error for ${deviceId}:`, err.message);
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`[relay] server listening on port ${PORT}`);
});
