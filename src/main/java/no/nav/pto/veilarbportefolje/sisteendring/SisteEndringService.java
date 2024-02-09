package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.interfaces.HandtereOppfolgingData;
import no.nav.pto.veilarbportefolje.mal.MalEndringKafkaDTO;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SisteEndringService implements HandtereOppfolgingData<AktorId> {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final SisteEndringRepositoryV2 sisteEndringRepositoryV2;

    public void veilederHarSett(AktorId aktorId, ZonedDateTime time) {
        LocalDateTime veilederharsett = time.toLocalDateTime();
        Map<String, Endring> sisteEndringer = sisteEndringRepositoryV2.getSisteEndringer(aktorId);
        sisteEndringer.forEach((kategori, endring) -> {
            if (endring.getEr_sett().equals("J")) {
                return;
            }
            if (veilederharsett.isAfter(DateUtils.toLocalDateTimeOrNull(endring.getTidspunkt()))) {
                sisteEndringRepositoryV2.oppdaterHarSett(aktorId, SisteEndringsKategori.valueOf(kategori), true);
                opensearchIndexerV2.updateSisteEndring(aktorId, SisteEndringsKategori.valueOf(kategori));
            }
        });
    }

    public void behandleMal(MalEndringKafkaDTO melding) {
        if (melding.getLagtInnAv() == null || melding.getLagtInnAv() == MalEndringKafkaDTO.InnsenderData.NAV) {
            return;
        }
        SisteEndringDTO sisteEndringDTO = new SisteEndringDTO(melding);
        if (hendelseErNyereEnnIPostgres(sisteEndringDTO)) {
            try {
                sisteEndringRepositoryV2.upsert(sisteEndringDTO);
                opensearchIndexerV2.updateSisteEndring(sisteEndringDTO);
            } catch (Exception e) {
                String message = String.format("Kunne ikke lagre eller indexere siste endring for aktoer id: %s", melding.getAktorId());
                secureLog.error(message, e);
            }

        }
    }

    public void behandleAktivitet(KafkaAktivitetMelding kafkaAktivitet) {
        if (kafkaAktivitet.getLagtInnAv() == null || kafkaAktivitet.getLagtInnAv() != KafkaAktivitetMelding.InnsenderData.BRUKER) {
            return;
        }

        SisteEndringDTO sisteEndringDTO = new SisteEndringDTO(kafkaAktivitet);
        if (sisteEndringDTO.getKategori() != null && hendelseErNyereEnnIPostgres(sisteEndringDTO)) {
            sisteEndringRepositoryV2.upsert(sisteEndringDTO);
        }
    }

    public void slettOppfolgingData(AktorId aktoerId) {
        sisteEndringRepositoryV2.slettSisteEndringer(aktoerId);
    }

    private boolean hendelseErNyereEnnIPostgres(SisteEndringDTO sisteEndringDTO) {
        if (sisteEndringDTO.getTidspunkt() == null) {
            secureLog.error("Endringstidspunkt var null for aktoerId: " + sisteEndringDTO.getAktoerId());
            return false;
        }
        Timestamp databaseVerdi = sisteEndringRepositoryV2.getSisteEndringTidspunkt(sisteEndringDTO.getAktoerId(), sisteEndringDTO.getKategori());
        if (databaseVerdi == null) {
            return true;
        }
        return toZonedDateTime(databaseVerdi).compareTo(sisteEndringDTO.getTidspunkt()) < 0;
    }

    public Map<AktorId, Map<String, Endring>> hentSisteEndringerFraPostgres(List<AktorId> aktorerId) {
        return sisteEndringRepositoryV2.getSisteEndringer(aktorerId);
    }
}
