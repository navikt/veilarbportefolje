package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;

public class YtelseDagpengerDTO extends YtelsesInnhold{
    @JsonAlias("ANTALLUKERIGJEN") Integer antallUkerIgjen;
    @JsonAlias("ANTALLDAGERIGJEN") Integer antallDagerIgjen;
    @JsonAlias("ANTALLUKERIGJENPERMITTERING") Integer antallUkerIgjenUnderPermittering;
    @JsonAlias("ANTALLDAGERIGJENPERMITTERING") Integer antallDagerIgjenUnderPermittering;
}