package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.elastic.domene.Endring;
import no.nav.pto.veilarbportefolje.mal.MalEndringKafkaDTO;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SisteEndringService {
    private final ElasticServiceV2 elasticServiceV2;
    private final SisteEndringRepository sisteEndringRepository;

    public void veilederHarSett(AktorId aktorId, ZonedDateTime time){
        LocalDateTime veilederharsett = time.toLocalDateTime();
        Map<String, Endring> sisteEndringer = sisteEndringRepository.getSisteEndringer(aktorId);
        sisteEndringer.forEach((kategori, endring) -> {
            if(endring.getEr_sett().equals("J")){
                return;
            }
            if(veilederharsett.isAfter(DateUtils.toLocalDateTimeOrNull(endring.getTidspunkt()))){
                sisteEndringRepository.oppdaterHarSett(aktorId, SisteEndringsKategori.valueOf(kategori),true);
                elasticServiceV2.updateSisteEndring(aktorId, SisteEndringsKategori.valueOf(kategori));
            }
        });
    }

    public void behandleMal(MalEndringKafkaDTO melding) {
        if (melding.getLagtInnAv() == null || melding.getLagtInnAv() == MalEndringKafkaDTO.InnsenderData.NAV) {
            return;
        }
        SisteEndringDTO sisteEndringDTO = new SisteEndringDTO(melding);
        if(hendelseErNyereEnnIDatabase(sisteEndringDTO)) {
            try {
                sisteEndringRepository.upsert(sisteEndringDTO);
                elasticServiceV2.updateSisteEndring(sisteEndringDTO);
            } catch (Exception e) {
                String message = String.format("Kunne ikke lagre eller indexere siste endring for aktoer id: %s", melding.getAktorId());
                log.error(message, e);
            }

        }
    }

    public void behandleAktivitet(KafkaAktivitetMelding kafkaAktivitet) {
        if (kafkaAktivitet.getLagtInnAv() == null || kafkaAktivitet.getLagtInnAv() == KafkaAktivitetMelding.InnsenderData.NAV) {
            return;
        }

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

    public void slettSisteEndringer(AktorId aktoerId) {
        sisteEndringRepository.slettSisteEndringer(aktoerId);
    }

    private boolean hendelseErNyereEnnIDatabase(SisteEndringDTO sisteEndringDTO) {
        if (sisteEndringDTO.getTidspunkt() == null) {
            log.error("Endringstidspunkt var null for aktoerId: " + sisteEndringDTO.getAktoerId());
            return false;
        }
        Timestamp databaseVerdi = sisteEndringRepository.getSisteEndringTidspunkt(sisteEndringDTO.getAktoerId(), sisteEndringDTO.getKategori());
        if (databaseVerdi == null) {
            return true;
        }
        return toZonedDateTime(databaseVerdi).compareTo(sisteEndringDTO.getTidspunkt()) < 0;
    }
}
