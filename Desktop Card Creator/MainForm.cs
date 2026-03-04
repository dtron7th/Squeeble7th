using QRCoder;
using System.Drawing.Drawing2D;
using System.IO;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Threading;

namespace DesktopCardCreator;

public sealed class MainForm : Form
{
    private readonly RichTextBox _eventLog;
    private readonly RichTextBox _accountBox;
    private readonly RichTextBox _qrInfoBox;
    private readonly PictureBox _qrPicture;
    private readonly Label _statusLabel;
    private HttpListener _listener = new();
    private readonly CancellationTokenSource _cts = new();
    private FirebaseRelayListener? _firebaseRelay;

    public MainForm()
    {
        Text = "SQUIEBLE SECURE SYSTEM v1.0";
        Width = 1180;
        Height = 760;
        StartPosition = FormStartPosition.CenterScreen;
        FormBorderStyle = FormBorderStyle.FixedSingle;
        MaximizeBox = false;
        DoubleBuffered = true;
        BackColor = Color.FromArgb(0, 0, 0);

        var frame = new Panel
        {
            Left = 8,
            Top = 8,
            Width = ClientSize.Width - 16,
            Height = ClientSize.Height - 16,
            BackColor = Color.FromArgb(0, 0, 0),
            Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right
        };

        var header = new Panel
        {
            Left = 12,
            Top = 10,
            Width = frame.Width - 24,
            Height = 34,
            Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right
        };
        header.Paint += (_, e) =>
        {
            using var brush = new LinearGradientBrush(
                header.ClientRectangle,
                Color.FromArgb(36, 40, 61),
                Color.FromArgb(28, 31, 48),
                LinearGradientMode.Horizontal);
            e.Graphics.FillRectangle(brush, header.ClientRectangle);
            using var pen = new Pen(Color.FromArgb(146, 122, 232), 1.0f);
            e.Graphics.SmoothingMode = SmoothingMode.None;
            
            // Draw complete closed rectangle
            e.Graphics.DrawRectangle(pen, 0, 0, header.Width - 1, header.Height - 1);
            
            // Ensure corners are properly closed
            e.Graphics.DrawLine(pen, 0, 0, 0, header.Height - 1);           // Left edge
            e.Graphics.DrawLine(pen, header.Width - 1, 0, header.Width - 1, header.Height - 1); // Right edge
            e.Graphics.DrawLine(pen, 0, 0, header.Width - 1, 0);             // Top edge
            e.Graphics.DrawLine(pen, 0, header.Height - 1, header.Width - 1, header.Height - 1); // Bottom edge
        };

        var headerLabel = new Label
        {
            Text = "~: Vista-D-NET Dashboard | HASH-ONLY QR AUTH",
            Left = 10,
            Top = 8,
            Width = header.Width - 20,
            ForeColor = Color.FromArgb(200, 206, 228),
            BackColor = Color.Transparent,
            Font = new Font("Consolas", 11, FontStyle.Bold),
            Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right
        };
        header.Controls.Add(headerLabel);

        var mainPanel = new Panel
        {
            Left = 12,
            Top = 52,
            Width = frame.Width - 24,
            Height = frame.Height - 92,
            BackColor = Color.FromArgb(0, 0, 0),
            Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right
        };

        var eventsPanel = CreateModulePanel("1 event_stream", Color.FromArgb(151, 124, 231));
        eventsPanel.SetBounds(0, 0, 750, 390);

        _eventLog = CreateTerminalBox();
        _eventLog.SetBounds(12, 26, 726, 352);
        _eventLog.Text =
            "> QR renderer online\r\n" +
            "> waiting for account payload on 0.0.0.0:5055\r\n" +
            "> endpoint: /create-card\r\n";
        eventsPanel.Controls.Add(_eventLog);

        var accountPanel = CreateModulePanel("2 account_data (hash-only)", Color.FromArgb(138, 219, 166));
        accountPanel.SetBounds(0, 402, 750, 230);

        _accountBox = CreateTerminalBox();
        _accountBox.SetBounds(12, 26, 726, 192);
        _accountBox.Text =
            "Name: hash-only\r\n" +
            "DOB : -\r\n" +
            "Card: -\r\n" +
            "SHA : -\r\n";
        accountPanel.Controls.Add(_accountBox);

        var qrPanel = CreateModulePanel("3 qr_renderer", Color.FromArgb(124, 170, 255));
        qrPanel.SetBounds(762, 0, 378, 390);

        _qrPicture = new PictureBox
        {
            Left = 48,
            Top = 44,
            Width = 280,
            Height = 280,
            BackColor = Color.FromArgb(248, 251, 255),
            BorderStyle = BorderStyle.FixedSingle,
            SizeMode = PictureBoxSizeMode.Zoom
        };
        qrPanel.Controls.Add(_qrPicture);

        var qrLegend = new Label
        {
            Left = 44,
            Top = 334,
            Width = 290,
            Text = "SHA-256 HASH-ONLY LOGIN QR",
            ForeColor = Color.FromArgb(177, 200, 248),
            Font = new Font("Consolas", 11, FontStyle.Bold),
            BackColor = Color.Transparent,
            TextAlign = ContentAlignment.MiddleCenter
        };
        qrPanel.Controls.Add(qrLegend);

        var infoPanel = CreateModulePanel("4 auth_status", Color.FromArgb(248, 165, 133));
        infoPanel.SetBounds(762, 402, 378, 230);

        _qrInfoBox = CreateTerminalBox();
        _qrInfoBox.SetBounds(12, 26, 354, 192);
        _qrInfoBox.Text =
            "state: idle\r\n" +
            "mode : hash-only\r\n" +
            "hash : waiting\r\n" +
            "qr   : not rendered\r\n" +
            "\r\n" +
            "flow:\r\n" +
            "1) signup completed\r\n" +
            "2) sha256 generated\r\n" +
            "3) qr rendered\r\n";
        infoPanel.Controls.Add(_qrInfoBox);

        _statusLabel = new Label
        {
            Left = 0,
            Top = mainPanel.Height + 6,
            Width = mainPanel.Width,
            Text = "status: online | hash-only mode | listening on http://localhost:5055/create-card (lan capable)",
            ForeColor = Color.FromArgb(188, 197, 226),
            Font = new Font("Consolas", 12, FontStyle.Regular),
            BackColor = Color.Transparent,
            Anchor = AnchorStyles.Left | AnchorStyles.Right | AnchorStyles.Bottom
        };

        mainPanel.Controls.Add(eventsPanel);
        mainPanel.Controls.Add(accountPanel);
        mainPanel.Controls.Add(qrPanel);
        mainPanel.Controls.Add(infoPanel);
        frame.Controls.Add(header);
        frame.Controls.Add(mainPanel);
        frame.Controls.Add(_statusLabel);
        Controls.Add(frame);

        Load += (_, _) => StartListener();
        FormClosing += (_, _) =>
        {
            _cts.Cancel();
            if (_listener.IsListening) _listener.Stop();
            _listener.Close();
            _firebaseRelay?.Dispose();
        };
    }

