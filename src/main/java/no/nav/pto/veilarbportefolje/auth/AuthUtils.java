package no.nav.pto.veilarbportefolje.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidsListeController.emptyArbeidsliste;

public class AuthUtils {

    static Bruker fjernKonfidensiellInfo(Bruker bruker) {
        return bruker.setFnr("").setKjonn("").setFodselsdato(null)
                .setEtternavn("").setFornavn("")
                .setArbeidsliste(emptyArbeidsliste())
                .setSkjermetTil(null)
                .setFoedeland(null)
                .setLandgruppe(null)
                .setTegnspraaktolk(null)
                .setTalespraaktolk(null)
                .setHovedStatsborgerskap(null)
                .setHarFlereStatsborgerskap(false);
    }

    static void test(String navn, Object data, boolean matches) {
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, format("sjekk av %s feilet, %s", navn, data));
        }
    }

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

    public static boolean harAADRolleForSystemTilSystemTilgang(AuthContextHolder authContextHolder) {
        return authContextHolder.getIdTokenClaims()
                .flatMap(claims -> {
                    try {
                        return Optional.ofNullable(claims.getStringListClaim("roles"));
                    } catch (ParseException e) {
                        return Optional.empty();
                    }
                })
                .orElse(emptyList())
                .contains("access_as_application");
    }

    public static boolean harAdminScope(AuthContextHolder authContextHolder) {
        List<String> scp = authContextHolder.getIdTokenClaims()
                .map(claims -> {
                    try {
                        return claims.getStringClaim("scp");
                    } catch (ParseException e) {
                        return "";
                    }
                })
                .map(scope -> scope.split(" "))
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList);
        return scp.contains("portefolje-admin");
    }

    public static String getContextAwareUserToken(
            DownstreamApi receivingApp,
            AuthContextHolder authContextHolder,
            AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient,
            EnvironmentProperties properties
    ) {
        final String azureAdIssuer = properties.getNaisAadIssuer();
        String token = authContextHolder.requireIdTokenString();

        String tokenIssuer = authContextHolder.getIdTokenClaims()
                .map(JWTClaimsSet::getIssuer)
                .orElseThrow();
        return azureAdIssuer.equals(tokenIssuer)
                ? getAadOboTokenForTjeneste(azureAdOnBehalfOfTokenClient, receivingApp)
                : token;
    }

    public static String getAadOboTokenForTjeneste(AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, DownstreamApi api) {
        String scope = "api://" + api.cluster() + "." + api.namespace() + "." + api.serviceName() + "/.default";
        return azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, getInnloggetBrukerToken());
    }
}
