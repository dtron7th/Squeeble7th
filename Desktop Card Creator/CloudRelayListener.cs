using System.IO;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;

namespace DesktopCardCreator;

public sealed class CloudRelayListener : IDisposable
{
    private readonly ClientWebSocket _ws = new();
    private readonly MainForm _mainForm;
    private readonly CancellationTokenSource _cts = new();
    private readonly string _deviceId;
    private bool _isConnected = false;

    public CloudRelayListener(MainForm mainForm)
    {
        _mainForm = mainForm;
        _deviceId = Environment.MachineName + "-" + Environment.UserName;
    }

    private static string LoadRelayUrl()
    {
        try
        {
            var configPath = Path.Combine(AppContext.BaseDirectory, "relay-config.json");
            if (File.Exists(configPath))
            {
                using var doc = JsonDocument.Parse(File.ReadAllText(configPath));
                var url = doc.RootElement.GetProperty("relayUrl").GetString();
                if (!string.IsNullOrWhiteSpace(url) && !url.Contains("your-relay-url"))
                    return url;
            }
        }
        catch { /* fall through to default */ }
        return string.Empty;
    }

    public async Task StartAsync()
    {
        var relayUrl = LoadRelayUrl();
        if (string.IsNullOrEmpty(relayUrl))
        {
            _mainForm.AppendLog("cloud relay: no relay URL configured in relay-config.json");
            _mainForm.AppendLog("cloud relay: deploy relay-server and update relay-config.json");
            return;
        }

        try
        {
            var wsUrl = $"{relayUrl}/ws/desktop?deviceId={Uri.EscapeDataString(_deviceId)}";
            await _ws.ConnectAsync(new Uri(wsUrl), _cts.Token);
            _isConnected = true;
            _mainForm.AppendLog($"cloud relay connected: {_deviceId}");
            _mainForm.AppendLog($"cloud relay URL: {relayUrl}");
            
            _ = Task.Run(ListenLoopAsync);
        }
        catch (Exception ex)
        {
            _mainForm.AppendLog($"cloud relay connect failed: {ex.Message}");
            _mainForm.AppendLog("cloud relay: check relay-config.json URL is correct");
        }
    }

    private async Task ListenLoopAsync()
    {
        var buffer = new byte[4096];
        while (_ws.State == WebSocketState.Open && !_cts.IsCancellationRequested)
        {
            try
            {
                var result = await _ws.ReceiveAsync(new ArraySegment<byte>(buffer), _cts.Token);
                if (result.MessageType == WebSocketMessageType.Text)
                {
                    var json = Encoding.UTF8.GetString(buffer, 0, result.Count);
                    await ProcessMessageAsync(json);
                }
                else if (result.MessageType == WebSocketMessageType.Close)
                {
                    await _ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closing", _cts.Token);
                    break;
                }
            }
            catch (Exception ex)
            {
                _mainForm.AppendLog($"cloud relay listen error: {ex.Message}");
                break;
            }
        }
        
        _isConnected = false;
        _mainForm.AppendLog("cloud relay disconnected");
    }

    private async Task ProcessMessageAsync(string json)
    {
        try
        {
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            
            if (root.TryGetProperty("type", out var typeProp) && typeProp.GetString() == "card-job")
            {
                var sha256Hash = root.GetProperty("sha256Hash").GetString() ?? string.Empty;
                
                if (!string.IsNullOrWhiteSpace(sha256Hash))
                {
                    var payload = new AccountPayload
                    {
                        Sha256Hash = sha256Hash
                    };
                    
                    _mainForm.BeginInvoke(() => _mainForm.RenderCard(payload));
                    
                    // Acknowledge job completion
                    var ack = new { type = "card-job-ack", jobId = root.GetProperty("jobId").GetString() };
                    var ackJson = JsonSerializer.Serialize(ack);
                    var ackBytes = Encoding.UTF8.GetBytes(ackJson);
                    await _ws.SendAsync(new ArraySegment<byte>(ackBytes), WebSocketMessageType.Text, true, _cts.Token);
                    
                    _mainForm.AppendLog($"cloud relay job processed: {sha256Hash[..8]}...");
                }
            }
        }
        catch (Exception ex)
        {
            _mainForm.AppendLog($"cloud relay message error: {ex.Message}");
        }
    }

    public async void Dispose()
    {
        _cts.Cancel();
        if (_isConnected && _ws.State == WebSocketState.Open)
        {
            await _ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "Disposing", CancellationToken.None);
        }
        _ws.Dispose();
        _cts.Dispose();
    }
}
