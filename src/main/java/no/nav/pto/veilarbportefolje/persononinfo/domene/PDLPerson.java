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

    public static PDLPerson genererFraApiRespons(PdlPersonResponse response) {
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

    private static boolean erDoed(PdlPersonResponse response) {
        return !response.getData().getHentPerson().getDoedsfall().isEmpty();
    }

    private static LocalDate kontrollerOgHentFodsel(PdlPersonResponse response) {
        var fodselsListe = response.getData().getHentPerson().getFoedsel();
        if (fodselsListe.size() > 1) {
            throw new IllegalStateException("Støtte for flere registrerte fødseler er ikke implentert");
        }
        return fodselsListe.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Støtte for ingen registrert fødsel er ikke implentert"))
                .getFoedselsdato();

    }

    private static Kjonn kontrollerResponseOgHentKjonn(PdlPersonResponse response) {
        var kjonnListe = response.getData().getHentPerson().getKjoenn();
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

    private static Fnr kontrollerResponseOgHentFnr(PdlPersonResponse response) {
        var fnrListe = response.getData().getHentPerson().getFolkeregisteridentifikator();
        if (fnrListe.size() > 1) {
            throw new IllegalStateException("Flere enn en aktiv ident på bruker");
        }
        return Fnr.of(fnrListe.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Ingen ident på bruker"))
                .getIdentifikasjonsnummer());
    }

    private static PdlPersonResponse.PdlPersonResponseData.Navn kontrollerResponseOgHentNavn(PdlPersonResponse response) {
        var navnListe = response.getData().getHentPerson().getFolkeregisteridentifikator();
        if (navnListe.size() > 1) {
            throw new IllegalStateException("Flere enn en aktivt navn på bruker");
        }
        return response.getData().getHentPerson().getNavn().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Ingen navn på bruker"));
    }
}
