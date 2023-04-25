package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Bostedsadresse;

import java.time.LocalDate;
import java.util.List;

@Data
@Slf4j
@Accessors(chain = true)
public class PDLPersonBarn {
    private LocalDate fodselsdato;
    private String gradering;

    private boolean erLiv;
    private Bostedsadresse bostedsadresse;

    public static PDLPersonBarn genererFraApiRespons(PdlBarnResponse.PdlBarnResponseData.HentPersonResponsData response) {
        PDLPersonBarn barn = new PDLPersonBarn();

        barn.setBostedsadresse(hentBostedAdresse(response.getBostedsadresse()));
        barn.setFodselsdato(hentFodselsdato(response.getFoedsel()));
        barn.setErLiv()

        return barn;
    }

    private static LocalDate hentFodselsdato(List<PdlBarnResponse.PdlBarnResponseData.Foedsel> foedsel) {
        if (foedsel == null) {
            return null;
        }
        return foedsel.stream()
                .filter(foedsel1 -> !foedsel1.getMetadata().isHistorisk())
                .findFirst()
                .map(PdlBarnResponse.PdlBarnResponseData.Foedsel::getFoedselsdato)
                .map(LocalDate::parse)
                .orElse(null);
    }

    private static Bostedsadresse hentBostedAdresse(List<Bostedsadresse> response) {
        if (response == null) {
            return null;
        }
        return response.stream()
                .filter(bostedsadresse -> !bostedsadresse.getMetadata().isHistorisk())
                .findFirst()
                .orElse(null);
    }

}
