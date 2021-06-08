package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class GruppeAktivitetInnhold implements ArenaInnholdKafka {
    @JsonAlias("VEILEDNINGDELTAKER_ID") int veiledningdeltakerId;
    @JsonAlias("MOTEPLAN_ID") int moteplanId;

    @JsonAlias("AKTIVITET_ID") long aktivitetIdNr;
    @JsonAlias("VEILEDNINGAKTIVITET_ID") int veiledningAktivitetId;
    @JsonAlias("AKTIVITETID") String aktivitetid;

    @JsonAlias("AKTIVITETSTYPE") String aktivitetstype;
    @JsonAlias("AKTIVITETSNAVN") String aktivitetsnavn;
    @JsonAlias("MOTEPLAN_STARTDATO") LocalDate aktivitetperiodeFra;
    @JsonAlias("MOTEPLAN_SLUTTDATO") LocalDate aktivitetperiodeTil;
    @JsonAlias("PERSON_ID") int personId;
    @JsonAlias("PERSONIDENT") String fnr; // eller dnr
    @JsonAlias("HENDELSE_ID") long hendelseId;
    @JsonAlias("OPPRETTET_DATO") LocalDate opprettetDato;
    @JsonAlias("OPPRETTET_AV") String opprettetAv;
    @JsonAlias("ENDRET_DATO") LocalDate endretDato;
    @JsonAlias("ENDRET_AV") String endretAv;
}