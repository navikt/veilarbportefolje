package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TiltakInnhold implements ArenaInnholdKafka {
    @JsonAlias("TILTAKDELTAKER_ID") String aktivitetIdNr;
    @JsonAlias("AKTIVITETID") String aktivitetid;

    @JsonAlias("TILTAKSTYPE") String tiltakstype;
    @JsonAlias("TILTAKSNAVN") String tiltaksnavn;
    @JsonAlias("DELTAKERSTATUS") String deltakerStatus;
    @JsonAlias("DELTAKERSTATUSNAVN") String deltakerStatusNavn;
    @JsonAlias("DELTAKERPERIODE_FOM") ArenaDato aktivitetperiodeFra;
    @JsonAlias("DELTAKERPERIODE_TOM") ArenaDato aktivitetperiodeTil;
    @JsonAlias("PERSON_ID") int personId;
    @JsonAlias("PERSONIDENT") String fnr; // eller dnr
    @JsonAlias("HENDELSE_ID") long hendelseId;
    @JsonAlias("OPPRETTET_DATO") ArenaDato opprettetDato;
    @JsonAlias("OPPRETTET_AV") String opprettetAv;
    @JsonAlias("ENDRET_DATO") ArenaDato endretDato;
    @JsonAlias("ENDRET_AV") String endretAv;
}