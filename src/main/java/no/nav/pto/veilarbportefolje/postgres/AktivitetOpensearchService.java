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

        boolean hentTiltaksAktiviteterFraAktivitetsplan = FeatureToggle.brukTiltaksaktivitetFraAktivitetsplan(defaultUnleash);
        /*
        TODO : I første omgang henter vi kun tiltaksaktiviteter fra den nye tabellen (kafka_aktivitet_melding). Alle øvrige aktiviteter hentes fortsatt fra den gamle tabellen (aktiviteter).
               Planen er å hente alle aktiviteter fra den nye tabellen når vi har fått inn samtlige aktivitetstyper der, inkludert aktivitetsplanaktiviteter, tiltaksaktiviteter, gruppeaktiviteter og utdanningsaktiviteter.
               Når den nye tabellen erstatter den gamle som primær kilde for aktiviteter, trenger vi ikke lenger å hente tiltaksaktiviteter og øvrige aktiviteter fra ulike datakilder.
               På det tidspunktet kan vi fjerne leggTilTiltaksAktiviteter() og leggTilTiltak() og ha en felles funksjon for å hente alle aktiviteter, siden den nye tabellen vil inneholde alle aktivitetstyper, inkludert tiltaksaktiviteter.
         */
        aktiviteterRepositoryV2.leggTilAktiviteterFraAktivitetsplanen(aktoerIder, true, result);
        gruppeAktivitetRepositoryV2.leggTilGruppeAktiviteter(aktoerIder, result);

        if (hentTiltaksAktiviteterFraAktivitetsplan) {
            tiltakRepositoryV3.leggTilTiltaksAktivitet(aktoerIder, true, result);
            log.info("leggTilTiltaksAktivitet på resultat for indeksering. Antall tiltaksaktiviteter: {}, aktorIder: {}", result.size(), aktoerIder);
        } else {
            tiltakRepositoryV3.leggTilTiltak(aktoerIder, result);
            log.info("leggTilTiltak på resultat for indeksering. Antall: {}, aktorIder: {}", result.size(), aktoerIder);
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
