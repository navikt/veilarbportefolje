package no.nav.pto.veilarbportefolje.arbeidsliste;

import lombok.Value;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomPersonId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ApplicationConfigTest.class)
class ArbeidslisteServiceTest {

    private final ArbeidslisteService arbeidslisteService;
    private final ArbeidslisteRepositoryV1 arbeidslisteRepositoryV1;
    private final ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2;
    private final JdbcTemplate jdbcTemplateOracle;
    private final JdbcTemplate jdbcTemplatePostgres;
    private final AktorClient aktorClient;
    private AktorId aktoerId;

    @Autowired
    public ArbeidslisteServiceTest(ArbeidslisteService arbeidslisteService, ArbeidslisteRepositoryV1 arbeidslisteRepositoryV1, ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2, JdbcTemplate jdbcTemplateOracle, @Qualifier("PostgresJdbc")  JdbcTemplate jdbcTemplatePostgres, AktorClient aktorClient) {
        this.arbeidslisteService = arbeidslisteService;
        this.arbeidslisteRepositoryV1 = arbeidslisteRepositoryV1;
        this.arbeidslisteRepositoryV2 = arbeidslisteRepositoryV2;
        this.jdbcTemplateOracle = jdbcTemplateOracle;
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.aktorClient = aktorClient;
    }

    @BeforeEach
    void setup() {
        aktoerId = TestDataUtils.randomAktorId();
        when(aktorClient.hentAktorId(any(Fnr.class))).thenReturn(aktoerId);
        jdbcTemplateOracle.execute("TRUNCATE TABLE " + Table.ARBEIDSLISTE.TABLE_NAME);
        jdbcTemplateOracle.execute("TRUNCATE TABLE " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplateOracle.execute("TRUNCATE TABLE " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
        jdbcTemplatePostgres.execute("TRUNCATE TABLE arbeidsliste");
    }

    @Test
    void skal_inserte_fnr_i_arbeidslisten() {
        NavKontor excpectedNavKontor = TestDataUtils.randomNavKontor();
        FnrOgNavKontor fnrOgNavKontor = setUpInitialState(aktoerId, excpectedNavKontor);

        String actualFnr = SqlUtils
                .select(jdbcTemplateOracle, Table.ARBEIDSLISTE.TABLE_NAME, rs -> rs.getString(Table.ARBEIDSLISTE.FNR))
                .column(Table.ARBEIDSLISTE.FNR)
                .where(WhereClause.equals(Table.ARBEIDSLISTE.FNR, fnrOgNavKontor.getFnr()))
                .execute();

        assertThat(actualFnr).isEqualTo(fnrOgNavKontor.getFnr());

        NavKontor actualNavKontor = NavKontor.of(arbeidslisteRepositoryV1.hentNavKontorForArbeidsliste(aktoerId).orElseThrow());

        assertThat(actualNavKontor).isEqualTo(excpectedNavKontor);
    }

    @Test
    public void migrerArbeidslista(){
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();

        ArbeidslisteDTO dto1 = new ArbeidslisteDTO(randomFnr())
                .setNavKontorForArbeidsliste("0000")
                .setAktorId(aktorId1)
                .setVeilederId(VeilederId.of("0"))
                .setFrist(Timestamp.from(now()))
                .setKategori(Arbeidsliste.Kategori.BLA)
                .setOverskrift("foo");

        ArbeidslisteDTO dto2 = new ArbeidslisteDTO(randomFnr())
                .setNavKontorForArbeidsliste("1111")
                .setAktorId(aktorId2)
                .setVeilederId(VeilederId.of("1"))
                .setFrist(Timestamp.from(now()))
                .setKategori(Arbeidsliste.Kategori.GRONN)
                .setKommentar("test")
                .setOverskrift("foo2");

        arbeidslisteRepositoryV1.insertArbeidsliste(dto1);
        arbeidslisteRepositoryV1.insertArbeidsliste(dto2);

        arbeidslisteService.migrerArbeidslistaTilPostgres();
        Arbeidsliste arbeidsliste1Oracle = arbeidslisteRepositoryV1.retrieveArbeidsliste(aktorId1).get();
        Arbeidsliste arbeidsliste2Oracle = arbeidslisteRepositoryV1.retrieveArbeidsliste(aktorId2).get();

        Arbeidsliste arbeidsliste1Postgres = arbeidslisteRepositoryV2.retrieveArbeidsliste(aktorId1).get();
        Arbeidsliste arbeidsliste2Postgres = arbeidslisteRepositoryV2.retrieveArbeidsliste(aktorId2).get();


        String navKontor1 = arbeidslisteRepositoryV2.hentNavKontorForArbeidsliste(aktorId1).get();
        String navKontor2 = arbeidslisteRepositoryV2.hentNavKontorForArbeidsliste(aktorId2).get();

        assertThat(arbeidsliste1Postgres).isEqualTo(arbeidsliste1Oracle);
        assertThat(arbeidsliste2Postgres).isEqualTo(arbeidsliste2Oracle);
        assertThat(arbeidsliste2Postgres).isEqualTo(arbeidsliste2Oracle);
        assertThat(navKontor1).isEqualTo("0000");
        assertThat(navKontor2).isEqualTo("1111");
    }

    private FnrOgNavKontor setUpInitialState(AktorId aktoerId, NavKontor navKontor) {
        Fnr fnr = randomFnr();
        PersonId personId = randomPersonId();

        SqlUtils
                .insert(jdbcTemplateOracle, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, navKontor.toString())
                .execute();

        SqlUtils
                .insert(jdbcTemplateOracle, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktoerId.toString())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.toString())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, true)
                .execute();

        ArbeidslisteDTO dto = new ArbeidslisteDTO(fnr)
                .setNavKontorForArbeidsliste("0000")
                .setAktorId(aktoerId)
                .setVeilederId(VeilederId.of("0"))
                .setFrist(Timestamp.from(now()))
                .setKategori(Arbeidsliste.Kategori.BLA)
                .setOverskrift("foo");

        arbeidslisteService.createArbeidsliste(dto);

        return new FnrOgNavKontor(fnr.toString(), navKontor.toString());
    }

    @Value
    private static class FnrOgNavKontor {
        String fnr;
        String navKontor;
    }

}
