using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text.Json;

namespace DesktopCardCreator;

public sealed class FirebaseRelayListener : IDisposable
{
    private readonly MainForm _mainForm;
    private readonly CancellationTokenSource _cts = new();
    private readonly HttpClient _http = new();
    private readonly string _deviceId;
    private string _firebaseUrl = string.Empty;

    public FirebaseRelayListener(MainForm mainForm)
    {
        _mainForm = mainForm;
        _deviceId = (Environment.MachineName + "-" + Environment.UserName)
                        .Replace(" ", "-").ToLowerInvariant();
    }

    private bool LoadConfig()
    {
        try
        {
            var configPath = Path.Combine(AppContext.BaseDirectory, "relay-config.json");
            if (!File.Exists(configPath)) return false;

            using var doc = JsonDocument.Parse(File.ReadAllText(configPath));
            var url = doc.RootElement.GetProperty("firebaseUrl").GetString();
            if (string.IsNullOrWhiteSpace(url) || url.Contains("your-project")) return false;

            _firebaseUrl = url.TrimEnd('/');
            return true;
        }
        catch { return false; }
    }

    public async Task StartAsync()
    {
        if (!LoadConfig())
        {
            _mainForm.AppendLog("firebase relay: not configured (edit relay-config.json)");
            return;
        }

        _mainForm.AppendLog($"firebase relay: starting for device [{_deviceId}]");
        _ = Task.Run(() => ListenLoopAsync(_cts.Token));
        await Task.CompletedTask;
    }

    private async Task ListenLoopAsync(CancellationToken ct)
    {
        var url = $"{_firebaseUrl}/jobs/{_deviceId}.json";

        while (!ct.IsCancellationRequested)
        {
            try
            {
                using var request = new HttpRequestMessage(HttpMethod.Get, url);
                request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("text/event-stream"));

                using var response = await _http.SendAsync(request,
                    HttpCompletionOption.ResponseHeadersRead, ct);

                _mainForm.AppendLog("firebase relay: connected, listening for jobs...");

                using var stream = await response.Content.ReadAsStreamAsync(ct);
                using var reader = new System.IO.StreamReader(stream);

                var eventName = string.Empty;

                while (!reader.EndOfStream && !ct.IsCancellationRequested)
                {
                    var line = await reader.ReadLineAsync(ct) ?? string.Empty;

                    if (line.StartsWith("event:"))
                    {
                        eventName = line["event:".Length..].Trim();
                    }
                    else if (line.StartsWith("data:"))
                    {
                        var data = line["data:".Length..].Trim();
                        if (eventName == "put" && data != "null")
                        {
                            await ProcessPutEventAsync(data, url, ct);
                        }
                        eventName = string.Empty;
                    }
                }
            }
            catch (OperationCanceledException) { break; }
            catch (Exception ex)
            {
                _mainForm.AppendLog($"firebase relay error: {ex.Message}");
                await Task.Delay(5000, ct).ContinueWith(_ => { }); // retry after 5s
            }
        }

        _mainForm.AppendLog("firebase relay: stopped");
    }

    private async Task ProcessPutEventAsync(string data, string jobUrl, CancellationToken ct)
    {
        try
        {
            using var doc = JsonDocument.Parse(data);
            var root = doc.RootElement;

            // Firebase SSE put event: {"path":"/","data":{...}}
            JsonElement jobData;
            if (root.TryGetProperty("data", out var dataEl))
            {
                if (dataEl.ValueKind == JsonValueKind.Null) return;
                jobData = dataEl;
            }
            else
            {
                jobData = root;
            }

            if (!jobData.TryGetProperty("sha256Hash", out var hashEl)) return;
            var sha256Hash = hashEl.GetString();
            if (string.IsNullOrWhiteSpace(sha256Hash)) return;

            _mainForm.AppendLog($"firebase relay: job received [{sha256Hash[..8]}...]");

            var payload = new AccountPayload { Sha256Hash = sha256Hash };
            _mainForm.BeginInvoke(() => _mainForm.RenderCard(payload));

            // Delete the job from Firebase after processing
            using var deleteReq = new HttpRequestMessage(HttpMethod.Delete, jobUrl);
            await _http.SendAsync(deleteReq, ct);
            _mainForm.AppendLog("firebase relay: job acknowledged and cleared");
        }
        catch (Exception ex)
        {
            _mainForm.AppendLog($"firebase relay: job parse error: {ex.Message}");
        }
    }

    public void Dispose()
    {
        _cts.Cancel();
        _http.Dispose();
        _cts.Dispose();
    }
}
