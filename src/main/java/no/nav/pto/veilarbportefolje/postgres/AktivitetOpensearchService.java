package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktiviteterRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV3;
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

    public Map<AktorId, List<AktivitetEntityDto>> hentAvtaltAktivitetData(List<AktorId> brukere) {
        String aktoerIder = brukere.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        HashMap<AktorId, List<AktivitetEntityDto>> result = new HashMap<>(brukere.size());

        aktiviteterRepositoryV2.leggTilAktiviteterFraAktivitetsplanen(aktoerIder, true, result);
        gruppeAktivitetRepositoryV2.leggTilGruppeAktiviteter(aktoerIder, result);
        tiltakRepositoryV3.leggTilTiltak(aktoerIder, result);

        return result;
    }

    public Map<AktorId, List<AktivitetEntityDto>> hentIkkeAvtaltAktivitetData(List<AktorId> brukere) {
        String aktoerIder = brukere.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        HashMap<AktorId, List<AktivitetEntityDto>> result = new HashMap<>(brukere.size());

        aktiviteterRepositoryV2.leggTilAktiviteterFraAktivitetsplanen(aktoerIder, false, result);
        return result;
    }
}
