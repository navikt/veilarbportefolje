package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;
import java.time.LocalDateTime;
import java.util.UUID;

public record TiltakshendelseForBruker(
        UUID id,
        LocalDateTime opprettet,
        String tekst,
        String lenke,
        Tiltakstype tiltakstype) {

    public static TiltakshendelseForBruker of(Tiltakshendelse tiltakshendelse) {
        if(tiltakshendelse != null){
            return new TiltakshendelseForBruker(tiltakshendelse.id(), tiltakshendelse.opprettet(), tiltakshendelse.tekst(), tiltakshendelse.lenke(), tiltakshendelse.tiltakstype());
        } else {
            return null;
        }
    }
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
     public TiltakshendelseForBruker {
     }
}
