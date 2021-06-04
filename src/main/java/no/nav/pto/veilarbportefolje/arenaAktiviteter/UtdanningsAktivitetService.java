package no.nav.pto.veilarbportefolje.arenaAktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.*;
import no.nav.pto.veilarbportefolje.arenaAktiviteter.arenaDTO.GoldenGateDTO;
import no.nav.pto.veilarbportefolje.arenaAktiviteter.arenaDTO.GoldenGateOperations;
import no.nav.pto.veilarbportefolje.arenaAktiviteter.arenaDTO.UtdanningsAktivitetInnhold;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtdanningsAktivitetService {
    private final AktivitetDAO aktivitetDAO;
    private final ArenaAktivitetService arenaAktivitetService;

    public void behandleKafkaMelding(GoldenGateDTO kafkaMelding) {
        log.info("Behandler utdannings-aktivtet-melding");
        UtdanningsAktivitetInnhold innhold = (UtdanningsAktivitetInnhold) arenaAktivitetService.getInnhold(kafkaMelding);
        if (innhold == null){
            return;
        }

        if(arenaAktivitetService.erGammelMelding(innhold.getAktivitetid(), innhold.getHendelseId())){
            log.info("Fikk tilsendt gammel utdannings-aktivtet-melding");
            return;
        }

        boolean skalSlettes = skalSlettes(kafkaMelding);
        if(skalSlettes){
            aktivitetDAO.deleteById(innhold.getAktivitetid());
        }else{
            AktorId aktorId = arenaAktivitetService.getAktorId(innhold.getFnr());
            KafkaAktivitetMelding melding = arenaAktivitetService.mapTilKafkaAktivitetMelding(innhold, aktorId);
            aktivitetDAO.upsertAktivitet(melding);
        }
    }

    private boolean skalSlettes(GoldenGateDTO kafkaMelding) {
        return GoldenGateOperations.DELETE.equals(kafkaMelding.getOperationType());
    }
}
