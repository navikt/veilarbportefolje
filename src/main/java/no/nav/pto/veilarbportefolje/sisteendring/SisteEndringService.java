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
        SisteEndringsKategori kategorier = getKategoriFromKafkaMessage(aktivitet);

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

    private boolean hendelseErNyereEnnIDatabase(ZonedDateTime endringstidspunkt, SisteEndringsKategori kategorier, AktoerId aktoerId) {
        if (endringstidspunkt == null) {
            return true;
        }
        Timestamp databaseVerdi = sisteEndringRepository.getSisteEndringTidspunkt(aktoerId, kategorier);
        if (databaseVerdi == null) {
            return true;
        }
        return toZonedDateTime(databaseVerdi).compareTo(endringstidspunkt) < 0;
    }

    private SisteEndringsKategori getKategoriFromKafkaMessage(KafkaAktivitetMelding aktivitet) {
        String potensiellSisteEndringsKategori;
        if (aktivitet.getEndretDato() == null) {
            potensiellSisteEndringsKategori = "NY_"+aktivitet.getAktivitetType();
        } else {
            potensiellSisteEndringsKategori = aktivitet.getAktivitetStatus() + "_" + aktivitet.getAktivitetType();
        }

        if(SisteEndringsKategori.contains(potensiellSisteEndringsKategori)){
            return SisteEndringsKategori.valueOf(potensiellSisteEndringsKategori);
        }
        return null;
    }

}