    private static IEnumerable<string> GetLanIPv4Addresses()
    {
        var results = new List<string>();

        foreach (var nic in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (nic.OperationalStatus != OperationalStatus.Up)
            {
                continue;
            }

            var props = nic.GetIPProperties();
            foreach (var unicast in props.UnicastAddresses)
            {
                if (unicast.Address.AddressFamily != AddressFamily.InterNetwork)
                {
                    continue;
                }

                if (IPAddress.IsLoopback(unicast.Address))
                {
                    continue;
                }

                results.Add(unicast.Address.ToString());
            }
        }

        return results.Distinct();
    }

    private static Panel CreateModulePanel(string title, Color borderColor)
    {
        var panel = new Panel
        {
            BackColor = Color.FromArgb(0, 0, 0)
        };

        panel.Paint += (_, e) =>
        {
            var bounds = new Rectangle(0, 0, panel.Width - 1, panel.Height - 1);
            using var pen = new Pen(borderColor, 1.0f);
            e.Graphics.SmoothingMode = SmoothingMode.None;
            
            // Draw complete closed rectangle
            e.Graphics.DrawRectangle(pen, bounds);
            
            // Ensure corners are properly closed
            e.Graphics.DrawLine(pen, 0, 0, 0, panel.Height - 1);           // Left edge
            e.Graphics.DrawLine(pen, panel.Width - 1, 0, panel.Width - 1, panel.Height - 1); // Right edge
            e.Graphics.DrawLine(pen, 0, 0, panel.Width - 1, 0);             // Top edge
            e.Graphics.DrawLine(pen, 0, panel.Height - 1, panel.Width - 1, panel.Height - 1); // Bottom edge

            var titleSize = e.Graphics.MeasureString(title, new Font("Consolas", 10, FontStyle.Bold));
            var titleRect = new RectangleF(10, -1, titleSize.Width + 8, 16);
            using var bgBrush = new SolidBrush(panel.BackColor);
            using var textBrush = new SolidBrush(borderColor);
            e.Graphics.FillRectangle(bgBrush, titleRect);
            e.Graphics.DrawString(title, new Font("Consolas", 10, FontStyle.Bold), textBrush, 14, 0);
        };

        return panel;
    }

