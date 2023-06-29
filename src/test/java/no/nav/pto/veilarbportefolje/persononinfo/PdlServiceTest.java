package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentResponse;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdlServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String pdlIdentResponsFraFil = readFileAsJsonString("/PDL_Files/identer_pdl.json", getClass());
    private final String pdlPersonResponsFraFil = readFileAsJsonString("/PDL_Files/person_pdl.json", getClass());
    private final String pdlPersonBarn1ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn_pdl.json", getClass());
    private final String pdlPersonBarn2ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn2_pdl.json", getClass());
    private final String pdlPersonBarn3ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn3_pdl.json", getClass());
    private final JdbcTemplate db;
    private final PdlPersonRepository pdlPersonRepository;

    private BarnUnder18AarService barnUnder18AarService;
    private PdlService pdlService;

    private WireMockServer server = new WireMockServer();
    public PdlServiceTest() {
        this.db = SingletonPostgresContainer.init().createJdbcTemplate();
        pdlPersonRepository = new PdlPersonRepository(db, db);
    }

    @BeforeEach
    public void setup() {
        db.update("truncate bruker_identer");
        server.stubFor(
                post(anyUrl())
                        .inScenario("PDL test")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(pdlIdentResponsFraFil))
                        .willSetStateTo("hent person")
        );
        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonResponsFraFil))
                .whenScenarioStateIs("hent person")
                .willSetStateTo("hent barn 1")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn1ResponsFraFil))
                .whenScenarioStateIs("hent barn 1")
                .willSetStateTo("hent barn 2")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn2ResponsFraFil))
                .whenScenarioStateIs("hent barn 2")
                .willSetStateTo("hent barn 3")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn3ResponsFraFil))
                .whenScenarioStateIs("hent barn 3")
        );

        server.start();

        PdlPortefoljeClient pdlPortefoljeClient = new PdlPortefoljeClient(new PdlClientImpl("http://localhost:" + server.port(), () -> "SYSTEM_TOKEN"));
        barnUnder18AarService = new BarnUnder18AarService(new BarnUnder18AarRepository(db, db), pdlPortefoljeClient);

        this.pdlService = new PdlService(
                new PdlIdentRepository(db),
                new PdlPersonRepository(db, db),
                this.barnUnder18AarService,
                pdlPortefoljeClient
        );
    }

    @AfterEach
    public void stopServer(){
        server.stop();
    }

    @Test
    @SneakyThrows
    public void hentOgLagrePdlData() {
        var identerFraFil = mapper.readValue(pdlIdentResponsFraFil, PdlIdentResponse.class)
                .getData()
                .getHentIdenter()
                .getIdenter();

        pdlService.hentOgLagrePdlData(randomAktorId());
        List<PDLIdent> identerFraPostgres = db.queryForList("select * from bruker_identer")
                .stream()
                .map(PdlIdentRepository::mapTilident)
                .toList();
        Fnr fnr = hentAktivFnr(identerFraFil);
        PDLPerson pdlPerson = pdlPersonRepository.hentPerson(fnr);
        List<BarnUnder18AarData> barnFraRepository = barnUnder18AarService.hentBarnUnder18Aar(List.of(fnr)).get(fnr);
        List<Fnr> foreldreansvar = barnUnder18AarService.hentBarnFnrsForForeldre(List.of(fnr));

        assertThat(identerFraPostgres).containsExactlyInAnyOrderElementsOf(identerFraFil);
        assertThat(pdlPerson.getFornavn()).isEqualTo("Dogmatisk");
        assertThat(pdlPerson.getEtternavn()).isEqualTo("Budeie");
        assertThat(pdlPerson.getFoedsel().toString()).isEqualTo("1991-12-30");
        assertThat(pdlPerson.getFoedeland()).isEqualTo("UKR");
        Assertions.assertEquals(2, pdlPerson.getStatsborgerskap().size());
        assertTrue(pdlPerson.getStatsborgerskap().stream().anyMatch(x -> x.getStatsborgerskap().equals("UKR")));
        assertTrue(pdlPerson.getStatsborgerskap().stream().anyMatch(x -> x.getGyldigFra().toString().equals("1991-12-30")));
        assertEquals("UK", pdlPerson.getTalespraaktolk());
        assertEquals("2022-06-02", pdlPerson.getTolkBehovSistOppdatert().toString());
        assertEquals("7", pdlPerson.getDiskresjonskode());
        assertEquals("FYUS", pdlPerson.getSikkerhetstiltak().getTiltakstype());
        assertEquals("Fysisk utestengelse", pdlPerson.getSikkerhetstiltak().getBeskrivelse());
        assertEquals("2022-05-12", pdlPerson.getSikkerhetstiltak().getGyldigFra().toString());
        assertEquals("2022-08-05", pdlPerson.getSikkerhetstiltak().getGyldigTil().toString());
        assertThat(foreldreansvar.size()).isEqualTo(3);

        Integer barnAlder1 = DateUtils.alderFraFodselsdato(DateUtils.toLocalDateOrNull(hentFodselsdatoBarn1()));
        Integer barnAlder2 = DateUtils.alderFraFodselsdato(DateUtils.toLocalDateOrNull(hentFodselsdatoBarn2()));
        assertTrue(barnFraRepository.stream().map(BarnUnder18AarData::getAlder).toList().containsAll(List.of(barnAlder1, barnAlder2)));
    }

    public String hentFodselsdatoBarn1() throws JsonProcessingException {
        return mapper.readValue(pdlPersonBarn1ResponsFraFil, PdlBarnResponse.class)
                .getData()
                .getHentPerson()
                .getFoedsel()
                .get(0)
                .getFoedselsdato();
    }

    public String hentFodselsdatoBarn2() throws JsonProcessingException {
        return mapper.readValue(pdlPersonBarn2ResponsFraFil, PdlBarnResponse.class)
                .getData()
                .getHentPerson()
                .getFoedsel()
                .get(0)
                .getFoedselsdato();
    }
}



