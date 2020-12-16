package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding.AktivitetStatus.AVBRUTT;
import static no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding.AktivitetStatus.FULLFORT;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategorier.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

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

        if (objectSkrevetTilDatabase != null) {
            elasticServiceV2.updateSisteEndring(objectSkrevetTilDatabase);
        }
    }

    public void slettSisteEndringer(AktoerId aktoerId) {
        sisteEndringRepository.slettSisteEndringer(aktoerId);
    }

    private SisteEndringDTO lagreAktivitetData(KafkaAktivitetMelding aktivitet) {
        SisteEndringDTO objectSkrevetTilDatabase = null;

        AktoerId aktoerId = AktoerId.of(aktivitet.getAktorId());
        ZonedDateTime tidspunkt = aktivitet.getEndretDato();
        SisteEndringsKategorier kategorier = getKategoriFromKafkaMessage(aktivitet);

        if (kategorier != null && hendelseErNyereEnnIDatabase(tidspunkt, kategorier, aktoerId)) {
            tidspunkt = (tidspunkt == null) ? ZonedDateTime.now() : tidspunkt;

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
        if (endringstidspunkt == null) {
            return true;
        }
        Timestamp databaseVerdi = sisteEndringRepository.getSisteEndringTidspunkt(aktoerId, kategorier);
        if (databaseVerdi == null) {
            return true;
        }
        return toZonedDateTime(databaseVerdi).compareTo(endringstidspunkt) < 0;
    }

    private SisteEndringsKategorier getKategoriFromKafkaMessage(KafkaAktivitetMelding aktivitet) {
        if (aktivitet.getEndretDato() == null) {
            switch (aktivitet.getAktivitetType()) {
                case STILLING:
                    return NY_STILLING;
                case IJOBB:
                    return NY_IJOBB;
                case EGEN:
                    return NY_EGEN;
                case BEHANDLING:
                    return NY_BEHANDLING;
            }
        } else if (aktivitet.getAktivitetStatus().equals(FULLFORT)) {
            switch (aktivitet.getAktivitetType()) {
                case STILLING:
                    return FULLFORT_STILLING;
                case IJOBB:
                    return FULLFORT_IJOBB;
                case EGEN:
                    return FULLFORT_EGEN;
                case BEHANDLING:
                    return FULLFORT_BEHANDLING;
                case SOKEAVTALE:
                    return FULLFORT_SOKEAVTALE;
            }
        } else if (aktivitet.getAktivitetStatus().equals(AVBRUTT)) {
            switch (aktivitet.getAktivitetType()) {
                case STILLING:
                    return AVBRUTT_STILLING;
                case IJOBB:
                    return AVBRUTT_IJOBB;
                case EGEN:
                    return AVBRUTT_EGEN;
                case BEHANDLING:
                    return AVBRUTT_BEHANDLING;
                case SOKEAVTALE:
                    return AVBRUTT_SOKEAVTALE;
            }
        }
        return null;
    }

}
