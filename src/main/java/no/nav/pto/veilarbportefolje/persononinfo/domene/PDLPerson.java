package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;

import java.time.LocalDate;

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
        PdlPersonResponse.PdlPersonResponseData.Navn navn = kontrollerResponseOgHentNavn(response);
        person.setFornavn(navn.getFornavn());
        person.setEtternavn(navn.getEtternavn());
        person.setMellomnavn(navn.getMellomnavn());
        person.setErDoed(erDoed(response));
        person.setFoedsel(kontrollerOgHentFodsel(response));
        person.setKjonn(kontrollerResponseOgHentKjonn(response));

        return person;
    }

    private static boolean erDoed(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response) {
        return !response.getDoedsfall().isEmpty();
    }

    private static LocalDate kontrollerOgHentFodsel(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response) {
        var fodselsListe = response.getFoedsel()
                .stream().filter(foedsel -> !foedsel.getMetadata().isHistorisk()).toList();
        if (fodselsListe.size() > 1) {
            throw new PdlPersonValideringException("Støtte for flere registrerte fødseler er ikke implentert");
        }
        return fodselsListe.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.Foedsel::getFoedselsdato)
                .map(LocalDate::parse)
                .orElseThrow(() -> new PdlPersonValideringException("Støtte for ingen registrert fødsel er ikke implentert"));
    }

    private static Kjonn kontrollerResponseOgHentKjonn(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response) {
        var kjonnListe = response.getKjoenn()
                .stream().filter(kjoenn -> !kjoenn.getMetadata().isHistorisk()).toList();
        if (kjonnListe.size() > 1) {
            throw new PdlPersonValideringException("Støtte for flere kjønn er ikke implentert");
        }
        var kjonn = kjonnListe.stream().findFirst()
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

    private static PdlPersonResponse.PdlPersonResponseData.Navn kontrollerResponseOgHentNavn(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response) {
        var navnListe = response.getNavn()
                .stream().filter(navn -> !navn.getMetadata().isHistorisk()).toList();
        if (navnListe.size() > 1) {
            throw new PdlPersonValideringException("Flere enn en aktivt navn");
        }
        return navnListe.stream().findFirst()
                .orElseThrow(() -> new PdlPersonValideringException("Ingen navn på bruker"));
    }
}
