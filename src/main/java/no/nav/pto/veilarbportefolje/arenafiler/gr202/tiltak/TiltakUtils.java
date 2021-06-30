package no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.aktiviteter.UtdanningaktivitetTyper;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.etterFilterDato;
import static no.nav.pto.veilarbportefolje.util.DbUtils.not;

public class TiltakUtils {

    private static Timestamp tilTimestamp(XMLGregorianCalendar calendar) {
        return Optional.ofNullable(calendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(GregorianCalendar::getTime)
                .map(Date::getTime)
                .map(Timestamp::new)
                .orElse(null);
    }

    public static Optional<Timestamp> utledTildato(Periode periode) {
        return Optional.ofNullable(periode)
                .map(Periode::getTom)
                .map(TiltakUtils::tilTimestamp);
    }
}
