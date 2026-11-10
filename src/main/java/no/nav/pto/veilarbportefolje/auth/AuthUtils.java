package no.nav.pto.veilarbportefolje.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class AuthUtils {

    public static String getInnloggetBrukerToken() {
        return AuthContextHolderThreadLocal
                .instance().getIdTokenString()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is missing"));
    }

    public static VeilederId getInnloggetVeilederIdent() {
        return AuthContextHolderThreadLocal
                .instance().getNavIdent()
                .map(id -> VeilederId.of(id.get()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is missing from subject"));
    }

    public static String hentApplikasjonFraContex(AuthContextHolder authContextHolder) {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> getStringClaimOrEmpty(claims, "azp_name")) //  "cluster:team:app"
                .orElse(null);
    }

    public static UUID hentInnloggetVeilederUUID(AuthContextHolder authContextHolder) {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> getStringClaimOrEmpty(claims, "oid"))
                .map(UUID::fromString)
                .orElse(null);
    }

    public static boolean erSystemkallFraAzureAd(AuthContextHolder authContextHolder) {
        UserRole role = authContextHolder.getRole()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return UserRole.SYSTEM.equals(role) && harAADRolle(authContextHolder, "access_as_application");
    }

    public static boolean erSystemkallFraAzureAdMedRolle(AuthContextHolder authContextHolder, String rolle) {
        return erSystemkallFraAzureAd(authContextHolder) && harAADRolle(authContextHolder, rolle);
    }

    private static boolean harAADRolle(AuthContextHolder authContextHolder, String rolle) {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> {
                    try {
                        return Optional.ofNullable(claims.getStringListClaim("roles"));
                    } catch (ParseException e) {
                        return Optional.empty();
                    }
                })
                .orElse(emptyList())
                .contains(rolle);
    }

    public static Optional<String> getStringClaimOrEmpty(JWTClaimsSet claims, String claimName) {
        try {
            return ofNullable(claims.getStringClaim(claimName));
        } catch (Exception e) {
            return empty();
        }
    }
}
