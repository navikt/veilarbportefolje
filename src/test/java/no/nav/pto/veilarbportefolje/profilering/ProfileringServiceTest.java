package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.profilering.ProfilertTil;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProfileringServiceTest {

    JdbcTemplate jdbcTemplate = new JdbcTemplate(setupInMemoryDatabase());
    private ProfileringRepository profileringRepository = new ProfileringRepository(jdbcTemplate);
    private static String AKTORID = "123456789";
    private ElasticIndexer elasticMock = mock(ElasticIndexer.class);
    private ProfileringService profileringService;

    @Before
    public void setup() {
        this.profileringService = new ProfileringService(profileringRepository, elasticMock);
    }

    @Test
    public void skallSetteInOppdateringerFraKafka () {
        jdbcTemplate.update( "INSERT INTO BRUKER_PROFILERING " +
                "(AKTOERID, PROFILERING_RESULTAT, PROFILERING_TIDSPUNKT) " +
                "VALUES " +
                "(?, ?, ?)", AKTORID, "ANTATT_GODE_MULIGHETER", "1970-01-01 00:00:00.000");


        ArbeidssokerProfilertEvent event = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.of(LocalDateTime.now().minusDays(30), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        profileringService.behandleKafkaMelding(event);
        assertThat(profileringRepository.hentBrukerProfilering(AktoerId.of(AKTORID))).isEqualTo(event);
    }


    @Test
    public void skallOppdatereProfileringHvisNyareVerdi () {
        ArbeidssokerProfilertEvent event = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.of(LocalDateTime.now().minusDays(30), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        ArbeidssokerProfilertEvent event2 = ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setProfilertTil(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)
                .setProfileringGjennomfort(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        profileringService.behandleKafkaMelding(event);
        profileringService.behandleKafkaMelding(event2);
        assertThat(profileringRepository.hentBrukerProfilering(AktoerId.of(AKTORID))).isEqualTo(event2);
    }






}
