namespace DesktopCardCreator;

public sealed class AccountPayload
{
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Dob { get; set; } = string.Empty;
    public string CardId { get; set; } = string.Empty;
    public string Sha256Hash { get; set; } = string.Empty;
}
