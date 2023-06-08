package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.domene.Sikkerhetstiltak;
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;
import no.nav.pto.veilarbportefolje.util.DateUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.RelasjonsBosted.UKJENT_BOSTED;

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
    private String talespraaktolk;
    private String tegnspraaktolk;
    private LocalDate tolkBehovSistOppdatert;
    private String kommunenummer;
    private String bydelsnummer;
    private String utenlandskAdresse;
    private boolean harUkjentBosted;
    private LocalDate bostedSistOppdatert;
    private String diskresjonskode;
    private Sikkerhetstiltak sikkerhetstiltak;

    private List<Familiemedlem> barn;


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
                .setBydelsnummer(hentBydel(response.getBostedsadresse()))
                .setKommunenummer(hentBostedKommune(response.getBostedsadresse()))
                .setUtenlandskAdresse(hentUtenlandskAdresse(response.getBostedsadresse()))
                .setHarUkjentBosted(hentHarUkjentBosted(response.getBostedsadresse()))
                .setBostedSistOppdatert(hentBostedSisteOppdatert(response.getBostedsadresse()))
                .setTalespraaktolk(hentTalespraaktolk(response.getTilrettelagtKommunikasjon()))
                .setTegnspraaktolk(hentTegnspraaktolk(response.getTilrettelagtKommunikasjon()))
                .setTolkBehovSistOppdatert(hentTolkBehovSistOppdatert(response.getTilrettelagtKommunikasjon()))
                .setDiskresjonskode(hentDiskresjonkode(response.getAdressebeskyttelse()))
                .setSikkerhetstiltak(hentSikkerhetstiltak(response.getSikkerhetstiltak()))
                .setBarn(hentBarn(response.getForelderBarnRelasjon(), hentBostedAdresse(response.getBostedsadresse())));

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

    private static String hentBydel(List<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> response) {
        return hentBostedAdresse(response)
                .map(PdlPersonResponse.PdlPersonResponseData.Bostedsadresse::getVegadresse)
                .map(PdlPersonResponse.PdlPersonResponseData.Vegadresse::getBydelsnummer)
                .orElse(null);
    }

    private static String hentUtenlandskAdresse(List<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> response) {
        return hentBostedAdresse(response)
                .map(PdlPersonResponse.PdlPersonResponseData.Bostedsadresse::getUtenlandskAdresse)
                .map(PdlPersonResponse.PdlPersonResponseData.UtenlandskAdresse::getLandkode)
                .orElse(null);
    }

    private static String hentBostedKommune(List<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> response) {
        return hentBostedAdresse(response)
                .map(PdlPersonResponse.PdlPersonResponseData.Bostedsadresse::getVegadresse)
                .map(PdlPersonResponse.PdlPersonResponseData.Vegadresse::getKommunenummer)
                .orElse(null);
    }

    private static String hentTalespraaktolk(List<PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon> tilrettelagtKommunikasjon) {
        if (tilrettelagtKommunikasjon == null) {
            return null;
        }
        var tilrettelagtKommunikasjonAktiv = tilrettelagtKommunikasjon.stream().filter(tilrettelagKomunikasjon -> !tilrettelagKomunikasjon.getMetadata().isHistorisk()).toList();
        return tilrettelagtKommunikasjonAktiv.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon::getTalespraaktolk)
                .map(PdlPersonResponse.PdlPersonResponseData.Spraak::getSpraak)
                .orElse("");
    }

    private static String hentTegnspraaktolk(List<PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon> tilrettelagtKommunikasjon) {
        if (tilrettelagtKommunikasjon == null) {
            return null;
        }
        var tilrettelagtKommunikasjonAktiv = tilrettelagtKommunikasjon.stream().filter(tilrettelagKomunikasjon -> !tilrettelagKomunikasjon.getMetadata().isHistorisk()).toList();
        return tilrettelagtKommunikasjonAktiv.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon::getTegnspraaktolk)
                .map(PdlPersonResponse.PdlPersonResponseData.Spraak::getSpraak)
                .orElse("");
    }

    private static LocalDate hentTolkBehovSistOppdatert(List<PdlPersonResponse.PdlPersonResponseData.TilrettelagtKommunikasjon> tilrettelagtKommunikasjon) {
        if (tilrettelagtKommunikasjon == null) {
            return null;
        }
        var tilrettelagtKommunikasjonAktiv = tilrettelagtKommunikasjon.stream().filter(tilrettelagKomunikasjon -> !tilrettelagKomunikasjon.getMetadata().isHistorisk()).toList();

        if (tilrettelagtKommunikasjonAktiv.isEmpty()) {
            return null;
        }
        return tilrettelagtKommunikasjonAktiv.stream().findFirst().get().getMetadata().getEndringer()
                .stream()
                .map(endringer -> DateUtils.toLocalDateOrNull(endringer.getRegistrert()))
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .get();
    }

    private static boolean hentHarUkjentBosted(List<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> response) {
        if (response == null) {
            return false;
        }
        var bostedsadresses = response.stream().filter(bostedsadresse -> !bostedsadresse.getMetadata().isHistorisk()).toList();
        return bostedsadresses.stream().findFirst()
                .map(x -> x.getUkjentBosted() != null)
                .orElse(false);
    }

    private static LocalDate hentBostedSisteOppdatert(List<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> response) {
        return hentBostedAdresse(response)
                .map(bostedsadresse -> bostedsadresse.getMetadata().getEndringer())
                .map(endringers -> endringers.get(0))
                .map(endringer -> DateUtils.toLocalDateOrNull(endringer.getRegistrert()))
                .orElse(null);
    }


    private static String hentDiskresjonkode(List<PdlPersonResponse.PdlPersonResponseData.Adressebeskyttelse> adressebeskyttelse) {
        if (adressebeskyttelse == null) {
            return null;
        }
        var adressebeskyttelseAktiv = adressebeskyttelse.stream().filter(x -> !x.getMetadata().isHistorisk()).toList();
        return adressebeskyttelseAktiv.stream().findFirst()
                .map(PdlPersonResponse.PdlPersonResponseData.Adressebeskyttelse::getGradering)
                .map(Adressebeskyttelse::mapKodeTilTall)
                .orElse(null);
    }

    private static Sikkerhetstiltak hentSikkerhetstiltak(List<PdlPersonResponse.PdlPersonResponseData.Sikkerhetstiltak> sikkerhetstiltak) {
        if (sikkerhetstiltak == null) {
            return null;
        }
        var sikkerhetstiltakAktiv = sikkerhetstiltak.stream().filter(x -> !x.getMetadata().isHistorisk()).toList();
        return sikkerhetstiltakAktiv.stream().findFirst()
                .map(x -> {
                    LocalDate gyldigFra = (x.getGyldigFraOgMed() != null) ? LocalDate.parse(x.getGyldigFraOgMed()) : null;
                    LocalDate gyldigTil = (x.getGyldigTilOgMed() != null) ? LocalDate.parse(x.getGyldigTilOgMed()) : null;
                    return new Sikkerhetstiltak(x.getTiltakstype(), x.getBeskrivelse(), gyldigFra, gyldigTil);
                })
                .orElse(null);
    }

    private static List<Statsborgerskap> hentStatsborgerskap(List<PdlPersonResponse.PdlPersonResponseData.Statsborgerskap> statsborgerskaps) {
        if (statsborgerskaps == null) {
            return Collections.emptyList();
        }
        var statsborgerskapsAktiv = statsborgerskaps.stream().filter(statsborgerskap -> !statsborgerskap.getMetadata().isHistorisk()).toList();
        return statsborgerskapsAktiv.stream().map(s -> {
            LocalDate gyldigFra = (s.getGyldigFraOgMed() != null) ? LocalDate.parse(s.getGyldigFraOgMed()) : null;
            LocalDate gyldigTil = (s.getGyldigTilOgMed() != null) ? LocalDate.parse(s.getGyldigTilOgMed()) : null;
            return new PDLStatsborgerskap(s.getLand(), gyldigFra, gyldigTil);
        }).map(PDLStatsborgerskap::toStatsborgerskap).collect(Collectors.toList());
    }

    private static List<Familiemedlem> hentBarn(List<PdlPersonResponse.PdlPersonResponseData.ForelderBarnRelasjon> forelderBarnRelasjon, Optional<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> foreldreBostedadresse) {
        var forelderBarnRelasjonAktiv = forelderBarnRelasjon.stream().filter(fb -> !fb.getMetadata().isHistorisk()).toList();

        List<Fnr> barnFnrListe = hentBarnaFnr(forelderBarnRelasjonAktiv);
        List<Familiemedlem> barnInfo = hentFamiliemedlemOpplysninger(barnFnrListe, foreldreBostedadresse);
        List<Familiemedlem> barnUtenFnrInfo = hentBarnUtenFnr(forelderBarnRelasjonAktiv);
        barnInfo.addAll(barnUtenFnrInfo);

        return barnInfo;
    }

    public static List<Fnr> hentBarnaFnr(List<PdlPersonResponse.PdlPersonResponseData.ForelderBarnRelasjon> familierelasjoner) {
        return familierelasjoner.stream()
                .filter(familierelasjon -> "BARN".equals(familierelasjon.getRelatertPersonsRolle()))
                .map(PdlPersonResponse.PdlPersonResponseData.ForelderBarnRelasjon::getRelatertPersonsIdent)
                .filter(Objects::nonNull)
                .map(Fnr::of)
                .collect(Collectors.toList());
    }

    private static List<Familiemedlem> hentBarnUtenFnr(List<PdlPersonResponse.PdlPersonResponseData.ForelderBarnRelasjon> forelderBarnRelasjoner) {
        return forelderBarnRelasjoner.stream()
                .filter(familierelasjon -> "BARN".equals(familierelasjon.getRelatertPersonsRolle()))
                .filter(barn -> barn.getRelatertPersonsIdent() == null)
                .map(PdlPersonResponse.PdlPersonResponseData.ForelderBarnRelasjon::getRelatertPersonUtenFolkeregisteridentifikator)
                .filter(Objects::nonNull)
                .map(barn -> {
                    return new Familiemedlem()
                            .setFodselsdato(barn.getFoedselsdato())
                            .setRelasjonsBosted(UKJENT_BOSTED);
                })
                .collect(Collectors.toList());
    }

    public static List<Familiemedlem> hentFamiliemedlemOpplysninger(List<Fnr> familemedlemFnr, Optional<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> foreldreBostedadresse) {
        List<HentPerson.PersonFraBolk> familiemedlemInfo = pdlClient.hentPersonBolk(familemedlemFnr);

        return familiemedlemInfo
                .stream()
                .filter(medlemInfo -> medlemInfo.getCode().equals("ok"))
                .map(HentPerson.PersonFraBolk::getPerson)
                .filter(PersonV2DataMapper::harGyldigIdent)
                .map(familiemedlem -> mapFamiliemedlem(familiemedlem, foreldreBostedadresse))
                .collect(Collectors.toList());
    }

    public Familiemedlem mapFamiliemedlem(Familiemedlem familiemedlem, PdlPersonResponse.PdlPersonResponseData.Bostedsadresse bostedsadresse) {
        Fnr familiemedlemFnr = PersonV2DataMapper.hentFamiliemedlemFnr(familiemedlem);

        return PersonV2DataMapper.familiemedlemMapper(
                familiemedlem,
                erSkjermet(familiemedlemFnr),
                bostedsadresse,
                authService
        );
    }


    private static Optional<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> hentBostedAdresse(List<PdlPersonResponse.PdlPersonResponseData.Bostedsadresse> response) {
        if (response == null) {
            return Optional.empty();
        }
        return response.stream().filter(bostedsadresse -> !bostedsadresse.getMetadata().isHistorisk()).findFirst();
    }
}
