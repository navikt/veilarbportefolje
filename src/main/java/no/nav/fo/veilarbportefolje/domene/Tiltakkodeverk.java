package no.nav.fo.veilarbportefolje.domene;

import lombok.Value;
import lombok.experimental.Wither;
import no.nav.fo.veilarbportefolje.util.sql.InsertBatchQuery;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Aktivitetstyper;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Tiltakstyper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Value(staticConstructor = "of")
@Wither
public class Tiltakkodeverk {
    private String kode;
    private String verdi;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Tiltakkodeverk that = (Tiltakkodeverk) o;

        return kode.toLowerCase().equals(that.kode.toLowerCase());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + kode.hashCode();
        return result;
    }

    public static Tiltakkodeverk of(Tiltakstyper tiltakstyper) {
        return Tiltakkodeverk.of(tiltakstyper.getValue(), tiltakstyper.getTermnavn());
    }

    public static Tiltakkodeverk of(Aktivitetstyper aktivitetstyper) {
        return Tiltakkodeverk.of(aktivitetstyper.getValue(), aktivitetstyper.getTermnavn());
    }

    public static int[] batchInsert(JdbcTemplate db, List<Tiltakkodeverk> data) {
        InsertBatchQuery<Tiltakkodeverk> insertQuery = new InsertBatchQuery<>(db, "tiltakkodeverk");

        return insertQuery
                .add("kode", Tiltakkodeverk::getKode, String.class)
                .add("verdi", Tiltakkodeverk::getVerdi, String.class)
                .execute(data);
    }
}
