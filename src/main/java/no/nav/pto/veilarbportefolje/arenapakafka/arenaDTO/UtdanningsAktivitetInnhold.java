package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;

@Data
@Accessors(chain = true)
public class UtdanningsAktivitetInnhold implements ArenaInnholdKafka {
    @JsonAlias("AKTIVITET_ID") String aktivitetIdNr;
    @JsonAlias("AKTIVITETID") String aktivitetid;

    @JsonAlias("AKTIVITETSTYPE") String aktivitetstype;
    @JsonAlias("AKTIVITETSNAVN") String aktivitetsnavn;
    @JsonAlias("AKTIVITETPERIODE_FOM") ArenaDato aktivitetperiodeFra;
    @JsonAlias("AKTIVITETPERIODE_TOM") ArenaDato aktivitetperiodeTil;
    @JsonAlias("PERSON_ID") int personId;
    @JsonAlias("PERSONIDENT") String fnr; // eller dnr
    @JsonAlias("HENDELSE_ID") long hendelseId;
    @JsonAlias("OPPRETTET_DATO") ArenaDato opprettetDato;
    @JsonAlias("OPPRETTET_AV") String opprettetAv;
    @JsonAlias("ENDRET_DATO") ArenaDato endretDato;
    @JsonAlias("ENDRET_AV") String endretAv;
}