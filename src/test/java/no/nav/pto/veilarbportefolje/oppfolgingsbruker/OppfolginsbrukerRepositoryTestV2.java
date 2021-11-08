package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OppfolginsbrukerRepositoryTestV2 {
    private JdbcTemplate db;
    private OppfolginsbrukerRepositoryV2 oppfolginsbrukerRepositoryV2;
    private final AktorId aktoerId = AktorId.of("0");

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        oppfolginsbrukerRepositoryV2 = new OppfolginsbrukerRepositoryV2(db);

        SqlUtils.delete(db, PostgresTable.OPPFOLGINGSBRUKER_ARENA.TABLE_NAME)
                .where(WhereClause.equals(PostgresTable.OPPFOLGINGSBRUKER_ARENA.AKTOERID, aktoerId.get()))
                .execute();
    }

    @Test
    public void skal_ikke_lagre_oppfolgingsbruker_med_eldre_endret_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(aktoerId.get(), "12015678912", "TEST", ZonedDateTime.now().minusDays(1),
                "Tester", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, true, true, ZonedDateTime.now(), ZonedDateTime.now());
        OppfolgingsbrukerEntity old_msg = new OppfolgingsbrukerEntity(aktoerId.get(), "12015678912", "TEST", ZonedDateTime.now().minusDays(1),
                "Tester", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, true, false, null, ZonedDateTime.now().minusDays(5));

        oppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(msg);

        oppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(old_msg);
        assertThat(oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isNotEqualTo(old_msg);
    }


    @Test
    public void skal_oppdater_oppfolgingsbruker_fra_nyere_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(aktoerId.get(), "12015678912", "TEST", ZonedDateTime.now().minusDays(1), "" +
                "Tester", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, true, false, null, ZonedDateTime.now().minusDays(5));
        OppfolgingsbrukerEntity new_msg = new OppfolgingsbrukerEntity(aktoerId.get(), "12015678912", "TEST", ZonedDateTime.now().minusDays(1), "" +
                "Tester", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", false, true, true, ZonedDateTime.now(), ZonedDateTime.now());

        oppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(msg);

        oppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(new_msg);
        assertThat(oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(new_msg);
    }
}
