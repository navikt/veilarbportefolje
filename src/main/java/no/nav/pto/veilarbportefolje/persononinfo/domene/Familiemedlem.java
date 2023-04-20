package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Bostedsadresse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Matrikkeladresse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Vegadresse;

import java.time.LocalDate;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.RelasjonsBosted.*;

@Data
@Accessors(chain = true)
public class Familiemedlem {
    Fnr fnr;
    LocalDate fodselsdato;
    String gradering;       //diskresjonskode
    RelasjonsBosted relasjonsBosted;

    public static Familiemedlem of(Fnr barnFrn, PDLPersonBarn pdlPersonBarn, Bostedsadresse foreldreBostedadresse) {
        if (pdlPersonBarn.isErIlive()) {
            return new Familiemedlem()
                    .setFnr(barnFrn)
                    .setGradering(pdlPersonBarn.getDiskresjonskode())
                    .setFodselsdato(pdlPersonBarn.getFodselsdato())
                    .setRelasjonsBosted(hentRelajsonsBosted(foreldreBostedadresse, pdlPersonBarn.getBostedsadresse()));
        }
        return null;
    }

    private static RelasjonsBosted hentRelajsonsBosted(Bostedsadresse foreldreBostedadresse, Bostedsadresse barnBostedAdresse) {

        Vegadresse foreldreVegadresse = ofNullable(foreldreBostedadresse)
                .map(Bostedsadresse::getVegadresse).orElse(null);
        Matrikkeladresse foreldreMetrikkeladresse = ofNullable(foreldreBostedadresse)
                .map(Bostedsadresse::getMatrikkeladresse).orElse(null);

        Vegadresse barnVegadresse = ofNullable(barnBostedAdresse)
                .map(Bostedsadresse::getVegadresse).orElse(null);
        Matrikkeladresse barnMetrikkeladresse = ofNullable(barnBostedAdresse)
                .map(Bostedsadresse::getMatrikkeladresse).orElse(null);

        if (foreldreVegadresse != null && barnVegadresse != null) {
            if (Objects.equals(foreldreVegadresse, barnVegadresse)) {
                return SAMME_BOSTED;
            } else {
                return ANNET_BOSTED;
            }
        } else if (foreldreMetrikkeladresse != null && barnMetrikkeladresse != null) {
            if (Objects.equals(foreldreMetrikkeladresse, barnMetrikkeladresse)) {
                return SAMME_BOSTED;
            } else {
                return ANNET_BOSTED;
            }
        }

        return UKJENT_BOSTED;
    }
}