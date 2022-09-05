package no.nav.pto.veilarbportefolje.auth;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static java.lang.String.format;
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
                .setHarFlereStatsborgerskap(false)
                .setBostedBydel(null)
                .setBostedKommune(null)
                .setHarUtelandsAddresse(false)
                .setBostedSistOppdatert(null);
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

    public static String getAadOboTokenForTjeneste(AzureAdOnBehalfOfTokenClient azureAdOnBehalfOfTokenClient, DownstreamApi api) {
        String scope = "api://" + api.cluster() + "." + api.namespace() + "." + api.serviceName() + "/.default";
        return azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, getInnloggetBrukerToken());
    }
}
