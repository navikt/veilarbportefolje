package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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

    @Autowired
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

        ZonedDateTime tidspunkt = aktivitet.getEndretDato() == null ? null : aktivitet.getEndretDato();
        SisteEndringsKategorier kategorier = (tidspunkt == null) ? NY_AKTIVITET : ENDRET_AKTIVITET;
        AktoerId aktoerId = AktoerId.of(aktivitet.getAktorId());

        if (tidspunkt == null || hendelseErNyereEnnIDatabase(tidspunkt, kategorier, aktoerId)) {
            tidspunkt = (tidspunkt == null) ? ZonedDateTime.now() : tidspunkt; // TODO: Antar at nye aktivterer (null verdier) er skapt "nå".

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

    private boolean hendelseErNyereEnnIDatabase(ZonedDateTime endringstidspunkt, SisteEndringsKategorier kategorier, AktoerId aktoerId) {
        Timestamp databaseVerdi = sisteEndringRepository.getSisteEndringTidspunkt(aktoerId, kategorier);
        if(databaseVerdi == null){
            return true;
        }
        return databaseVerdi.toInstant().atZone(ZoneId.of("Europe/Oslo")).compareTo(endringstidspunkt) < 0;
    }
}
