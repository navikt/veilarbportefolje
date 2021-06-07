package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.*;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GoldenGateDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GoldenGateOperations;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.UtdanningsAktivitetInnhold;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtdanningsAktivitetService {
    private final AktivitetService aktivitetService;
    private final ArenaAktivitetService arenaAktivitetService;

    public void behandleKafkaMelding(GoldenGateDTO<UtdanningsAktivitetInnhold> kafkaMelding) {
        log.info("Behandler utdannings-aktivtet-melding");
        UtdanningsAktivitetInnhold innhold = arenaAktivitetService.getInnhold(kafkaMelding);
        if (innhold == null){
            return;
        }

        if(arenaAktivitetService.erGammelMelding(innhold.getAktivitetid(), innhold.getHendelseId())){
            log.info("Fikk tilsendt gammel utdannings-aktivtet-melding");
            return;
        }

        AktorId aktorId = arenaAktivitetService.getAktorId(innhold.getFnr());
        if(skalSlettes(kafkaMelding)){
            aktivitetService.slettAktivitet(innhold.getAktivitetid(), aktorId);
        }else{
            KafkaAktivitetMelding melding = arenaAktivitetService.mapTilKafkaAktivitetMelding(innhold, aktorId);
            aktivitetService.upsertOgIndekserAktiviteter(melding);
        }
    }

    private boolean skalSlettes(GoldenGateDTO<UtdanningsAktivitetInnhold> kafkaMelding) {
        return GoldenGateOperations.DELETE.equals(kafkaMelding.getOperationType());
    }
}
