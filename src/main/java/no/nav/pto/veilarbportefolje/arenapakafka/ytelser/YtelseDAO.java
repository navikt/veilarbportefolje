package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class YtelseDAO {
    AktorId aktorId;
    PersonId personId;
    String saksId;
    TypeKafkaYtelse type;

    String sakstypeKode;
    String rettighetstypeKode;

    Timestamp startDato;
    Timestamp utlopsDato;
    Integer antallUkerIgjen;
    Integer antallUkerIgjenPermittert;
    Integer antallDagerIgjenUnntak;
}
