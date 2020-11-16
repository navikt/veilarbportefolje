package no.nav.pto.veilarbportefolje.registrering;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class RegistreringService implements KafkaConsumerService<ArbeidssokerRegistrertEvent> {
    private final RegistreringRepository registreringRepository;
    private final ElasticServiceV2 elastic;
    private final BrukerRepository brukerRepository;

    @Autowired
    public RegistreringService(RegistreringRepository registreringRepository, ElasticServiceV2 elastic, BrukerRepository brukerRepository) {
        this.registreringRepository = registreringRepository;
        this.elastic = elastic;
        this.brukerRepository = brukerRepository;
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        registreringRepository.upsertBrukerRegistrering(kafkaRegistreringMelding);

        Optional<String> fnr = brukerRepository.hentFnrView(AktoerId.of(kafkaRegistreringMelding.getAktorid()));
        if(fnr.isPresent()){
            elastic.updateRegistering(Fnr.of(fnr.get()), kafkaRegistreringMelding);
        }else{
            log.error("Fant ikke fnr på aktør id: " + kafkaRegistreringMelding.getAktorid());
        }
    }


    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