    private static RichTextBox CreateTerminalBox()
    {
        return new RichTextBox
        {
            ReadOnly = true,
            Multiline = true,
            DetectUrls = false,
            BackColor = Color.FromArgb(0, 0, 0),
            ForeColor = Color.FromArgb(204, 213, 236),
            BorderStyle = BorderStyle.None,
            ScrollBars = RichTextBoxScrollBars.Vertical,
            Font = new Font("Consolas", 12, FontStyle.Regular),
            TabStop = false,
            ShortcutsEnabled = false
        };
    }

    public void AppendLog(string line)
    {
        _eventLog.AppendText($"{DateTime.Now:HH:mm:ss}  {line}{Environment.NewLine}");
        _eventLog.SelectionStart = _eventLog.TextLength;
        _eventLog.ScrollToCaret();
    }

    private void StartListener()
    {
        try
        {
            if (TryStartListener("http://+:5055/"))
            {
                AppendLog("listener started (lan enabled)");
                _statusLabel.Text = "status: online | listening on http://0.0.0.0:5055/create-card";
                foreach (var lanIp in GetLanIPv4Addresses())
                {
                    AppendLog($"phone bridge: ?desktopHost={lanIp}");
                }
            }
            else if (TryStartListener("http://localhost:5055/"))
            {
                AppendLog("listener started (localhost only)");
                AppendLog("tip: run app as admin to enable LAN phone bridge on port 5055");
                _statusLabel.Text = "status: online | listening on http://localhost:5055/create-card";
            }
            else
            {
                throw new InvalidOperationException("Unable to bind listener on port 5055.");
            }

            _ = Task.Run(ListenLoopAsync);
            
            // Start Firebase relay listener for global reach
            _firebaseRelay = new FirebaseRelayListener(this);
            _ = _firebaseRelay.StartAsync();
        }
        catch (Exception ex)
        {
            AppendLog($"listener failed: {ex.Message}");
            _statusLabel.Text = $"Listener failed: {ex.Message}";
        }
    }

    private bool TryStartListener(string prefix)
    {
        try
        {
            if (_listener.IsListening)
            {
                _listener.Stop();
            }

            _listener.Close();
            _listener = new HttpListener();
            _listener.Prefixes.Add(prefix);
            _listener.Start();
            return true;
        }
        catch (Exception ex)
        {
            AppendLog($"bind failed ({prefix}): {ex.Message}");
            return false;
        }
    }

    private async Task ListenLoopAsync()
    {
        while (!_cts.IsCancellationRequested)
        {
            HttpListenerContext context;
            try
            {
                context = await _listener.GetContextAsync();
            }
            catch
            {
                return;
            }

            _ = Task.Run(() => HandleRequestAsync(context));
        }
    }

