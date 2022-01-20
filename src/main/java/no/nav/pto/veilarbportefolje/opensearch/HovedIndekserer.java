package no.nav.pto.veilarbportefolje.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HovedIndekserer {
    private final OpensearchIndexer opensearchIndexer;
    private final OpensearchAdminService opensearchAdminService;

    @SneakyThrows
    public void hovedIndeksering(List<AktorId> brukere) {
        long tidsStempel0 = System.currentTimeMillis();
        log.info("Hovedindeksering: Indekserer {} brukere", brukere.size());

        String gammelIndex = opensearchAdminService.hentBrukerIndex();
        String nyIndex = opensearchAdminService.opprettSkriveIndeksPaAlias();

        log.info("Hovedindeksering: skaper 'write index': {}", nyIndex);

        boolean success = tryIndekserAlleBrukere(brukere);

        if (success) {
            log.info("Hovedindeksering: Sletter gammel index {}", gammelIndex);
            boolean bleSlettet = opensearchAdminService.slettIndex(gammelIndex);
            if (bleSlettet) {
                opensearchAdminService.settAliasSettingsTilDefault(nyIndex);
            } else {
                log.error("gammel index ble ikke slettet!");
            }

            long tid = System.currentTimeMillis() - tidsStempel0;
            log.info("Hovedindeksering: Ferdig på {} ms, indekserte {} brukere", tid, brukere.size());
        } else {
            opensearchAdminService.slettIndex(nyIndex);
            log.error("Hovedindeksering: ble ikke fullført");
        }
    }


    private boolean tryIndekserAlleBrukere(List<AktorId> brukere) {
        try {
            return opensearchIndexer.indexerInParallel(brukere);
        } catch (Exception e) {
            log.error("Hovedindeksering: feilet...", e);
            return false;
        }
    }
}
