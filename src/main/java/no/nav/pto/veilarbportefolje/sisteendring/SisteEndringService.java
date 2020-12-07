package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategorier.ENDRET_AKTIVITET;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategorier.NY_AKTIVITET;
import static no.nav.pto.veilarbportefolje.util.DateUtils.dateToTimestamp;

@Slf4j
@Service
@Transactional
public class SisteEndringService {
    private final ElasticServiceV2 elasticServiceV2;
    private final SisteEndringRepository sisteEndringRepository;

    public SisteEndringService(ElasticServiceV2 elasticServiceV2, SisteEndringRepository sisteEndringRepository) {
        this.elasticServiceV2 = elasticServiceV2;
        this.sisteEndringRepository = sisteEndringRepository;
    }

    public void behandleAktivitet(KafkaAktivitetMelding kafkaAktivitet) {
        SisteEndringDTO objectSkrevetTilDatabase = lagreAktivitetData(kafkaAktivitet);

        if(objectSkrevetTilDatabase != null){
            elasticServiceV2.updateSisteEndring(objectSkrevetTilDatabase);
        }
    }

    private SisteEndringDTO lagreAktivitetData(KafkaAktivitetMelding aktivitet) {
        SisteEndringDTO objectSkrevetTilDatabase = null;
        Timestamp tidspunkt = aktivitet.getEndretDato() == null ? null : dateToTimestamp(aktivitet.getEndretDato());
        AktoerId aktoerId =  AktoerId.of(aktivitet.getAktorId());
        SisteEndringsKategorier kategorier = (tidspunkt == null) ? NY_AKTIVITET : ENDRET_AKTIVITET;

        if (tidspunkt == null || hendelseErNyereEnnIDatabase(tidspunkt, aktoerId)) {
            tidspunkt = (tidspunkt == null) ? Timestamp.from(now()) : tidspunkt; // TODO: Antar at nye aktivterer (null verdier) er skapt "nå".
            try {
                objectSkrevetTilDatabase = new SisteEndringDTO()
                        .setAktoerId(aktoerId)
                        .setKategori(kategorier)
                        .setTidspunkt(tidspunkt);
                sisteEndringRepository.upsert(objectSkrevetTilDatabase);
            } catch (Exception e) {
                String message = String.format("Kunne ikke lagre siste endring for aktivitetid %s", aktivitet.getAktivitetId());
                log.error(message, e);
                 objectSkrevetTilDatabase = null;
            }
        }
        return objectSkrevetTilDatabase;
    }

    private boolean hendelseErNyereEnnIDatabase(Timestamp endringstidspunkt, AktoerId aktoerId) {
        Timestamp databaseVerdi = sisteEndringRepository.getSisteEndringTidspunkt(aktoerId);
        return databaseVerdi.compareTo(endringstidspunkt) < 0;
    }
}
