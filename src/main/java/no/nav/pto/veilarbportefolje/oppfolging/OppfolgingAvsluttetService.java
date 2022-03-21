package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.cv.CVRepositoryV2;
import no.nav.pto.veilarbportefolje.cv.CvRepository;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingAvsluttetService {
    private final ArbeidslisteService arbeidslisteService;
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final RegistreringService registreringService;
    private final CvRepository cvRepository;
    private final CVRepositoryV2 cvRepositoryV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final SisteEndringService sisteEndringService;

    public void avsluttOppfolging(AktorId aktoerId) {
        oppfolgingRepository.slettOppfolgingData(aktoerId);
        oppfolgingRepositoryV2.slettOppfolgingData(aktoerId);
        registreringService.slettRegistering(aktoerId);
        arbeidslisteService.slettArbeidsliste(aktoerId);
        sisteEndringService.slettSisteEndringer(aktoerId);
        cvRepository.resetHarDeltCV(aktoerId);
        cvRepositoryV2.resetHarDeltCV(aktoerId);

        opensearchIndexerV2.slettDokumenter(List.of(aktoerId));
        log.info("Bruker: {} har avsluttet oppfølging og er slettet", aktoerId);
    }
}
