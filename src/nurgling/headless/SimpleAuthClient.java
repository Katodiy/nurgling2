package nurgling.headless;

import haven.AuthClient;
import haven.HackThread;

/**
 * Simple authentication client for headless mode.
 * Uses username/password credentials instead of saved tokens.
 * Adapted from WebHaven's SimpleAuthClient.
 */
public class SimpleAuthClient {
    private final String host;
    private final int authPort;

    public SimpleAuthClient() {
        this("game.havenandhearth.com", 1871);
    }

    public SimpleAuthClient(String host, int authPort) {
        this.host = host;
        this.authPort = authPort;
    }

    /**
     * Authenticate with username and password.
     * Returns a SimpleAuthResponse containing the session cookie.
     */
    public SimpleAuthResponse authenticate(String username, String password) throws InterruptedException {
        SimpleAuthResponse response = new SimpleAuthResponse();

        // Run authentication in a HackThread to match expected threading model
        HackThread th = new HackThread(() -> {
            try {
                AuthClient client = new AuthClient(new haven.NamedSocketAddress(host, authPort));
                try {
                    AuthClient.NativeCred cred = new AuthClient.NativeCred(username, password);
                    String authedUser = cred.tryauth(client);
                    response.setCookie(client.getcookie());
                    response.setUsername(authedUser);
                } finally {
                    client.close();
                }
            } catch (AuthClient.Credentials.AuthException e) {
                response.setError("Authentication failed: " + e.getMessage());
            } catch (Exception e) {
                response.setError("Connection error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "HeadlessAuth");

        th.start();
        th.join(30000); // 30 second timeout

        if (th.isAlive()) {
            th.interrupt();
            response.setError("Authentication timeout");
        }

        return response;
    }

    /**
     * Authenticate using configuration.
     */
    public SimpleAuthResponse authenticate(HeadlessConfig config) throws InterruptedException {
        return authenticate(config.user, config.password);
    }
}
