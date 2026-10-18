package com.buildat.account.api;

import java.net.URI;
import java.util.UUID;
import com.buildat.account.AccountService;
import com.buildat.http.ApplicationJsonApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@ApplicationJsonApi
@RequestMapping("/api/v1/me")
@SecurityRequirement(name = "supabaseBearer")
public class MeController {
    public record AccountDto(@jakarta.validation.constraints.NotNull UUID id) { }
    private final AccountService accounts;
    public MeController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve the current active Buildat account")
    @ApiResponse(responseCode = "200", description = "Current account")
    public ResponseEntity<AccountDto> me() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new AccountDto(accounts.requireActiveAccount().id()));
    }

    @PostMapping(path = "/bootstrap", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create the current account once, or retrieve its existing active mapping")
    @ApiResponse(responseCode = "200", description = "Existing account")
    @ApiResponse(responseCode = "201", description = "Account created; Location is /api/v1/me")
    public ResponseEntity<AccountDto> bootstrap() {
        var result = accounts.bootstrap();
        var response = result.created() ? ResponseEntity.created(URI.create("/api/v1/me")) : ResponseEntity.ok();
        return response.cacheControl(CacheControl.noStore()).body(new AccountDto(result.account().id()));
    }
}
