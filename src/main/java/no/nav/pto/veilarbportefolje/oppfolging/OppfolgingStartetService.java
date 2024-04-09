package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerServiceV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

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

        // TODO: Dersom en eller flere av disse operasjonene feiler og kaster exception vil
        //  kafka-meldingen bli lagret og retryet. Dette kan resultere i at vi mellomlagrer data
        //  vi ikke skal. Mulig vi burde wrappe dette i en try-catch slik at vi kan rydde opp
        //  i catch-en.
    public void startOppfolging(AktorId aktorId, ZonedDateTime oppfolgingStartetDate) {
        pdlService.hentOgLagrePdlData(aktorId);

        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartetDate);

        siste14aVedtakService.hentOgLagreSiste14aVedtak(aktorId);

        // TODO: Håndtere identer; hva hvis aktorId er historisk?
        //  Dette er et litt søkt scenario, men teoretisk sett kan en ident (her gitt ved aktorId-argumentet)
        //  bli satt som historisk før linjen under blir utført (fordi di vi lever i en multi-trådet verden osv.)
        //  Dersom man skal hesynta dette kan det være interessant å få litt innsikt i hvordan dette skjer, og
        //  eventuelt hvor vanlig det er med historiske Aktør-IDer
        //  Dersom det viser seg at Aktør-IDer aldri blir historiske, kan dette ignoreres.
        oppfolgingsbrukerServiceV2.hentOgLagreOppfolgingsbruker(aktorId);

        opensearchIndexer.indekser(aktorId);
        secureLog.info("Bruker {} har startet oppfølging: {}", aktorId, oppfolgingStartetDate);
    }

}
