package no.nav.pto.veilarbportefolje.arbeidssokerregistrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.arbeid.soker.registrering.UtdanningBestattSvar;
import no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar;
import no.nav.arbeid.soker.registrering.UtdanningSvar;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v1.registrering.ArbeidssokerRegistreringRepositoryV2;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class ArbeidssokerRegistreringRepositoryV2Test {
    @Autowired
    private ArbeidssokerRegistreringRepositoryV2 arbeidssokerRegistreringRepositoryV2;
    private final static String AKTORID = randomAktorId().get();

    @Test
    public void skallSetteInBrukerSituasjon() {
        ArbeidssokerRegistrertEvent event = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(DateUtils.nowToStr())
                .build();

        arbeidssokerRegistreringRepositoryV2.upsertBrukerRegistrering(event);

        Optional<ArbeidssokerRegistrertEvent> registrering = arbeidssokerRegistreringRepositoryV2.hentBrukerRegistrering(AktorId.of(AKTORID));
        assertThat(registrering.orElseThrow(IllegalStateException::new)).isEqualTo(event);
    }

    @Test
    public void skallOppdatereBrukerSituasjon() {
        ArbeidssokerRegistrertEvent event1 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(DateUtils.now().minusDays(4).format(ISO_ZONED_DATE_TIME))
                .build();
        ArbeidssokerRegistrertEvent event2 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Hjemmekontor")
                .setUtdanning(UtdanningSvar.HOYERE_UTDANNING_1_TIL_4)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.NEI)
                .setRegistreringOpprettet(DateUtils.nowToStr())
                .build();

        arbeidssokerRegistreringRepositoryV2.upsertBrukerRegistrering(event1);
        arbeidssokerRegistreringRepositoryV2.upsertBrukerRegistrering(event2);

        Optional<ArbeidssokerRegistrertEvent> registrering = arbeidssokerRegistreringRepositoryV2.hentBrukerRegistrering(AktorId.of(AKTORID));
        assertThat(registrering.orElseThrow(IllegalStateException::new)).isEqualTo(event2);
    }

    @Test
    public void skallOppdatereUtdanning() {
        ArbeidssokerRegistrertEvent event1 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.JA)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(DateUtils.now().minusDays(4).format(ISO_ZONED_DATE_TIME))
                .build();
        ArbeidssokerRegistrertEvent event2 = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID)
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.HOYERE_UTDANNING_5_ELLER_MER)
                .setUtdanningBestatt(UtdanningBestattSvar.NEI)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.INGEN_SVAR)
                .setRegistreringOpprettet(DateUtils.nowToStr())
                .build();

        arbeidssokerRegistreringRepositoryV2.upsertBrukerRegistrering(event1);
        arbeidssokerRegistreringRepositoryV2.upsertBrukerRegistrering(event2);

        Optional<ArbeidssokerRegistrertEvent> registrering = arbeidssokerRegistreringRepositoryV2.hentBrukerRegistrering(AktorId.of(AKTORID));
        assertThat(registrering.orElseThrow(IllegalStateException::new)).isEqualTo(event2);
    }

}
