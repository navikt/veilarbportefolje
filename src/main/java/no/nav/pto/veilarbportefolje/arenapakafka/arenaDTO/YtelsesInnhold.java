package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;

@Data
public class YtelsesInnhold implements ArenaInnholdKafka {
    @JsonAlias("VEDTAKID") String vedtakId;
    @JsonAlias("PERSON_ID") String personId;
    @JsonAlias("PERSONIDENT") String fnr; // eller dnr

    @JsonAlias("SAKSTYPEKODE") String sakstypeKode;
    @JsonAlias("RETTIGHETSTYPEKODE") String rettighetstypeKode;
    @JsonAlias("VEDTAKSPERIODE") Periode vedtaksperiode;
    @JsonAlias("DAGPENGETELLERE") Dagpengetellere dagpengetellere;
    @JsonAlias("AAPTELLERE") Aaptellere aaptellere;
    @JsonAlias("HENDELSE_ID") long hendelseId;

    @Data
    public static class Periode {
        @JsonAlias("fom") ArenaDato fraDato;
        @JsonAlias("tom") ArenaDato tilogMedDato;
    }

    @Data
    public static class Dagpengetellere {
        Integer antallUkerIgjen;
        Integer antallDagerIgjen;
        Integer antallUkerIgjenUnderPermittering;
        Integer antallDagerIgjenUnderPermittering;
    }

    @Data
    public static class Aaptellere {
        Integer antallUkerIgjen;
        Integer antallDagerIgjen;
        Integer antallDagerIgjenUnntak;
    }
}
