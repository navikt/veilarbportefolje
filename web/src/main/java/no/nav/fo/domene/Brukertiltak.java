package no.nav.fo.domene;

import lombok.Value;
import lombok.experimental.Wither;
import no.nav.fo.util.sql.InsertBatchQuery;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Bruker;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Tiltaksaktivitet;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.filmottak.tiltak.TiltakUtils.utledTildato;

@Value(staticConstructor = "of")
@Wither
public class Brukertiltak {
    private Fnr fnr;
    private String tiltak;
    private Timestamp tildato;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Brukertiltak that = (Brukertiltak) o;

        if (!fnr.equals(that.fnr)) return false;
        return tiltak.equals(that.tiltak);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + fnr.hashCode();
        result = 31 * result + tiltak.hashCode();
        return result;
    }

    public static Brukertiltak of(Tiltaksaktivitet tiltaksaktivitet, String fnr) {
        return new Brukertiltak(Fnr.of(fnr), tiltaksaktivitet.getTiltakstype(),utledTildato(tiltaksaktivitet.getDeltakelsePeriode()));
    }

    public static List<Brukertiltak> of(Bruker bruker) {
        String fnr = bruker.getPersonident();
        return bruker.getTiltaksaktivitetListe().stream().map(tiltak -> of(tiltak, fnr)).collect(toList());
    }

    public static int[] batchInsert(JdbcTemplate db, List<Brukertiltak> data) {
        InsertBatchQuery<Brukertiltak> insertQuery = new InsertBatchQuery<>(db, "BRUKERTILTAK");

        return insertQuery
                .add("FODSELSNR", bruker -> bruker.getFnr().toString(), String.class)
                .add("TILTAKSKODE", Brukertiltak::getTiltak, String.class)
                .add("TILDATO", Brukertiltak::getTildato, Timestamp.class)
                .execute(data);
    }


}
