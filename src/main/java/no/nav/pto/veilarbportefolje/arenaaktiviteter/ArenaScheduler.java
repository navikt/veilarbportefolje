package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.ArenaAktivitetDTO;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Slf4j
@EnableScheduling
@RequiredArgsConstructor
public class ArenaScheduler {
    private final AktivitetService aktivitetService;

    @Scheduled(cron = "0 1 0 * * ?")
    private void slettUtgatteUtdanningAktivteter() {
        List<ArenaAktivitetDTO> utgatteUtdanningAktiviteter = aktivitetService.hentUtgatteUtdanningAktiviteter();
        log.info("Sletter: {} utgatte utdanningaktiviteter",utgatteUtdanningAktiviteter.size());
        utgatteUtdanningAktiviteter.forEach(utgattAktivitet -> aktivitetService.slettUtgatteAktivitet(utgattAktivitet.getAktivitetId(), AktorId.of(utgattAktivitet.getAktoerid())));
    }

    private void slettGruppeAktiviteter() {
        //var utgatteGruppeAktiviteter = aktivitetService.hentUtgatteGruppeAktiviteter();
        log.info("Sletter {} utgatte gruppe aktivteter");
    }
}
