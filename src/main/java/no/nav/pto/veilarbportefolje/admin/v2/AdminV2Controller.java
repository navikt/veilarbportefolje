package no.nav.pto.veilarbportefolje.admin.v2;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.pto.veilarbportefolje.auth.DownstreamApi;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.pto.veilarbportefolje.auth.AuthUtils.erSystemkallFraAzureAd;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.hentApplikasjonFraContex;

@Slf4j
@RestController
@RequestMapping("/api/v2/admin")
@RequiredArgsConstructor
@Tag(name = "Admin V2", description = "Admin-funksjonalitet V2 som ikke er tilgjengelig for vanlige brukere. Funksjonaliteten er kun tilgjengelig for medlemmer av applikasjonens forvaltningsteam.")
public class AdminV2Controller {
    private final String PTO_ADMIN = new DownstreamApi(EnvironmentUtils.isProduction().orElse(false) ?
            "prod-fss" : "dev-fss", "pto", "pto-admin").toString();
    private final AktorClient aktorClient;
    private final OpensearchIndexer opensearchIndexer;
    private final AuthContextHolder authContextHolder;

    @Operation(summary = "Indekser bruker med fødselsnummer", description = "Hent og skriv oppdatert data for bruker, gitt ved fødselsnummer, til søkemotoren (OpenSearch).")
    @PutMapping("/indeks/bruker/fnr")
    public String indeks(@RequestBody AdminIndeksBrukerRequest adminIndeksBrukerRequest) {
        sjekkTilgangTilAdmin();
        String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(adminIndeksBrukerRequest.fnr().get())).get();
        opensearchIndexer.indekser(AktorId.of(aktorId));
        return "Indeksering fullfort";
    }

    @Operation(summary = "Indekser bruker med Aktør-ID", description = "Hent og skriv oppdatert data for bruker, gitt ved Aktør-ID, til søkemotoren (OpenSearch).")
    @PutMapping("/indeks/bruker")
    public String indeksAktoerId(@RequestBody AdminIndexAktorRequest adminIndexAktorRequest) {
        sjekkTilgangTilAdmin();
        opensearchIndexer.indekser(adminIndexAktorRequest.aktorId());
        return "Indeksering fullfort";
    }

    private void sjekkTilgangTilAdmin() {
        boolean erSystemBrukerFraAzure = erSystemkallFraAzureAd(authContextHolder);
        boolean erPtoAdmin = PTO_ADMIN.equals(hentApplikasjonFraContex(authContextHolder));

        if (erPtoAdmin && erSystemBrukerFraAzure) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
