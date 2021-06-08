package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.ArenaInnholdKafka;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GoldenGateDTO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.GoldenGateOperations;
import no.nav.pto.veilarbportefolje.domene.AktorClient;

public abstract class ArenaAktivitetService {
    public ArenaInnholdKafka getInnhold(GoldenGateDTO goldenGateDTO) {
        switch (goldenGateDTO.getOperationType()) {
            case GoldenGateOperations.DELETE:
                return goldenGateDTO.getBefore();
            case GoldenGateOperations.INSERT:
            case GoldenGateOperations.UPDATE:
                return goldenGateDTO.getAfter();
            default:
                throw new IllegalArgumentException("Ukjent GoldenGate opperasjon");
        }
    }

    public boolean skalSlettes(GoldenGateDTO kafkaMelding) {
        return GoldenGateOperations.DELETE.equals(kafkaMelding.getOperationType());
    }

    public boolean erGammelMelding(String id, long hendelseId) {
        return false; // TODO: finn en logikk som fungerer
    }

    public AktorId getAktorId(AktorClient aktorClient, String personident) {
        return aktorClient.hentAktorId(Fnr.ofValidFnr(personident));
    }
}
