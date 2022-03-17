package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelseDAO;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesStatusRepositoryV2;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.YtelseMapping;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerDataService {
    private final BrukerDataRepository brukerDataRepository;
    private final YtelsesStatusRepositoryV2 ytelsesStatusRepositoryV2;

    public void oppdaterYtelserOracle(AktorId aktorId, PersonId personId, Optional<YtelseDAO> innhold) {
        Brukerdata ytelsesTilstand = new Brukerdata()
                .setAktoerid(aktorId.get())
                .setPersonid(personId.getValue());
        if (innhold.isEmpty()) {
            brukerDataRepository.upsertYtelser(ytelsesTilstand);
            return;
        }

        switch (innhold.get().getType()) {
            case DAGPENGER -> {
                leggTilYtelsesData(ytelsesTilstand, innhold.get());
                leggTilRelevantDagpengeData(ytelsesTilstand, innhold.get());
            }
            case AAP -> {
                leggTilYtelsesData(ytelsesTilstand, innhold.get());
                leggTilRelevantAAPData(ytelsesTilstand, innhold.get());
            }
            case TILTAKSPENGER -> leggTilYtelsesData(ytelsesTilstand, innhold.get());
        }

        brukerDataRepository.upsertYtelser(ytelsesTilstand);
    }

    public void oppdaterYtelserPostgres(AktorId aktorId, Optional<YtelseDAO> innhold) {
        Brukerdata ytelsesTilstand = new Brukerdata()
                .setAktoerid(aktorId.get());
        if (innhold.isEmpty()) {
            ytelsesStatusRepositoryV2.upsertYtelse(ytelsesTilstand);
            return;
        }

        switch (innhold.get().getType()) {
            case DAGPENGER -> {
                leggTilYtelsesData(ytelsesTilstand, innhold.get());
                leggTilRelevantDagpengeData(ytelsesTilstand, innhold.get());
            }
            case AAP -> {
                leggTilYtelsesData(ytelsesTilstand, innhold.get());
                leggTilRelevantAAPData(ytelsesTilstand, innhold.get());
            }
            case TILTAKSPENGER -> leggTilYtelsesData(ytelsesTilstand, innhold.get());
        }

        ytelsesStatusRepositoryV2.upsertYtelse(ytelsesTilstand);
    }

    private void leggTilYtelsesData(Brukerdata ytelsesTilstand, YtelseDAO innhold) {
        YtelseMapping ytelseMapping = YtelseMapping.of(innhold)
                .orElseThrow(() -> new RuntimeException(innhold.toString()));
        LocalDateTime utlopsDato = Optional.ofNullable(innhold.getUtlopsDato())
                .map(Timestamp::toLocalDateTime)
                .orElse(null);

        ytelsesTilstand.setYtelse(ytelseMapping).setUtlopsdato(utlopsDato);
    }

    private void leggTilRelevantDagpengeData(Brukerdata ytelsesTilstand, YtelseDAO innhold) {
        ytelsesTilstand
                .setDagputlopUke(innhold.getAntallUkerIgjen())
                .setPermutlopUke(innhold.getAntallUkerIgjenPermittert());
    }

    private void leggTilRelevantAAPData(Brukerdata ytelsesTilstand, YtelseDAO innhold) {
        ytelsesTilstand
                .setAapmaxtidUke(innhold.getAntallUkerIgjen())
                .setAapUnntakDagerIgjen(innhold.getAntallDagerIgjenUnntak());
    }
}
