package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class Brukerdata {
    private String aktoerid;
    private String personid;
    private YtelseMapping ytelse;
    private LocalDateTime utlopsdato;
    private Integer dagputlopUke;
    private Integer permutlopUke;
    private Integer aapmaxtidUke;
    private Integer aapUnntakDagerIgjen;
    private Timestamp nyesteUtlopteAktivitet;
    private Timestamp aktivitetStart;
    private Timestamp nesteAktivitetStart;
    private Timestamp forrigeAktivitetStart;
}
