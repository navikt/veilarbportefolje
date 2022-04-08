package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class PdlRepository {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void upsertFodselsdag(AktorId aktorId, LocalDate fodselsdag){

    }

    public void slettPdlData(AktorId aktorId){

    }

}
