package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class UtdanningsAktivitetInnhold implements ArenaInnholdKafka {
    @JsonAlias("AKTIVITET_ID") long aktivitetIdNr;
    @JsonAlias("AKTIVITETID") String aktivitetid;

    @JsonAlias("AKTIVITETSTYPE") String aktivitetstype;
    @JsonAlias("AKTIVITETSNAVN") String aktivitetsnavn;
    @JsonAlias("AKTIVITETPERIODE_FOM") Date aktivitetperiodeFra;
    @JsonAlias("AKTIVITETPERIODE_TOM") Date aktivitetperiodeTil;
    @JsonAlias("PERSON_ID") int personId;
    @JsonAlias("PERSONIDENT") String fnr; // eller dnr
    @JsonAlias("HENDELSE_ID") long hendelseId;
    @JsonAlias("OPPRETTET_DATO") Date opprettetDato;
    @JsonAlias("OPPRETTET_AV") String opprettetAv;
    @JsonAlias("ENDRET_DATO") Date endretDato;
    @JsonAlias("ENDRET_AV") String endretAv;
}