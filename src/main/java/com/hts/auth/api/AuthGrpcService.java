package com.hts.auth.api;

import com.hts.auth.domain.model.ServiceResult;
import com.hts.auth.domain.service.AuthCommandService;
import com.hts.auth.domain.service.AuthQueryService;
import com.hts.generated.grpc.*;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class AuthGrpcService implements AuthService {

    @Inject AuthCommandService commandService;
    @Inject AuthQueryService queryService;

    @Override
    public Uni<LoginReply> login(LoginRequest request) {
        return commandService.login(
                        request.getAccountId(),
                        request.getPassword(),
                        request.getIpAddr(),
                        request.getUserAgent()
                )
                .map(this::toReply);
    }

    @Override
    public Uni<LogoutReply> logout(LogoutRequest request) {
        return queryService.validateSession(request.getSessionId())
                .flatMap(result -> {
                    if (!result.isSuccess()) {
                        return Uni.createFrom().item(toLogoutReply(AuthResult.SESSION_NOT_FOUND));
                    }
                    return commandService.logout(request.getSessionId(), result.accountId())
                            .map(this::toLogoutReply);
                });
    }

    @Override
    public Uni<ValidateSessionReply> validateSession(ValidateSessionRequest request) {
        return queryService.validateSession(request.getSessionId())
                .map(this::toValidateSessionReply);
    }

    private LoginReply toReply(ServiceResult result) {
        return LoginReply.newBuilder()
                .setCode(result.code())
                .setSessionId(result.sessionId())
                .setAccountId(result.accountId())
                .build();
    }

    private LogoutReply toLogoutReply(ServiceResult result) {
        return LogoutReply.newBuilder()
                .setCode(result.code())
                .build();
    }

    private LogoutReply toLogoutReply(AuthResult code) {
        return LogoutReply.newBuilder()
                .setCode(code)
                .build();
    }

    private ValidateSessionReply toValidateSessionReply(ServiceResult result) {
        return ValidateSessionReply.newBuilder()
                .setCode(result.code())
                .setAccountId(result.accountId())
                .setIsValid(result.isSuccess())
                .build();
    }
}
