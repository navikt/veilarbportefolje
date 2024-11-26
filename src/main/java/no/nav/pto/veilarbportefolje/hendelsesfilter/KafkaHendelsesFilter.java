package no.nav.pto.veilarbportefolje.hendelsesfilter;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.hendelsesfilter.domain.Hendelse;
import no.nav.pto.veilarbportefolje.hendelsesfilter.domain.Kategori;
import no.nav.pto.veilarbportefolje.hendelsesfilter.domain.Operasjon;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.KafkaTiltakshendelse;

import java.util.UUID;

public record KafkaHendelsesFilter(
        UUID id;
        String personID;
        String avsender;
        Kategori kategori;
        Operasjon operasjon;
        Hendelse hendelse;
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KafkaHendelsesFilter {
    }

    public static  KafkaHendelsesFilter mapHendelsesFilter(KafkaHendelsesFilter hendelse) {
        return new KafkaHendelsesFilter(hendelse.id, hendelse.personID, hendelse.avsender, hendelse.kategori, hendelse.operasjon, hendelse.hendelse);
    }
}
