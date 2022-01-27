package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatusRepositoryV2;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetTyperFraKafka;
import no.nav.pto.veilarbportefolje.aktiviteter.AktiviteterRepositoryV2;
import no.nav.pto.veilarbportefolje.aktiviteter.AktoerAktiviteter;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.UtdanningsAktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.UtdanningsAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.UtdanningsAktivitetInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import no.nav.sbl.sql.SqlUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class ArenaAktivitetIntegrasjonsTest {
    private final UtdanningsAktivitetService utdanningsAktivitetService;
    private final JdbcTemplate jdbcTemplate;
    private final AktivitetService aktivitetService;
    private final AktivitetDAO aktivitetDAO;
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final AktorId aktorId = AktorId.of("1000123");
    private final Fnr fnr = Fnr.of("12345678912");
    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId testEnhet = EnhetId.of("0000");
    private final PersonId personId = PersonId.of("123");

    @Autowired
    public ArenaAktivitetIntegrasjonsTest(SisteEndringService sisteEndringService, BrukerService brukerService, AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering, JdbcTemplate jdbcTemplate, AktiviteterRepositoryV2 aktiviteterRepositoryV2, AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2, BrukerDataService brukerDataService, AktiviteterRepositoryV2 aktiviteterRepositoryV21) {
        this.jdbcTemplate = jdbcTemplate;
        this.aktivitetDAO = aktivitetDAO;
        this.aktiviteterRepositoryV2 = aktiviteterRepositoryV21;

        ArenaHendelseRepository arenaHendelseRepository = mock(ArenaHendelseRepository.class);
        Mockito.when(arenaHendelseRepository.upsertAktivitetHendelse(anyString(), anyLong())).thenReturn(1);
        AktorClient aktorClient = Mockito.mock(AktorClient.class);
        Mockito.when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        Mockito.when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);

        this.aktivitetService = new AktivitetService(aktivitetDAO, aktiviteterRepositoryV2, aktivitetStatusRepositoryV2, persistentOppdatering, brukerService, brukerDataService, sisteEndringService, mock(UnleashService.class), mock(OpensearchIndexer.class));
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
    public void skal_komme_i_utdannnings_aktivitet() {
        insertBruker();
        utdanningsAktivitetService.behandleKafkaMelding(getUtdanningsInsertDTO());

        AktoerAktiviteter aktiviteterForAktoerid = aktiviteterRepositoryV2.getAktiviteterForAktoerid(aktorId, false);
        AktivitetStatus aktivitetStatus = aktiviteterRepositoryV2.getAktivitetStatus(aktorId, KafkaAktivitetMelding.AktivitetTypeData.UTDANNINGAKTIVITET, false);
        Optional<AktivitetStatus> utdanning = hentAktivitetStatus(AktivitetTyperFraKafka.utdanningaktivitet);
        assertThat(utdanning).isPresent();
        assertThat(aktiviteterForAktoerid.getAktiviteter().stream().anyMatch(x->x.getAktivitetID().equals("UA-123456789"))).isTrue();
        assertThat(aktivitetStatus.isAktiv()).isTrue();
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
        return new UtdanningsAktivitetDTO()
                .setAfter(new UtdanningsAktivitetInnhold()
                        .setFnr(fnr.get())
                        .setHendelseId(1)
                        .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                        .setAktivitetperiodeTil(new ArenaDato("2030-01-01"))
                        .setEndretDato(new ArenaDato("2021-01-01"))
                        .setAktivitetid("UA-123456789")
                );
    }

    private UtdanningsAktivitetDTO getUtdanningsDeleteDTO() {
        return new UtdanningsAktivitetDTO()
                .setBefore(new UtdanningsAktivitetInnhold()
                        .setFnr(fnr.get())
                        .setHendelseId(1)
                        .setAktivitetperiodeFra(new ArenaDato("2020-01-01"))
                        .setAktivitetperiodeTil(new ArenaDato("2030-01-01"))
                        .setEndretDato(new ArenaDato("2021-01-01"))
                        .setAktivitetid("UA-123456789")
                );
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
