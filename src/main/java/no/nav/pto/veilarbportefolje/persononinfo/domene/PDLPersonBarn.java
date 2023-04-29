package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.AdressebeskyttelseDto;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Bostedsadresse;

import java.time.LocalDate;
import java.util.List;

@Data
@Slf4j
@Accessors(chain = true)
public class PDLPersonBarn {
    private LocalDate fodselsdato;
    private String diskresjonskode;

    private boolean erIlive;
    private Bostedsadresse bostedsadresse;

    public static PDLPersonBarn genererFraApiRespons(PdlBarnResponse.PdlBarnResponseData.HentPersonResponsData response) {
        PDLPersonBarn barn = new PDLPersonBarn();

        barn.setBostedsadresse(hentBostedAdresse(response.getBostedsadresse()));
        barn.setFodselsdato(hentFodselsdato(response.getFoedsel()));
        barn.setErIlive(hentErILive(response.getDoedsfall()));
        barn.setDiskresjonskode(hentDiskresjonkode(response.getAdressebeskyttelse()));
        return barn;
    }

    private static boolean hentErILive(List<PdlBarnResponse.PdlBarnResponseData.Doedsfall> doedsfall) {
        if (doedsfall == null) {
            return true;
        }
        return doedsfall.stream()
                .noneMatch(d -> d.getDoedsdato() != null);
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

    private static String hentDiskresjonkode(List<AdressebeskyttelseDto> adressebeskyttelseDto) {
        if (adressebeskyttelseDto == null) {
            return null;
        }
        var adressebeskyttelseAktiv = adressebeskyttelseDto.stream().filter(x -> !x.getMetadata().isHistorisk()).toList();
        return adressebeskyttelseAktiv.stream().findFirst()
                .map(AdressebeskyttelseDto::getGradering)
                .map(Adressebeskyttelse::mapKodeTilTall)
                .orElse(null);
    }

}
