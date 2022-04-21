package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingPeriodeService;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PDLIdent;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class PdlRepositoryTest {
    private final JdbcTemplate db;
    private final JdbcTemplate oracle;
    private final PdlRepository pdlRepository;
    private final OppfolgingPeriodeService oppfolgingPeriodeService;

    @Autowired
    public PdlRepositoryTest(@Qualifier("PostgresJdbc") JdbcTemplate db, JdbcTemplate oracle, PdlRepository pdlRepository, OppfolgingPeriodeService oppfolgingPeriodeService) {
        this.db = db;
        this.oracle = oracle;
        this.pdlRepository = pdlRepository;
        this.oppfolgingPeriodeService = oppfolgingPeriodeService;
    }

    @BeforeEach
    public void reset(){
        oracle.execute("truncate table OPPFOLGINGSBRUKER");
        oracle.execute("truncate table AKTOERID_TO_PERSONID");
        db.update("truncate oppfolging_data");
        db.update("truncate bruker_identer");
    }

    @Test
    public void identOvergang_splitt() {
        PDLIdent identIKonflikt = new PDLIdent("12345", true, AKTORID);
        List<PDLIdent> identerBrukerA = List.of(
                identIKonflikt,
                new PDLIdent("12346", false, AKTORID),
                new PDLIdent("00000", false, FOLKEREGISTERIDENT)
        );
        List<PDLIdent> identerBrukerB = List.of(
                identIKonflikt,
                new PDLIdent("12347", false, AKTORID),
                new PDLIdent("00001", false, FOLKEREGISTERIDENT)
        );
        pdlRepository.upsertIdenter(identerBrukerA);
        String lokalIdentBrukerA = pdlRepository.hentLokalIdent(identIKonflikt.getIdent());

        var brukerAPreBrukerB = db.queryForList("select * from bruker_identer where person = ?", lokalIdentBrukerA)
                .stream()
                .map(PdlServiceTest::mapTilident)
                .toList();

        pdlRepository.upsertIdenter(identerBrukerB);

        String lokalIdentBrukerB = pdlRepository.hentLokalIdent(identIKonflikt.getIdent());
        var brukerAPostBrukerB = db.queryForList("select * from bruker_identer where person = ?", lokalIdentBrukerA)
                .stream()
                .map(PdlServiceTest::mapTilident)
                .toList();
        var brukerB = db.queryForList("select * from bruker_identer where person = ?", lokalIdentBrukerB)
                .stream()
                .map(PdlServiceTest::mapTilident)
                .toList();

        assertThat(lokalIdentBrukerA).isNotEqualTo(lokalIdentBrukerB);
        assertThat(brukerAPreBrukerB).isEqualTo(identerBrukerA);
        assertThat(brukerAPostBrukerB).isNotEqualTo(identerBrukerA);
        assertThat(brukerB).isEqualTo(identerBrukerB);
        assertThat(brukerAPostBrukerB.size()).isEqualTo(2);
    }

    @Test
    public void oppfolgingAvsluttet_flereIdenterUnderOppfolging() {
        AktorId historiskIdent = AktorId.of("12345");
        AktorId ident = AktorId.of("12346");
        List<PDLIdent> identer = List.of(
                new PDLIdent(historiskIdent.get(), true, AKTORID),
                new PDLIdent(ident.get(), false, AKTORID)
        );
        var historiskOpfolgingStart = new SisteOppfolgingsperiodeV1(null, historiskIdent.get(), ZonedDateTime.now(), null);
        var nyOpfolgingStart = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), null);
        var nyOpfolgingAvslutt = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), ZonedDateTime.now());
        // Mock fix for oracle
        oracle.update("insert into OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR) values (1234, '01010100000')");
        oracle.update("insert into OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR) values (1235, '01010200000')");
        oracle.update("insert into AKTOERID_TO_PERSONID (AKTOERID, PERSONID, GJELDENE) values ("+historiskIdent.get()+", 1235, 1)");
        oracle.update("insert into AKTOERID_TO_PERSONID (AKTOERID, PERSONID, GJELDENE) values ("+ident.get()+", 1234, 1)");

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(historiskOpfolgingStart);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(nyOpfolgingStart);
        // Mock PDL respons
        pdlRepository.upsertIdenter(identer);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(nyOpfolgingAvslutt);

        var lokaleIdenter = db.queryForList("select * from bruker_identer where person = ?", pdlRepository.hentLokalIdent(historiskIdent.get()))
                .stream()
                .map(PdlServiceTest::mapTilident)
                .toList();
        assertThat(identer).isEqualTo(lokaleIdenter);
    }

    @Test
    public void oppfolgingAvsluttet_ingenAndreIdenterUnderOppfolging() {
        AktorId ident = AktorId.of("12346");
        // Mock fix for oracle
        oracle.update("insert into OPPFOLGINGSBRUKER (PERSON_ID, FODSELSNR) values (1234, '01010100000')");
        oracle.update("insert into AKTOERID_TO_PERSONID (AKTOERID, PERSONID, GJELDENE) values ("+ident.get()+", 1234, 1)");
        List<PDLIdent> identer = List.of(
                new PDLIdent(ident.get(), false, AKTORID)
        );
        var opfolgingStart = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), null);
        var opfolgingAvslutt = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), ZonedDateTime.now());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(opfolgingStart);
        // Mock PDL respons
        pdlRepository.upsertIdenter(identer);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(opfolgingAvslutt);
        var lokaleIdenter = db.queryForList("select * from bruker_identer where person = ?", pdlRepository.hentLokalIdent(ident.get()))
                .stream()
                .map(PdlServiceTest::mapTilident)
                .toList();
        assertThat(lokaleIdenter.size()).isEqualTo(0);
    }
}
