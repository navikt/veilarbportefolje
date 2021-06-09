package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.*;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.sbl.sql.SqlUtils;
import org.elasticsearch.action.get.GetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ArenaAktivitetIntegrasjonsTest extends EndToEndTest {
    private final UtdanningsAktivitetService utdanningsAktivitetService;
    private final GruppeAktivitetService gruppeAktivitetService;
    private final TiltaksService tiltaksService;
    private final JdbcTemplate jdbcTemplate;

    private final AktorId aktorId = AktorId.of("1000123");
    private final Fnr fnr = Fnr.of("12345678912");
    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId testEnhet = EnhetId.of("0000");
    private final PersonId personId = PersonId.of("123");

    @Autowired
    public ArenaAktivitetIntegrasjonsTest(AktivitetService aktivitetService, JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        AktorClient aktorClient = Mockito.mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);

        this.tiltaksService = new TiltaksService(aktivitetService, aktorClient);
        this.gruppeAktivitetService = new GruppeAktivitetService(aktivitetService, aktorClient);
        this.utdanningsAktivitetService = new UtdanningsAktivitetService(aktivitetService, aktorClient);
    }

    @BeforeEach
    public void resetOgInsert() {
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGING_DATA.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);

    }

    @Test
    public void skal_komme_i_gruppe_aktivitet() {
        insertBruker();
        GruppeAktivitetDTO gruppeAktivitet =  new GruppeAktivitetDTO()
                .setAfter(new GruppeAktivitetInnhold()
                        .setFnr(fnr.get())
                        .setAktivitetperiodeFra(LocalDate.of(2020, 1, 1)) //2020-01-01
                        .setAktivitetperiodeTil(LocalDate.of(2030, 1, 1)) //2030-01-01
                        .setEndretDato(LocalDate.now())
                        .setAktivitetid("UA-123456789")
                );
        gruppeAktivitet.setOperationType(GoldenGateOperations.INSERT);
        gruppeAktivitetService.behandleKafkaMelding(gruppeAktivitet);

        GetResponse response = elasticTestClient.fetchDocument(aktorId);
        List<String> aktiviteter = (List<String>)response.getSourceAsMap().get("aktiviteter");

        assertThat(response.isExists()).isTrue();
        assertThat(aktiviteter).isNotNull();
    }

    @Test
    public void skal_komme_i_tiltak() {
        insertBruker();
        TiltakDTO tiltakDTO =  new TiltakDTO()
                .setAfter(new TiltakInnhold()
                        .setFnr(fnr.get())
                        .setAktivitetperiodeFra(LocalDate.of(2020, 1, 1)) //2020-01-01
                        .setAktivitetperiodeTil(LocalDate.of(2030, 1, 1)) //2030-01-01
                        .setEndretDato(LocalDate.now())
                        .setAktivitetid("UA-123456789")
                );
        tiltakDTO.setOperationType(GoldenGateOperations.INSERT);
        tiltaksService.behandleKafkaMelding(tiltakDTO);

        GetResponse response = elasticTestClient.fetchDocument(aktorId);
        List<String> aktiviteter = (List<String>)response.getSourceAsMap().get("aktiviteter");

        assertThat(response.isExists()).isTrue();
        assertThat(aktiviteter).isNotNull();
    }

    @Test
    public void skal_komme_i_utdannnings_aktivitet() {
        insertBruker();
        utdanningsAktivitetService.behandleKafkaMelding(getUtdanningsInsertDTO());

        GetResponse response = elasticTestClient.fetchDocument(aktorId);
        List<String> aktiviteter = (List<String>)response.getSourceAsMap().get("aktiviteter");

        assertThat(response.isExists()).isTrue();
        assertThat(aktiviteter).isNotNull();
    }


    @Test
    public void skal_ut_av_aktivitet() {
        insertBruker();
        utdanningsAktivitetService.behandleKafkaMelding(getUtdanningsInsertDTO());
        utdanningsAktivitetService.behandleKafkaMelding(getUtdanningsDeleteDTO());

        GetResponse response = elasticTestClient.fetchDocument(aktorId);
        List<String> aktiviteter = (List<String>) response.getSourceAsMap().get("aktiviteter");

        assertThat(response.isExists()).isTrue();
        assertThat(aktiviteter.isEmpty()).isTrue();
    }

    private UtdanningsAktivitetDTO getUtdanningsInsertDTO(){
        UtdanningsAktivitetDTO utdanningsAktivitet =  new UtdanningsAktivitetDTO()
                .setAfter(new UtdanningsAktivitetInnhold()
                        .setFnr(fnr.get())
                        .setAktivitetperiodeFra(LocalDate.of(2020, 1, 1)) //2020-01-01
                        .setAktivitetperiodeTil(LocalDate.of(2030, 1, 1)) //2030-01-01
                        .setEndretDato(LocalDate.now())
                        .setAktivitetid("UA-123456789")
                );
        utdanningsAktivitet.setOperationType(GoldenGateOperations.INSERT);
        return utdanningsAktivitet;
    }

    private UtdanningsAktivitetDTO getUtdanningsDeleteDTO(){
        UtdanningsAktivitetDTO utdanningsAktivitet =  new UtdanningsAktivitetDTO()
                .setBefore(new UtdanningsAktivitetInnhold()
                        .setFnr(fnr.get())
                        .setAktivitetperiodeFra(LocalDate.of(2020, 1, 1)) //2020-01-01
                        .setAktivitetperiodeTil(LocalDate.of(2030, 1, 1)) //2030-01-01
                        .setEndretDato(LocalDate.now())
                        .setAktivitetid("UA-123456789")
                );
        utdanningsAktivitet.setOperationType(GoldenGateOperations.DELETE);
        return utdanningsAktivitet;
    }

    private void insertBruker() {
        populateElastic(testEnhet, veilederId, aktorId.get());
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, testEnhet.toString())
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .execute();
        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktorId.toString())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .value(Table.OPPFOLGING_DATA.VEILEDERIDENT, veilederId.toString())
                .execute();
        SqlUtils.insert(jdbcTemplate, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktorId.toString())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.toString())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, 1)
                .execute();
    }
}
