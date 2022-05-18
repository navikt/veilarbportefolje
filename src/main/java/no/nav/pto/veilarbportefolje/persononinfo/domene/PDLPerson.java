package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;

import java.time.LocalDate;

@Data
@Slf4j
@Accessors(chain = true)
public class PDLPerson {
    private Fnr fnr;
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
        person.setFnr(kontrollerResponseOgHentFnr(response));

        return person;
    }

    private static boolean erDoed(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData  response) {
        return !response.getDoedsfall().isEmpty();
    }

    private static LocalDate kontrollerOgHentFodsel(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData  response) {
        var fodselsListe = response.getFoedsel();
        if (fodselsListe.size() > 1) {
            throw new IllegalStateException("Støtte for flere registrerte fødseler er ikke implentert");
        }
        return fodselsListe.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Støtte for ingen registrert fødsel er ikke implentert"))
                .getFoedselsdato();

    }

    private static Kjonn kontrollerResponseOgHentKjonn(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData  response) {
        var kjonnListe = response.getKjoenn();
        if (kjonnListe.size() > 1) {
            throw new IllegalStateException("Støtte for flere kjønn er ikke implentert");
        }
        var kjonn = kjonnListe.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Støtte for ingen kjønn er ikke implentert"))
                .getKjoenn();

        if ("KVINNE".equals(kjonn)) {
            return Kjonn.K;
        } else if ("MANN".equals(kjonn)) {
            return Kjonn.M;
        }
        log.error("Ikke implementert støtte for kjønn: {} ", kjonn);
        throw new IllegalStateException("Fant kjønn som ikke er støttet");
    }

    private static Fnr kontrollerResponseOgHentFnr(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData  response) {
        var fnrListe = response.getFolkeregisteridentifikator();
        if (fnrListe.size() > 1) {
            throw new IllegalStateException("Flere enn en aktiv ident på bruker");
        }
        return Fnr.of(fnrListe.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Ingen ident på bruker"))
                .getIdentifikasjonsnummer());
    }

    private static PdlPersonResponse.PdlPersonResponseData.Navn kontrollerResponseOgHentNavn(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response) {
        var navnListe = response.getFolkeregisteridentifikator();
        if (navnListe.size() > 1) {
            throw new IllegalStateException("Flere enn en aktivt navn på bruker");
        }
        return response.getNavn().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Ingen navn på bruker"));
    }
}
