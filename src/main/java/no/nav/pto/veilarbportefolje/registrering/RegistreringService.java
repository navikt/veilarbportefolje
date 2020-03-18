package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RegistreringService {
    private RegistreringRepository registreringRepository;

    public RegistreringService(RegistreringRepository registreringRepository) {
        this.registreringRepository = registreringRepository;
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        ArbeidssokerRegistrertEvent hentBrukerRegistrering = registreringRepository.hentBrukerRegistrering(AktoerId.of(kafkaRegistreringMelding.getAktorid()));

        if(hentBrukerRegistrering != null) {
            if (erNyereRegistering(hentBrukerRegistrering, kafkaRegistreringMelding)) {
                registreringRepository.uppdaterBrukerRegistring(kafkaRegistreringMelding);
            }
            return;
        }
        registreringRepository.insertBrukerRegistrering(kafkaRegistreringMelding);

    }

    private boolean erNyereRegistering(ArbeidssokerRegistrertEvent gjeldendeBrukerRegistrering, ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        ZonedDateTime registreringOpprettetDato = LocalDateTime.parse(gjeldendeBrukerRegistrering.getRegistreringOpprettet(), DateTimeFormatter.ISO_ZONED_DATE_TIME).atZone(ZoneId.systemDefault());
        ZonedDateTime registeringsOpprettetDatoDatoFraKafka = LocalDateTime.parse(kafkaRegistreringMelding.getRegistreringOpprettet(), DateTimeFormatter.ISO_ZONED_DATE_TIME).atZone(ZoneId.systemDefault());
        return registeringsOpprettetDatoDatoFraKafka.isAfter(registreringOpprettetDato);
    }

}
