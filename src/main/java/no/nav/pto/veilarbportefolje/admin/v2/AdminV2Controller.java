package no.nav.pto.veilarbportefolje.admin.v2;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.pto.veilarbportefolje.auth.AuthUtils.erSystemkallFraAzureAd;
import static no.nav.pto.veilarbportefolje.auth.AuthUtils.hentApplikasjonFraContex;

@Slf4j
@RestController
@RequestMapping("/api/v2/admin")
@RequiredArgsConstructor
public class AdminV2Controller {
	private final String PTO_ADMIN = new DownstreamApi(EnvironmentUtils.isProduction().orElse(false) ?
			"prod-fss" : "dev-fss", "pto", "pto-admin").toString();
	private final AktorClient aktorClient;
	private final OpensearchIndexer opensearchIndexer;
	private final AuthContextHolder authContextHolder;

	@PutMapping("/indeks/bruker/fnr")
	public String indeks(@RequestBody AdminIndeksBrukerRequest adminIndeksBrukerRequest) {
		sjekkTilgangTilAdmin();
		String aktorId = aktorClient.hentAktorId(Fnr.ofValidFnr(adminIndeksBrukerRequest.fnr().get())).get();
		opensearchIndexer.indekser(AktorId.of(aktorId));
		return "Indeksering fullfort";
	}

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
