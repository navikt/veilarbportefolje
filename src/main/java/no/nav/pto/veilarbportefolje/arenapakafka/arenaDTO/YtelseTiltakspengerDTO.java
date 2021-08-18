package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;

public class YtelseTiltakspengerDTO extends YtelsesInnhold {
    @JsonAlias("VEDTAKID") String vedtakId;
    @JsonAlias("PERSON_ID") String personId;
    @JsonAlias("PERSONIDENT") String fnr; // eller dnr
    @JsonAlias("HENDELSE_ID") long hendelseId;
    @JsonAlias("SAKSTYPEKODE") String sakstypeKode;
    @JsonAlias("RETTIGHETSTYPEKODE") String rettighetstypeKode;

    @JsonAlias("VEDTAKSPERIODE_FOM") ArenaDato fraOgMedDato;
    @JsonAlias("VEDTAKSPERIODE_TOM") ArenaDato tilOgMedDato;
}
