package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Data
@Slf4j
@Accessors(chain = true)
public class PDLPerson {
    private String fornavn;
    private String etternavn;
    private String mellomnavn;
    private Kjonn kjonn;
    private boolean erDoed;
    private LocalDate foedsel;

    public static PDLPerson genererFraApiRespons(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response) {
        PDLPerson person = new PDLPerson();
        PdlPersonResponse.PdlPersonResponseData.Navn navn = kontrollerResponseOgHentNavn(response.getNavn());

        return person.setFornavn(navn.getFornavn())
                .setEtternavn(navn.getEtternavn())
                .setMellomnavn(navn.getMellomnavn())
                .setErDoed(erDoed(response.getDoedsfall()))
                .setFoedsel(kontrollerOgHentFodsel(response.getFoedsel()))
                .setKjonn(kontrollerResponseOgHentKjonn(response.getKjoenn()));
    }

    public static PdlPersonResponse.PdlPersonResponseData.Navn kontrollerResponseOgHentNavn(List<PdlPersonResponse.PdlPersonResponseData.Navn> response) {
        return response
                .stream()
                .filter(navn -> !navn.getMetadata().isHistorisk())
                .min(Comparator.comparing(n -> n.getMetadata().getMaster().ordinal()))
                .orElseThrow(() -> new PdlPersonValideringException("Ingen navn på bruker"));
    }

    private static boolean erDoed(List<PdlPersonResponse.PdlPersonResponseData.Doedsfall> response) {
        return !response.isEmpty();
    }

    private static LocalDate kontrollerOgHentFodsel(List<PdlPersonResponse.PdlPersonResponseData.Foedsel> response) {
        var fodselsListe = response.stream().filter(foedsel -> !foedsel.getMetadata().isHistorisk()).toList();
        if (fodselsListe.size() > 1) {
            throw new PdlPersonValideringException("Støtte for flere registrerte fødseler er ikke implentert");
        }
        return fodselsListe.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.Foedsel::getFoedselsdato)
                .map(LocalDate::parse)
                .orElseThrow(() -> new PdlPersonValideringException("Støtte for ingen registrert fødsel er ikke implentert"));
    }

    private static Kjonn kontrollerResponseOgHentKjonn(List<PdlPersonResponse.PdlPersonResponseData.Kjoenn> response) {
        var kjonnListe = response.stream().filter(kjoenn -> !kjoenn.getMetadata().isHistorisk()).toList();
        if (kjonnListe.size() > 1) {
            throw new PdlPersonValideringException("Støtte for flere kjønn er ikke implentert");
        }
        var kjonn = kjonnListe.stream().findAny()
                .orElseThrow(() -> new PdlPersonValideringException("Støtte for ingen kjønn er ikke implentert"))
                .getKjoenn();

        if ("KVINNE".equals(kjonn)) {
            return Kjonn.K;
        } else if ("MANN".equals(kjonn)) {
            return Kjonn.M;
        }
        log.error("Ikke implementert støtte for kjønn: {} ", kjonn);
        throw new PdlPersonValideringException("Fant kjønn som ikke er støttet");
    }
}
