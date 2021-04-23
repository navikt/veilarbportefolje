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
        OppfolgingsbrukerKafkaDTO msg = new OppfolgingsbrukerKafkaDTO()
                .setAktoerid(aktoerId.get())
                .setFodselsnr("12345678912")
                .setFormidlingsgruppekode("TEST")
                .setIserv_fra_dato(ZonedDateTime.now().minusDays(1))
                .setFornavn("Tester")
                .setEtternavn("Testerson")
                .setNav_kontor("1001")
                .setKvalifiseringsgruppekode("ORG")
                .setRettighetsgruppekode("OP")
                .setHovedmaalkode("TES")
                .setSikkerhetstiltak_type_kode("IKKE")
                .setFr_kode("1234")
                .setHar_oppfolgingssak(true)
                .setSperret_ansatt(true)
                .setEr_doed(true)
                .setDoed_fra_dato(ZonedDateTime.now())
                .setEndret_dato(ZonedDateTime.now());


        OppfolgingsbrukerKafkaDTO old_msg = new OppfolgingsbrukerKafkaDTO()
                .setAktoerid(aktoerId.get())
                .setDoed_fra_dato(ZonedDateTime.now())
                .setEr_doed(false)
                .setEndret_dato(ZonedDateTime.now().minusDays(5));

        oppfolginsbrukerRepositoryV2.LeggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(msg);

        oppfolginsbrukerRepositoryV2.LeggTilEllerEndreOppfolgingsbruker(old_msg);
        assertThat(oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isNotEqualTo(old_msg);
    }


    @Test
    public void skal_oppdater_oppfolgingsbruker_fra_nyere_dato() {
        OppfolgingsbrukerKafkaDTO msg = new OppfolgingsbrukerKafkaDTO()
                .setAktoerid(aktoerId.get())
                .setEr_doed(true)
                .setSperret_ansatt(false)
                .setHar_oppfolgingssak(true)
                .setDoed_fra_dato(ZonedDateTime.now())
                .setEndret_dato(ZonedDateTime.now().minusDays(1));

        OppfolgingsbrukerKafkaDTO new_msg = new OppfolgingsbrukerKafkaDTO()
                .setAktoerid(aktoerId.get())
                .setSperret_ansatt(true)
                .setHar_oppfolgingssak(false)
                .setEr_doed(false)
                .setEndret_dato(ZonedDateTime.now());

        oppfolginsbrukerRepositoryV2.LeggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(msg);

        oppfolginsbrukerRepositoryV2.LeggTilEllerEndreOppfolgingsbruker(new_msg);
        assertThat(oppfolginsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(new_msg);
    }
}
