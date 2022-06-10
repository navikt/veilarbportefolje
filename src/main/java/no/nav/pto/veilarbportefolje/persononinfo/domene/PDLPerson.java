package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.persononinfo.Landgruppe;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private String foedeland;
    private List<Statsborgerskap> statsborgerskap;
    private String innflyttingTilNorgeFraLand;
    private LocalDate angittFlyttedato;
    private String folkeregisterpersonstatus;
    private String talespraaktolk;
    private String tegnspraaktolk;
    private String landgruppe;


    public static PDLPerson genererFraApiRespons(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData response) {
        PDLPerson person = new PDLPerson();
        PdlPersonResponse.PdlPersonResponseData.Navn navn = kontrollerResponseOgHentNavn(response.getNavn());

        return person.setFornavn(navn.getFornavn())
                .setEtternavn(navn.getEtternavn())
                .setMellomnavn(navn.getMellomnavn())
                .setKjonn(kontrollerResponseOgHentKjonn(response.getKjoenn()))
                .setErDoed(erDoed(response.getDoedsfall()))
                .setFoedsel(kontrollerOgHentFodsel(response.getFoedsel()))
                .setFoedeland(hentFoedselLand(response.getFoedsel()))
                .setStatsborgerskap(hentStatsborgerskap(response.getStatsborgerskap()))
                .setAngittFlyttedato(hentAngittFlyttedato(response.getBostedsadresse()))
                .setFolkeregisterpersonstatus(hentFolkeregisterpersonstatus(response.getFolkeregisterpersonstatus()))
                .setTalespraaktolk(hentTalespraaktolk(response.getTilrettelagtKommunikasjon()))
                .setTegnspraaktolk(hentTegnspraaktolk(response.getTilrettelagtKommunikasjon()))
                .setLandgruppe(hentLandKode(response.getFoedsel()));
    }

    public static PdlPersonResponse.PdlPersonResponseData.Navn kontrollerResponseOgHentNavn(List<PdlPersonResponse.PdlPersonResponseData.Navn> response) {
        return response
                .stream()
                .filter(navn -> !navn.getMetadata().isHistorisk())
                .min(Comparator.comparing(n -> n.getMetadata().getMaster().prioritet))
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

    private static String hentFoedselLand(List<PdlPersonResponse.PdlPersonResponseData.Foedsel> response) {
        var fodselsListe = response.stream().filter(foedsel -> !foedsel.getMetadata().isHistorisk()).toList();
        if (fodselsListe.size() > 1) {
            throw new PdlPersonValideringException("Støtte for flere registrerte foedselLand er ikke implentert");
        }
        return fodselsListe.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.Foedsel::getFoedeland)
                .orElse("");
    }

    private static LocalDate hentAngittFlyttedato(List<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> response) {
        var bostedsadresses = response.stream().filter(bostedsadresse -> !bostedsadresse.getMetadata().isHistorisk()).toList();
        return bostedsadresses.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.Bostedsadresse::getAngittFlyttedato)
                .map(LocalDate::parse)
                .orElse(null);
    }

    private static String hentLandKode(List<PdlPersonResponse.PdlPersonResponseData.Foedsel> response) {
        var fodselsListe = response.stream().filter(foedsel -> !foedsel.getMetadata().isHistorisk()).toList();
        if (fodselsListe.size() > 1) {
            throw new PdlPersonValideringException("Støtte for flere registrerte foedselLand er ikke implentert");
        }
        return fodselsListe.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.Foedsel::getFoedeland)
                .map(Landgruppe::getLandgruppe)
                .map(Optional::get)
                .map(String::valueOf)
                .orElse("");

    }

    private static String hentFolkeregisterpersonstatus(List<PdlPersonResponse.PdlPersonResponseData.Folkeregisterpersonstatus> folkeregisterpersonstatus) {
        var folkeregisterStatus = folkeregisterpersonstatus.stream().filter(foedsel -> !foedsel.getMetadata().isHistorisk()).toList();
        return folkeregisterStatus.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.Folkeregisterpersonstatus::getFolkeregisterpersonstatus)
                .orElse("");
    }

    private static String hentTalespraaktolk(List<PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon> tilrettelagtKommunikasjon) {
        var tilrettelagtKommunikasjonAktiv = tilrettelagtKommunikasjon.stream().filter(tilrettelagKomunikasjon -> !tilrettelagKomunikasjon.getMetadata().isHistorisk()).toList();
        return tilrettelagtKommunikasjonAktiv.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon::getTalespraaktolk)
                .map(PdlPersonResponse.PdlPersonResponseData.Spraak::getSpraak)
                .orElse("");
    }

    private static String hentTegnspraaktolk(List<PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon> tilrettelagtKommunikasjon) {
        var tilrettelagtKommunikasjonAktiv = tilrettelagtKommunikasjon.stream().filter(tilrettelagKomunikasjon -> !tilrettelagKomunikasjon.getMetadata().isHistorisk()).toList();
        return tilrettelagtKommunikasjonAktiv.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon::getTegnspraaktolk)
                .map(PdlPersonResponse.PdlPersonResponseData.Spraak::getSpraak)
                .orElse("");
    }

    private static List<Statsborgerskap> hentStatsborgerskap(List<PdlPersonResponse.PdlPersonResponseData.Statsborgerskap> statsborgerskaps) {

        var statsborgerskapsAktiv = statsborgerskaps.stream().filter(statsborgerskap -> !statsborgerskap.getMetadata().isHistorisk()).toList();
        return statsborgerskapsAktiv.stream().map(s -> {
            LocalDate gyldigFra = (s.getGyldigFraOgMed() != null) ? LocalDate.parse(s.getGyldigFraOgMed()) : null;
            LocalDate gyldigTil = (s.getGyldigTilOgMed() != null) ? LocalDate.parse(s.getGyldigTilOgMed()) : null;
            return new Statsborgerskap(s.getLand(), gyldigFra, gyldigTil);
        }).collect(Collectors.toList());
    }
}
