package nurgling.headless;

/**
 * Response from authentication containing username and session cookie.
 * Adapted from WebHaven's SimpleAuthResponse.
 */
public class SimpleAuthResponse {
    private String username;
    private byte[] cookie;
    private String error;
    private boolean success;

    public SimpleAuthResponse() {
        this.username = null;
        this.cookie = null;
        this.error = null;
        this.success = false;
    }

    public SimpleAuthResponse(String username, byte[] cookie) {
        this.username = username;
        this.cookie = cookie;
        this.error = null;
        this.success = true;
    }

    public static SimpleAuthResponse failure(String error) {
        SimpleAuthResponse resp = new SimpleAuthResponse();
        resp.error = error;
        resp.success = false;
        return resp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.success = true;
    }

    public byte[] getCookie() {
        return cookie;
    }

    public void setCookie(byte[] cookie) {
        this.cookie = cookie;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
        this.success = false;
    }

    public boolean isSuccess() {
        return success && cookie != null && cookie.length > 0;
    }
}
