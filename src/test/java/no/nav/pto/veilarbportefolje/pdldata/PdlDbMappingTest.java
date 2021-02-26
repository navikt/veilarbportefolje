package no.nav.pto.veilarbportefolje.pdldata;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.sbl.sql.SqlUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomPersonId;
import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PdlDbMappingTest {

    @Mock
    private UnleashService unleashService;

    private PdlRepository pdlRepository;
    private BrukerRepository brukerRepository;
    private JdbcTemplate jdbc;

    @Before
    public void setup() {
        SingleConnectionDataSource ds = setupInMemoryDatabase();

        jdbc = new JdbcTemplate(ds);
        brukerRepository = new BrukerRepository(jdbc, new NamedParameterJdbcTemplate(ds), unleashService);
        pdlRepository = new PdlRepository(jdbc);

        jdbc.execute("TRUNCATE TABLE " + Table.PDL_DATA.TABLE_NAME);
        jdbc.execute("TRUNCATE TABLE " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbc.execute("TRUNCATE TABLE " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
        jdbc.execute("TRUNCATE TABLE " + Table.BRUKER_DATA.TABLE_NAME);
        jdbc.execute("TRUNCATE TABLE " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);

        reset(unleashService);
    }

    @Test
    public void skal_hente_bruker_fra_view_har_riktig_fodselinfo_uten_pdl() {
        final Fnr fnr = Fnr.ofValidFnr("12039531212");
        final PersonId personId = randomPersonId();
        SqlUtils.insert(jdbc, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .execute();

        when(unleashService.isEnabled(FeatureToggle.PDL)).thenReturn(false);

        final Optional<OppfolgingsBruker> bruker = brukerRepository.hentBrukerFraView(fnr);
        assertThat(bruker).isPresent();
        assertThat(bruker.get().getFodselsdato()).isEqualTo("1995-03-12T00:00:00Z");
        assertThat(bruker.get().getFodselsdag_i_mnd()).isEqualTo(12);
    }

    @Test
    public void skal_hente_bruker_fra_view_har_riktig_fodselinfo_med_pdl() {
        Fnr fnr = randomFnr();
        AktorId aktorId = AktorId.of("123456789");
        PersonId personId = PersonId.of("12345");

        SqlUtils.insert(jdbc, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .execute();
        SqlUtils.insert(jdbc, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktorId.get())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.getValue())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, 1)
                .execute();

        pdlRepository.upsert(aktorId, DateUtils.getLocalDateFromSimpleISODate("1995-03-12"));

        when(unleashService.isEnabled(FeatureToggle.PDL)).thenReturn(true);

        Optional<OppfolgingsBruker> bruker = brukerRepository.hentBrukerFraView(fnr);
        assertThat(bruker).isPresent();
        assertThat(bruker.get().getFodselsdato()).isEqualTo("1995-03-12T00:00:00Z");
        assertThat(bruker.get().getFodselsdag_i_mnd()).isEqualTo(12);
    }
}
