package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class GruppeAktivitetInnhold implements ArenaInnholdKafka {
    @JsonAlias("VEILEDNINGDELTAKER_ID") String veiledningdeltakerId;
    @JsonAlias("MOTEPLAN_ID") String moteplanId;

    @JsonAlias("AKTIVITET_ID") String aktivitetIdNr;
    @JsonAlias("VEILEDNINGAKTIVITET_ID") int veiledningAktivitetId;
    @JsonAlias("AKTIVITETID") String aktivitetid;

    @JsonAlias("AKTIVITETSTYPE") String aktivitetstype;
    @JsonAlias("AKTIVITETSNAVN") String aktivitetsnavn;
    @JsonAlias("MOTEPLAN_STARTDATO") ArenaDato aktivitetperiodeFra;
    @JsonAlias("MOTEPLAN_SLUTTDATO") ArenaDato aktivitetperiodeTil;
    @JsonAlias("PERSON_ID") int personId;
    @JsonAlias("PERSONIDENT") String fnr; // eller dnr
    @JsonAlias("HENDELSE_ID") long hendelseId;
    @JsonAlias("OPPRETTET_DATO") ArenaDato opprettetDato;
    @JsonAlias("OPPRETTET_AV") String opprettetAv;
    @JsonAlias("ENDRET_DATO") ArenaDato endretDato;
    @JsonAlias("ENDRET_AV") String endretAv;
}