    private async Task HandleRequestAsync(HttpListenerContext context)
    {
        var response = context.Response;
        response.Headers.Add("Access-Control-Allow-Origin", "*");
        response.Headers.Add("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.Headers.Add("Access-Control-Allow-Headers", "Content-Type");
        response.Headers.Add("Access-Control-Allow-Private-Network", "true");

        // Log incoming request
        AppendLog($"request received: {context.Request.HttpMethod} {context.Request.Url?.AbsolutePath}");
        AppendLog($"origin: {context.Request.Headers["Origin"] ?? "unknown"}");

        if (context.Request.HttpMethod == "OPTIONS")
        {
            response.StatusCode = 204;
            response.Close();
            return;
        }

        if (context.Request.HttpMethod != "POST" || context.Request.Url?.AbsolutePath != "/create-card")
        {
            response.StatusCode = 404;
            response.Close();
            return;
        }

        try
        {
            using var reader = new StreamReader(context.Request.InputStream, context.Request.ContentEncoding ?? Encoding.UTF8);
            var body = await reader.ReadToEndAsync();
            var payload = JsonSerializer.Deserialize<AccountPayload>(body, new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            });

            if (payload is null || string.IsNullOrWhiteSpace(payload.Sha256Hash))
            {
                response.StatusCode = 400;
                await WriteJsonAsync(response, "{\"ok\":false,\"error\":\"Invalid payload\"}");
                return;
            }

            BeginInvoke(() => RenderCard(payload));
            response.StatusCode = 200;
            await WriteJsonAsync(response, "{\"ok\":true}");
        }
        catch (Exception ex)
        {
            response.StatusCode = 500;
            await WriteJsonAsync(response, $"{{\"ok\":false,\"error\":\"{EscapeJson(ex.Message)}\"}}");
        }
    }

    private static async Task WriteJsonAsync(HttpListenerResponse response, string json)
    {
        var bytes = Encoding.UTF8.GetBytes(json);
        response.ContentType = "application/json";
        response.ContentLength64 = bytes.LongLength;
        await response.OutputStream.WriteAsync(bytes, 0, bytes.Length);
        response.Close();
    }

    public void RenderCard(AccountPayload payload)
    {
        AppendLog("payload received (hash-only mode)");
        AppendLog("sha256 accepted, rendering qr");

        var nameValue = string.IsNullOrWhiteSpace(payload.FirstName) && string.IsNullOrWhiteSpace(payload.LastName)
            ? "hash-only"
            : $"{payload.FirstName} {payload.LastName}".Trim();
        var dobValue = string.IsNullOrWhiteSpace(payload.Dob) ? "-" : payload.Dob;
        var cardValue = string.IsNullOrWhiteSpace(payload.CardId) ? "-" : payload.CardId;

        _accountBox.Text =
            $"name : {nameValue}{Environment.NewLine}" +
            $"dob  : {dobValue}{Environment.NewLine}" +
            $"card : {cardValue}{Environment.NewLine}" +
            $"sha  : {payload.Sha256Hash}{Environment.NewLine}";

        _statusLabel.Text = $"status: rendered | hash-only payload | time: {DateTime.Now:T}";

        using var qrGen = new QRCodeGenerator();
        using var qrData = qrGen.CreateQrCode(payload.Sha256Hash, QRCodeGenerator.ECCLevel.Q);
        using var qr = new QRCode(qrData);
        var image = qr.GetGraphic(10, Color.Black, Color.White, true);

        var previous = _qrPicture.Image;
        _qrPicture.Image = new Bitmap(image);
        previous?.Dispose();

        _qrInfoBox.Text =
            $"state: active{Environment.NewLine}" +
            $"mode : hash-only{Environment.NewLine}" +
            $"hash : {payload.Sha256Hash[..Math.Min(24, payload.Sha256Hash.Length)]}...{Environment.NewLine}" +
            $"qr   : rendered{Environment.NewLine}" +
            Environment.NewLine +
            "flow:" + Environment.NewLine +
            "1) signup completed" + Environment.NewLine +
            "2) sha256 generated" + Environment.NewLine +
            "3) qr rendered" + Environment.NewLine +
            "4) scanner login ready";
    }

    private static string EscapeJson(string input)
        => input.Replace("\\", "\\\\").Replace("\"", "\\\"");
}
