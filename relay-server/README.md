# Vista-D-NET Relay Server

Bridges the website (https://vista-d-net.world) to the desktop card creator app from any device globally.

## How it works
1. Phone user creates account on website
2. Website POSTs hash to this server
3. Desktop app connects via WebSocket and receives hash
4. Desktop generates QR card

## Deploy to Railway (free, fastest)

1. Go to https://railway.app and sign in with GitHub
2. Click **New Project → Deploy from GitHub repo**
3. Select the `Squeeble7th` repo, set root directory to `relay-server`
4. Railway auto-detects Node.js and deploys
5. Copy the generated URL (e.g. `vista-d-net-relay.up.railway.app`)
6. Set that URL in `Desktop Card Creator/relay-config.json` and in `Index.html`

## Deploy to Render (free alternative)

1. Go to https://render.com and sign in with GitHub
2. Click **New → Web Service**
3. Select `Squeeble7th` repo, root directory: `relay-server`
4. Build command: `npm install`
5. Start command: `npm start`
6. Copy the generated URL

## After deploying

Update these two places with your deployed URL:

### 1. Desktop app config (`Desktop Card Creator/relay-config.json`)
```json
{ "relayUrl": "wss://your-relay-url.up.railway.app" }
```

### 2. Website (`Index.html`) - search for `api.vista-d-net.world`
Replace with your relay URL:
```
https://your-relay-url.up.railway.app/relay/card-job
```

## Local testing
```bash
npm install
npm start
# Server runs on http://localhost:3000
```

Test status: http://localhost:3000/relay/status
