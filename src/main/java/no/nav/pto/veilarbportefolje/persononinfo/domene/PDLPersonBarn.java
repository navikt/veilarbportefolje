package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PDLPersonBarnBolk;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.AdressebeskyttelseDto;

import java.time.LocalDate;
import java.util.*;

@Data
@Slf4j
@Accessors(chain = true)
public class PDLPersonBarn {
    private LocalDate fodselsdato;
    private String diskresjonskode;

    private boolean erIlive;

    public static PDLPersonBarn genererFraApiRespons(PdlBarnResponse.PdlBarnResponseData.HentPersonResponsData response) {
        PDLPersonBarn barn = new PDLPersonBarn();
        barn.setFodselsdato(hentFodselsdato(response.getFoedsel()));
        barn.setErIlive(hentErILive(response.getDoedsfall()));
        barn.setDiskresjonskode(hentDiskresjonkode(response.getAdressebeskyttelse()));
        return barn;
    }

    public static Map<Fnr, PDLPersonBarn> genererFraApiRespons(PDLPersonBarnBolk.PdlBarnResponseData responseDataBolk) {
        Map<Fnr, PDLPersonBarn> barn = new HashMap<>();

        if (responseDataBolk == null || responseDataBolk.getHentPersonBolk() == null || responseDataBolk.getHentPersonBolk().isEmpty()){
            return Collections.emptyMap();
        }
        responseDataBolk.getHentPersonBolk().forEach(barnDataBolk -> {
            if (barnDataBolk.getCode().equals("ok")){
                barn.put(Fnr.of(barnDataBolk.getIdent()), genererFraApiRespons(barnDataBolk.getPerson()));
            }
        });

        return barn;
    }


    private static boolean hentErILive(List<PdlBarnResponse.PdlBarnResponseData.Doedsfall> doedsfall) {
        if (doedsfall == null || doedsfall.isEmpty()) {
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
