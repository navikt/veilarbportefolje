package no.nav.fo.veilarbportefolje.krr;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.CollectionUtils;
import no.nav.sbl.rest.RestUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.fo.veilarbportefolje.util.SubjectUtils.getOidcToken;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class KrrService {

    public static final String DKIF_URL_PROPERTY_NAME = "DKIF_URL";
    static final int BULK_SIZE = 500;
    static final String DKIF_URL_PATH = "/api/v1/personer/kontaktinformasjon";

    private KrrRepository krrRepository;

    @Inject
    public KrrService(KrrRepository krrRepository) {
        this.krrRepository = krrRepository;
    }

    public void oppdaterDigitialKontaktinformasjon() {
        log.info("Indeksering: Starter henting av KRR informasjon...");

        krrRepository.slettKrrInformasjon();
        List<String> fodselsnummere = krrRepository.hentAlleFnrUnderOppfolging();

        oppdaterKrrInfo(fodselsnummere);

        log.info("Indeksering: Fullført henting av KRR informasjon");
    }

    public void oppdaterKrrInfo(List<String> fodselsnummere) {
        CollectionUtils
                .partition(fodselsnummere, BULK_SIZE)
                .forEach(this::oppdaterKrrKontaktinfo);
    }

    private void oppdaterKrrKontaktinfo(List<String> fodselsnummere) {
        log.info("Oppdaterer KRR for {} brukere", fodselsnummere.size());

        KrrDTO response = hentKrrKontaktInfo(fodselsnummere);
        List<KrrKontaktInfoDTO> kontaktinfo = new ArrayList<>(response.getKontaktinfo().values());
        krrRepository.lagreKrrKontaktInfo(kontaktinfo);
    }

    public static KrrDTO hentKrrKontaktInfo(List<String> fodselsnummere) {
        return RestUtils.withClient(client ->
                client.target(getRequiredProperty(DKIF_URL_PROPERTY_NAME))
                        .path(DKIF_URL_PATH)
                        .queryParam("inkluderSikkerDigitalPost", false)
                        .request()
                        .header(AUTHORIZATION, "Bearer " + getOidcToken())
                        .header("Nav-Personidenter", fodselsnummere)
                        .get(KrrDTO.class)
        );
    }
}
