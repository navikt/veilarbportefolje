package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyperFraKafka;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.UtdanningsAktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.*;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.sbl.sql.SqlUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.GR202_PA_KAFKA;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class ArenaAktivitetIntegrasjonsTest {
    private final UtdanningsAktivitetService utdanningsAktivitetService;
    private final JdbcTemplate jdbcTemplate;
    private final UnleashService unleashService;
    private final AktivitetService aktivitetService;
    private final AktivitetDAO aktivitetDAO;

    private final AktorId aktorId = AktorId.of("1000123");
    private final Fnr fnr = Fnr.of("12345678912");
    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId testEnhet = EnhetId.of("0000");
    private final PersonId personId = PersonId.of("123");

    @Autowired
    public ArenaAktivitetIntegrasjonsTest(SisteEndringService sisteEndringService, BrukerService brukerService, AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering, JdbcTemplate jdbcTemplate, UnleashService unleashService) {
        this.jdbcTemplate = jdbcTemplate;
        this.unleashService = unleashService;
        this.aktivitetDAO = aktivitetDAO;

        ArenaHendelseRepository arenaHendelseRepository = mock(ArenaHendelseRepository.class);
        Mockito.when(arenaHendelseRepository.upsertHendelse(anyString(), anyLong())).thenReturn(1);
        AktorClient aktorClient = Mockito.mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);

        this.aktivitetService = new AktivitetService(aktivitetDAO, persistentOppdatering, brukerService, sisteEndringService, unleashService);
        this.utdanningsAktivitetService = new UtdanningsAktivitetService(aktivitetService, aktorClient, arenaHendelseRepository);
    }

    @BeforeEach
    public void reset() {
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.OPPFOLGING_DATA.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
        jdbcTemplate.execute("truncate table " + Table.AKTIVITETER.TABLE_NAME);
        jdbcTemplate.execute("truncate table BRUKERSTATUS_AKTIVITETER");
    }

    @Test
    public void skal_kunne_toggle_pa_GR202() {
        insertBruker();
        String melding = new JSONObject()
                .put("aktivitetId", 1)
                .put("aktorId", aktorId.get())
                .put("fraDato", "2020-08-31T10:03:20+02:00")
                .put("tilDato", "2040-08-31T10:03:20+02:00")
                .put("endretDato", "2020-07-29T15:43:41.049+02:00")
                .put("aktivitetType", "IJOBB")
                .put("aktivitetStatus", "GJENNOMFORES")
                .put("avtalt", true)
                .put("historisk", false)
                .put("version", 1)
                .toString();

        when(unleashService.isEnabled(GR202_PA_KAFKA)).thenReturn(false);
        aktivitetService.behandleKafkaMelding(melding);
        when(unleashService.isEnabled(GR202_PA_KAFKA)).thenReturn(true);

        utdanningsAktivitetService.behandleKafkaMelding(getUtdanningsInsertDTO());

        Optional<AktivitetStatus> utdanningsAktivitet = hentAktivitetStatus(AktivitetTyperFraKafka.utdanningaktivitet);
        Optional<AktivitetStatus> ijobbAktivitet = hentAktivitetStatus(AktivitetTyperFraKafka.ijobb);

        assertThat(utdanningsAktivitet).isPresent();
        assertThat(ijobbAktivitet).isPresent();
    }

    @Test
    public void skal_komme_i_utdannnings_aktivitet() {
        insertBruker();
        utdanningsAktivitetService.behandleKafkaMelding(getUtdanningsInsertDTO());

        Optional<AktivitetStatus> utdanning = hentAktivitetStatus(AktivitetTyperFraKafka.utdanningaktivitet);
        assertThat(utdanning).isPresent();
    }


    @Test
    public void skal_ut_av_aktivitet() {
        insertBruker();
        utdanningsAktivitetService.behandleKafkaMelding(getUtdanningsInsertDTO());

        Optional<AktivitetStatus> utdanningPre = hentAktivitetStatus(AktivitetTyperFraKafka.utdanningaktivitet);
        utdanningsAktivitetService.behandleKafkaMelding(getUtdanningsDeleteDTO());
        Optional<AktivitetStatus> utdanningPost = hentAktivitetStatus(AktivitetTyperFraKafka.utdanningaktivitet);

        assertThat(utdanningPre).isPresent();
        assertThat(utdanningPost).isEmpty();
    }

    private UtdanningsAktivitetDTO getUtdanningsInsertDTO() {
        UtdanningsAktivitetDTO utdanningsAktivitet = new UtdanningsAktivitetDTO()
                .setAfter(new UtdanningsAktivitetInnhold()
                        .setFnr(fnr.get())
                        .setHendelseId(1)
                        .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                        .setAktivitetperiodeTil(new ArenaDato("2030-01-01"))
                        .setEndretDato(new ArenaDato("2021-01-01"))
                        .setAktivitetid("UA-123456789")
                );
        utdanningsAktivitet.setOperationType(GoldenGateOperations.INSERT);
        return utdanningsAktivitet;
    }

    private UtdanningsAktivitetDTO getUtdanningsDeleteDTO() {
        UtdanningsAktivitetDTO utdanningsAktivitet = new UtdanningsAktivitetDTO()
                .setBefore(new UtdanningsAktivitetInnhold()
                        .setFnr(fnr.get())
                        .setHendelseId(1)
                        .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                        .setAktivitetperiodeTil(new ArenaDato("2030-01-01"))
                        .setEndretDato(new ArenaDato("2021-01-01"))
                        .setAktivitetid("UA-123456789")
                );
        utdanningsAktivitet.setOperationType(GoldenGateOperations.DELETE);
        return utdanningsAktivitet;
    }

    private Optional<AktivitetStatus> hentAktivitetStatus(AktivitetTyperFraKafka type) {
        Set<AktivitetStatus> aktivitetstatusForBrukere = aktivitetDAO.getAktivitetstatusForBrukere(List.of(personId)).get(personId);
        return aktivitetstatusForBrukere.stream()
                .filter(AktivitetStatus::isAktiv)
                .filter(x -> x.getAktivitetType().equals(type.name()))
                .findFirst();
    }

    private void insertBruker() {
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
