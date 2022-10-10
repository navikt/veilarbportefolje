package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class Utkast14aStatusRepositoryTest {

    private Utkast14aStatusRepository utkast14aStatusRepository;

    private static String AKTORID = "123456789";
    private static long VEDTAKID = 1;
    private static final String VEILEDER_IDENT = "Z1234";
    private static final String VEILEDER_NAVN = "Veileder 1234";

    @Before
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        this.utkast14aStatusRepository = new Utkast14aStatusRepository(db);
        utkast14aStatusRepository.slettUtkastForBruker(AKTORID);
    }

    @Test
    public void skallSetteInVedtak() {
        insertVedtakIDB();
        Optional<Kafka14aStatusendring> endringer = utkast14aStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer.isPresent()).isTrue();
    }

    @Test
    public void skallOppretteVedtak() {
        LocalDateTime time = LocalDateTime.now().withNano(0);
        Kafka14aStatusendring vedtakStatusEndring = new Kafka14aStatusendring()
                .setVedtakStatusEndring(Kafka14aStatusendring.Status.KLAR_TIL_BESLUTTER)
                .setHovedmal(Kafka14aStatusendring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setTimestamp(time)
                .setAktorId(AKTORID)
                .setVeilederNavn(VEILEDER_NAVN)
                .setVeilederIdent(VEILEDER_IDENT)
                .setVedtakId(VEDTAKID);

        utkast14aStatusRepository.upsert(vedtakStatusEndring);
        Optional<Kafka14aStatusendring> endringer = utkast14aStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer.isPresent()).isTrue();
        assertThat(endringer.get().veilederIdent).isEqualTo(VEILEDER_IDENT);
        assertThat(endringer.get().veilederNavn).isEqualTo(VEILEDER_NAVN);
        assertThat(endringer.get().aktorId).isEqualTo(AKTORID);
        assertThat(endringer.get().vedtakId).isEqualTo(VEDTAKID);
        assertThat(endringer.get().getTimestamp()).isEqualTo(time);
    }

    @Test
    public void skallOppdatereVedtakMedAnsvarligVeileder() {
        insertVedtakIDB();

        Optional<Kafka14aStatusendring> endringer1 = utkast14aStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer1.isPresent()).isTrue();
        assertThat(endringer1.get().veilederIdent).isEqualTo(null);
        assertThat(endringer1.get().veilederNavn).isEqualTo(null);

        Kafka14aStatusendring vedtakStatusEndring = new Kafka14aStatusendring()
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setVeilederNavn(VEILEDER_NAVN)
                .setVeilederIdent(VEILEDER_IDENT);

        utkast14aStatusRepository.oppdaterAnsvarligVeileder(vedtakStatusEndring);

        Optional<Kafka14aStatusendring> endringer2 = utkast14aStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer2.isPresent()).isTrue();
        assertThat(endringer2.get().veilederIdent).isEqualTo(VEILEDER_IDENT);
        assertThat(endringer2.get().veilederNavn).isEqualTo(VEILEDER_NAVN);
    }

    @Test
    public void skallSletteVedtak() {
        insertVedtakIDB();
        utkast14aStatusRepository.slettUtkastForBruker(AKTORID);
        Optional<Kafka14aStatusendring> endringer = utkast14aStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer.isEmpty()).isTrue();
    }
    @Test
    public void skallOppdatereVedtak() {
        insertVedtakIDB();

        LocalDateTime time = LocalDateTime.now().withNano(0);
        Kafka14aStatusendring vedtakStatusEndring = new Kafka14aStatusendring()
                .setVedtakStatusEndring(Kafka14aStatusendring.Status.KLAR_TIL_BESLUTTER)
                .setHovedmal(Kafka14aStatusendring.Hovedmal.SKAFFE_ARBEID)
                .setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
                .setTimestamp(time)
                .setAktorId(AKTORID)
                .setVeilederNavn(VEILEDER_NAVN)
                .setVeilederIdent(VEILEDER_IDENT)
                .setVedtakId(VEDTAKID);

        utkast14aStatusRepository.update(vedtakStatusEndring);
        Optional<Kafka14aStatusendring> endringer = utkast14aStatusRepository.hentStatusEndringForBruker(AKTORID);
        assertThat(endringer.isPresent()).isTrue();
        assertThat(endringer.get().veilederIdent).isEqualTo(null); //TODO: finn ut om dette er riktig.
        assertThat(endringer.get().veilederNavn).isEqualTo(null);  // her som i den "gamle" versjonen oppdaters dette og forrige felt kun i funksjonen oppdaterAnsvarligVeileder()
        assertThat(endringer.get().aktorId).isEqualTo(AKTORID);
        assertThat(endringer.get().vedtakId).isEqualTo(VEDTAKID);
        assertThat(endringer.get().getTimestamp()).isEqualTo(time);
    }


    private void insertVedtakIDB() {
        LocalDateTime time = LocalDateTime.now().withNano(0);
        Kafka14aStatusendring vedtakStatusEndring = new Kafka14aStatusendring()
                .setVedtakStatusEndring(Kafka14aStatusendring.Status.UTKAST_OPPRETTET)
                .setTimestamp(time)
                .setAktorId(AKTORID)
                .setVedtakId(VEDTAKID)
                .setHovedmal(null)
                .setInnsatsgruppe(null)
                .setVeilederNavn(null)
                .setVeilederIdent(null);
        utkast14aStatusRepository.upsert(vedtakStatusEndring);
    }
}
