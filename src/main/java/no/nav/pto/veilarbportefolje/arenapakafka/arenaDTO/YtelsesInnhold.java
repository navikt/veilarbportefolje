package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;

@Data
public class YtelsesInnhold implements ArenaInnholdKafka {
    @JsonAlias("VEDTAK_ID") String vedtakId; // Denne har verdi NUMBER i Arena er da bruk av VARCHER(20) bra nok?
    @JsonAlias("SAK_ID") String saksId;
    @JsonAlias("PERSON_ID") String personId;
    @JsonAlias("PERSONIDENT") String fnr; // eller dnr
    @JsonAlias("HENDELSE_ID") long hendelseId;
    @JsonAlias("SAKSTYPEKODE") String sakstypeKode;
    @JsonAlias("RETTIGHETSTYPEKODE") String rettighetstypeKode;

    @JsonAlias("VEDTAKSPERIODE_FOM") ArenaDato fraOgMedDato;
    @JsonAlias("VEDTAKSPERIODE_TOM") ArenaDato tilOgMedDato;

    @JsonAlias("ANTALLUKERIGJEN") Integer antallUkerIgjen;
    @JsonAlias("ANTALLDAGERIGJEN") Integer antallDagerIgjen;
    @JsonAlias("ANTALLUKERIGJENPERMITTERING") Integer antallUkerIgjenUnderPermittering;
    @JsonAlias("ANTALLDAGERIGJENPERMITTERING") Integer antallDagerIgjenUnderPermittering;

    @JsonAlias("ANTALLDAGERIGJENUNNTAK") Integer antallDagerIgjenUnntak;
}
