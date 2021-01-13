package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

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
        if (kafkaAktivitet.getLagtInnAv() == KafkaAktivitetMelding.InnsenderData.BRUKER) {
            SisteEndringDTO sisteEndringDTO = new SisteEndringDTO(kafkaAktivitet);
            if (sisteEndringDTO.getKategori() != null && hendelseErNyereEnnIDatabase(sisteEndringDTO)) {
                try {
                    sisteEndringRepository.upsert(sisteEndringDTO);
                    elasticServiceV2.updateSisteEndring(sisteEndringDTO);
                } catch (Exception e) {
                    String message = String.format("Kunne ikke lagre eller indexere siste endring for aktivitetid %s", kafkaAktivitet.getAktivitetId());
                    log.error(message, e);
                }
            }
        }
    }

    public void slettSisteEndringer(AktoerId aktoerId) {
        sisteEndringRepository.slettSisteEndringer(aktoerId);
    }

    private boolean hendelseErNyereEnnIDatabase(SisteEndringDTO sisteEndringDTO) {
        if (sisteEndringDTO.getTidspunkt() == null) {
            log.warn("Endringstidspunkt var null for aktoerId: " + sisteEndringDTO.getAktoerId());
            return false;
        }
        Timestamp databaseVerdi = sisteEndringRepository.getSisteEndringTidspunkt(sisteEndringDTO.getAktoerId(), sisteEndringDTO.getKategori());
        if (databaseVerdi == null) {
            return true;
        }
        return toZonedDateTime(databaseVerdi).compareTo(sisteEndringDTO.getTidspunkt()) < 0;
    }

}
