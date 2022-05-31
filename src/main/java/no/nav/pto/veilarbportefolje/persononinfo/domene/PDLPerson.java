package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
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

    public static PDLPerson genererFraApiRespons(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response, AktorId aktorId) {
        PDLPerson person = new PDLPerson();
        PdlPersonResponse.PdlPersonResponseData.Navn navn = kontrollerResponseOgHentNavn(response, aktorId);
        person.setFornavn(navn.getFornavn());
        person.setEtternavn(navn.getEtternavn());
        person.setMellomnavn(navn.getMellomnavn());
        person.setErDoed(erDoed(response));
        person.setFoedsel(kontrollerOgHentFodsel(response, aktorId));
        person.setKjonn(kontrollerResponseOgHentKjonn(response, aktorId));
        person.setFnr(kontrollerResponseOgHentFnr(response, aktorId));

        return person;
    }

    private static boolean erDoed(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response) {
        return !response.getDoedsfall().isEmpty();
    }

    private static LocalDate kontrollerOgHentFodsel(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response, AktorId aktorId) {
        var fodselsListe = response.getFoedsel()
                .stream().filter(foedsel -> !foedsel.getMetadata().isHistorisk()).toList();
        if (fodselsListe.size() > 1) {
            throw new IllegalStateException("Støtte for flere registrerte fødseler er ikke implentert, aktorId: " + aktorId);
        }
        return fodselsListe.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.Foedsel::getFoedselsdato)
                .map(LocalDate::parse)
                .orElseThrow(() -> new IllegalStateException("Støtte for ingen registrert fødsel er ikke implentert, aktorId: " + aktorId));
    }

    private static Kjonn kontrollerResponseOgHentKjonn(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response, AktorId aktorId) {
        var kjonnListe = response.getKjoenn()
                .stream().filter(kjoenn -> !kjoenn.getMetadata().isHistorisk()).toList();
        if (kjonnListe.size() > 1) {
            throw new IllegalStateException("Støtte for flere kjønn er ikke implentert, aktorId: " + aktorId);
        }
        var kjonn = kjonnListe.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Støtte for ingen kjønn er ikke implentert, aktorId: " + aktorId))
                .getKjoenn();

        if ("KVINNE".equals(kjonn)) {
            return Kjonn.K;
        } else if ("MANN".equals(kjonn)) {
            return Kjonn.M;
        }
        log.error("Ikke implementert støtte for kjønn: {} ", kjonn);
        throw new IllegalStateException("Fant kjønn som ikke er støttet, på aktorId: " + aktorId);
    }

    private static Fnr kontrollerResponseOgHentFnr(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response, AktorId aktorId) {
        var fnrListe = response.getFolkeregisteridentifikator()
                .stream().filter(fnr -> !fnr.getMetadata().isHistorisk()).toList();
        if (fnrListe.size() > 1) {
            throw new IllegalStateException("Flere enn en aktiv ident på aktorId: "+ aktorId);
        }
        return Fnr.of(fnrListe.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Ingen ident på aktorId: "+ aktorId))
                .getIdentifikasjonsnummer());
    }

    private static PdlPersonResponse.PdlPersonResponseData.Navn kontrollerResponseOgHentNavn(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response, AktorId aktorId) {
        var navnListe = response.getNavn()
                .stream().filter(navn -> !navn.getMetadata().isHistorisk()).toList();
        if (navnListe.size() > 1) {
            throw new IllegalStateException("Flere enn en aktivt navn på aktorId: " + aktorId);
        }
        return navnListe.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Ingen navn på aktorId: " + aktorId));
    }
}