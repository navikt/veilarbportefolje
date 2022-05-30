package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.YtelseMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class YtelsesStatusRepositoryV2 {
    private final JdbcTemplate db;

    public void upsertYtelseStatus(Brukerdata brukerdata) {
        String ytelse = Optional.ofNullable(brukerdata.getYtelse()).map(YtelseMapping::toString).orElse(null);

        db.update("""
                        INSERT INTO YTELSE_STATUS_FOR_BRUKER
                        (AKTOERID, YTELSE, UTLOPSDATO, DAGPUTLOPUKE, PERMUTLOPUKE, AAPMAXTIDUKE, AAPUNNTAKDAGERIGJEN)
                        VALUES(?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (AKTOERID)
                        DO UPDATE SET (YTELSE, UTLOPSDATO, DAGPUTLOPUKE, PERMUTLOPUKE, AAPMAXTIDUKE, AAPUNNTAKDAGERIGJEN) = (?, ?, ?, ?, ?, ?)
                        """,
                brukerdata.getAktoerid(), ytelse, brukerdata.getUtlopsdato(), brukerdata.getDagputlopUke(), brukerdata.getPermutlopUke(), brukerdata.getAapmaxtidUke(), brukerdata.getAapUnntakDagerIgjen(),
                ytelse, brukerdata.getUtlopsdato(), brukerdata.getDagputlopUke(), brukerdata.getPermutlopUke(), brukerdata.getAapmaxtidUke(), brukerdata.getAapUnntakDagerIgjen()
        );
    }

    public void slettYtelseStatus(AktorId aktorid) {
        db.update("delete from ytelse_status_for_bruker where aktoerid = ?", aktorid.get());
    }
}
