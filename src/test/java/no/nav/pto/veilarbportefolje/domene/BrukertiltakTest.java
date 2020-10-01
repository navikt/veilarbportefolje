package no.nav.pto.veilarbportefolje.domene;

import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Bruker;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Gruppeaktivitet;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Tiltaksaktivitet;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Utdanningsaktivitet;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BrukertiltakTest {

    @Test
    public void skalBarereturnereTiltaksaktiviteter() {
        Bruker bruker = new Bruker();
        bruker.setPersonident("00000000000");
        List<Tiltaksaktivitet> tiltaksaktiviteter = bruker.getTiltaksaktivitetListe();
        List<Gruppeaktivitet> gruppeaktiviteter = bruker.getGruppeaktivitetListe();
        List<Utdanningsaktivitet> utdanningsaktiviteter = bruker.getUtdanningsaktivitetListe();

        Tiltaksaktivitet tiltaksaktivitet = new Tiltaksaktivitet();
        tiltaksaktivitet.setTiltakstype("TILTAKSAKTIVITET");

        Utdanningsaktivitet utdanningsaktivitet = new Utdanningsaktivitet();
        utdanningsaktivitet.setAktivitetstype("UTDANNINGSAKTIVITET");

        Gruppeaktivitet gruppeaktivitet = new Gruppeaktivitet();
        gruppeaktivitet.setAktivitetstype("GRUPPEAKTIVITET");

        tiltaksaktiviteter.add(tiltaksaktivitet);
        utdanningsaktiviteter.add(utdanningsaktivitet);
        gruppeaktiviteter.add(gruppeaktivitet);

        List<Brukertiltak> brukertiltak = Brukertiltak.of(bruker);
        assertThat(brukertiltak.size()).isEqualTo(1);
    }
}
