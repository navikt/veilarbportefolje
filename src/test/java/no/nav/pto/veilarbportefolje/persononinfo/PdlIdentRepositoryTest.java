package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingPeriodeService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class PdlIdentRepositoryTest {
    private final JdbcTemplate db;
    private final PdlIdentRepository pdlIdentRepository;
    private final OppfolgingPeriodeService oppfolgingPeriodeService;

    @Autowired
    public PdlIdentRepositoryTest(JdbcTemplate db, PdlIdentRepository pdlIdentRepository, OppfolgingPeriodeService oppfolgingPeriodeService) {
        this.db = db;
        this.pdlIdentRepository = pdlIdentRepository;
        this.oppfolgingPeriodeService = oppfolgingPeriodeService;
    }

    @BeforeEach
    public void reset() {
        db.update("truncate oppfolging_data");
        db.update("truncate bruker_data");
        db.update("truncate bruker_statsborgerskap");
        db.update("truncate bruker_identer");
    }

    @Test
    public void identSplitt_allePersonerMedTidligereIdenterSkalSlettes() {
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
        pdlIdentRepository.upsertIdenter(identerBrukerA);
        String lokalIdentBrukerA = pdlIdentRepository.hentPerson(identIKonflikt.getIdent());
        var brukerAPreBrukerB = pdlIdentRepository.hentIdenter(lokalIdentBrukerA);

        pdlIdentRepository.upsertIdenter(identerBrukerB);

        String lokalIdentBrukerB = pdlIdentRepository.hentPerson(identIKonflikt.getIdent());
        var brukerAPostBrukerB = pdlIdentRepository.hentIdenter(lokalIdentBrukerA);
        var brukerB = pdlIdentRepository.hentIdenter(lokalIdentBrukerB);

        assertThat(lokalIdentBrukerA).isNotEqualTo(lokalIdentBrukerB);
        assertThat(brukerAPreBrukerB).isEqualTo(identerBrukerA);
        assertThat(brukerAPostBrukerB).isNotEqualTo(identerBrukerA);
        assertThat(brukerB).isEqualTo(identerBrukerB);
        assertThat(brukerAPostBrukerB.size()).isEqualTo(0);
    }

    @Test
    public void oppfolgingAvsluttet_flereIdenterUnderOppfolging_lokalIdentLagringSkalIkkeSlettes() {
        AktorId historiskIdent = AktorId.of("12345");
        AktorId ident = AktorId.of("12346");
        List<PDLIdent> identer = List.of(
                new PDLIdent(historiskIdent.get(), true, AKTORID),
                new PDLIdent(ident.get(), false, AKTORID),
                new PDLIdent(randomFnr().get(), false, FOLKEREGISTERIDENT)
        );
        var historiskOpfolgingStart = new SisteOppfolgingsperiodeV1(null, historiskIdent.get(), ZonedDateTime.now(), null);
        var nyOpfolgingStart = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), null);
        var nyOpfolgingAvslutt = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), ZonedDateTime.now());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(historiskOpfolgingStart);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(nyOpfolgingStart);
        // Mock PDL respons
        pdlIdentRepository.upsertIdenter(identer);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(nyOpfolgingAvslutt);

        var lokaleIdenter = hentLokaleIdenter(historiskIdent);
        assertThat(identer).isEqualTo(lokaleIdenter);
    }

    @Test
    public void oppfolgingAvsluttet_ingenAndreIdenterUnderOppfolging_identLagringSkalSlettes() {
        AktorId ident = AktorId.of("12346");
        List<PDLIdent> identer = List.of(
                new PDLIdent(ident.get(), false, AKTORID)
        );
        var opfolgingStart = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), null);
        var opfolgingAvslutt = new SisteOppfolgingsperiodeV1(null, ident.get(), ZonedDateTime.now(), ZonedDateTime.now());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(opfolgingStart);
        // Mock PDL respons
        pdlIdentRepository.upsertIdenter(identer);
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(opfolgingAvslutt);
        var lokaleIdenter = hentLokaleIdenter(ident);
        assertThat(lokaleIdenter.size()).isEqualTo(0);
    }

    private List<PDLIdent> hentLokaleIdenter(AktorId ident) {
        return pdlIdentRepository.hentIdenter(pdlIdentRepository.hentPerson(ident.get()));
    }
}
