package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.cv.CVRepositoryV2;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingAvsluttetService {
    private final ArbeidslisteService arbeidslisteService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final RegistreringService registreringService;
    private final CVRepositoryV2 cvRepositoryV2;
    private final PdlService pdlService;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final SisteEndringService sisteEndringService;
    private final Siste14aVedtakService siste14aVedtakService;

    public void avsluttOppfolging(AktorId aktoerId) {
        oppfolgingRepositoryV2.slettOppfolgingData(aktoerId);
        registreringService.slettRegistering(aktoerId);
        arbeidslisteService.slettArbeidsliste(aktoerId);
        sisteEndringService.slettSisteEndringer(aktoerId);
        cvRepositoryV2.resetHarDeltCV(aktoerId);
        siste14aVedtakService.slettSiste14aVedtak(aktoerId.get());
        pdlService.slettPdlData(aktoerId);

        opensearchIndexerV2.slettDokumenter(List.of(aktoerId));
        secureLog.info("Bruker: {} har avsluttet oppfølging og er slettet", aktoerId);
    }
}
