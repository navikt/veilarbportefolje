package no.nav.pto.veilarbportefolje.postgres;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktiviteterRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV3;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AktivitetOpensearchService {
    private final TiltakRepositoryV3 tiltakRepositoryV3;
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final GruppeAktivitetRepositoryV2 gruppeAktivitetRepositoryV2;
    private final DefaultUnleash defaultUnleash;

    public Map<AktorId, List<AktivitetEntityDto>> hentAvtaltAktivitetData(List<AktorId> brukere) {
        String aktoerIder = brukere.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        HashMap<AktorId, List<AktivitetEntityDto>> result = new HashMap<>(brukere.size());

        boolean bruktTiltaksAktivitetFraAktivitetsplan = FeatureToggle.brukTiltaksaktivitetFraAktivitetsplan(defaultUnleash);
        /*
        TODO : Erstatt aktiviteter tabellen med den nye kafka_aktivitet_melding tabellen og legg til aktiviteter i resultatet derfra.
               Fjern leggTilTiltaksAktivitet() og leggTilTiltak(), siden den nye tabellen inneholder alle typer aktiviteter inkludert tiltaksaktiviteter.
        */
        aktiviteterRepositoryV2.leggTilAktiviteterFraAktivitetsplanen(aktoerIder, true, result);
        gruppeAktivitetRepositoryV2.leggTilGruppeAktiviteter(aktoerIder, result);

        if (bruktTiltaksAktivitetFraAktivitetsplan) {
            tiltakRepositoryV3.leggTilTiltaksAktivitet(aktoerIder, true, result);
            log.debug("leggTilTiltaksAktivitet på resultat for indeksering. Antall tiltaksaktiviteter: {}", result.size());
        } else {
            tiltakRepositoryV3.leggTilTiltak(aktoerIder, result);
            log.debug("leggTilTiltak på resultat for indeksering. Antall: {}", result.size());
        }

        return result;
    }

    public Map<AktorId, List<AktivitetEntityDto>> hentIkkeAvtaltAktivitetData(List<AktorId> brukere) {
        String aktoerIder = brukere.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        HashMap<AktorId, List<AktivitetEntityDto>> result = new HashMap<>(brukere.size());

        aktiviteterRepositoryV2.leggTilAktiviteterFraAktivitetsplanen(aktoerIder, false, result);
        return result;
    }
}
