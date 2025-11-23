package com.hts.auth.domain.model;

public record AuthWriteResult(boolean success, String message, String sessionId) {
    public static AuthWriteResult ok(String sessionId) {
        return new AuthWriteResult(true, "", sessionId);
    }
    public static AuthWriteResult failure(String message) {
        return new AuthWriteResult(false, message, "");
    }
}
