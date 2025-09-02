package de.maxhenkel.audioplayer.webserver;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TokenManager {

    private final Map<UUID, Token> tokens;

    public TokenManager() {
        tokens = new ConcurrentHashMap<>();
    }

    public UUID generateToken(UUID playerId) {
        UUID token = UUID.randomUUID();
        tokens.put(token, new Token(token, playerId));
        return token;
    }

    /**
     * @param token the token
     * @return the player ID or <code>null</code> if the token is invalid
     */
    @Nullable
    public UUID useToken(UUID token) {
        Token t = tokens.get(token);
        if (t == null) {
            return null;
        }
        tokens.remove(token);
        if (!t.isValid()) {
            return null;
        }
        return t.getPlayerId();
    }

    public boolean isValidToken(UUID token) {
        Token t = tokens.get(token);
        if (t == null) {
            return false;
        }
        return t.isValid();
    }

    //TODO Clean tokens regularly
    public void cleanInvalidTokens() {
        tokens.values().removeIf(token -> !token.isValid());
    }

    protected static class Token {
        private final UUID token;
        private final UUID playerId;
        private final long time;

        public Token(UUID token, UUID playerId) {
            this.token = token;
            this.playerId = playerId;
            this.time = System.currentTimeMillis();
        }

        public UUID getToken() {
            return token;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public long getTime() {
            return time;
        }

        public boolean isValid() {
            return System.currentTimeMillis() - time <= WebServerEvents.WEB_SERVER_CONFIG.tokenTimeout.get();
        }
    }

}
