package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerServiceV2;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PdlPersonValideringException;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingStartetService {
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;
    private final PdlService pdlService;
    private final Siste14aVedtakService siste14aVedtakService;
    private final OppfolgingsbrukerServiceV2 oppfolgingsbrukerServiceV2;
    private final ArbeidssoekerService arbeidssoekerService;

    // TODO: Dersom en eller flere av disse operasjonene feiler og kaster exception vil
    //  kafka-meldingen bli lagret og retryet. Dette kan resultere i at vi mellomlagrer data
    //  vi ikke skal. Mulig vi burde wrappe dette i en try-catch slik at vi kan rydde opp
    //  i catch-en.
    //  Dette vil også muligens bidra til å gjøre usikkerheten rundt hvordan vi håndterer
    //  scenarier der AktorID/Fnr blir historisk før startOppfolging fullfører.
    public void startOppfolging(AktorId aktorId, ZonedDateTime oppfolgingStartetDate) {
        try {
            pdlService.hentOgLagrePdlData(aktorId);
        } catch (PdlPersonValideringException e) {
            if (isDevelopment().orElse(false)) {
                secureLog.info(String.format("Ignorerer dårlig datakvalitet i dev, bruker: %s", aktorId), e);
                return;
            }
            throw e;
        }

        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartetDate);

        siste14aVedtakService.hentOgLagreSiste14aVedtak(aktorId);
        oppfolgingsbrukerServiceV2.hentOgLagreOppfolgingsbruker(aktorId);
        arbeidssoekerService.hentOgLagreArbeidssoekerdataForBruker(aktorId);

        opensearchIndexer.indekser(aktorId);
        secureLog.info("Bruker {} har startet oppfølging: {}", aktorId, oppfolgingStartetDate);
    }
}
