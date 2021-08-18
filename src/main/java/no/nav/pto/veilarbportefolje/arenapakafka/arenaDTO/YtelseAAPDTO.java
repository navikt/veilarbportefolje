package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;

public class YtelseAAPDTO extends YtelsesInnhold{
    @JsonAlias("ANTALLUKERIGJEN") Integer antallUkerIgjen;
    @JsonAlias("ANTALLDAGERIGJEN") Integer antallDagerIgjen;
    @JsonAlias("ANTALLDAGERIGJENUNNTAK") Integer antallDagerIgjenUnntak;
}
