package no.nav.fo.service;

import io.vavr.API;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.database.KrrRepository;
import no.nav.fo.domene.KrrDAO;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSEpostadresse;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSForretningsmessigUnntakForBolk;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSMobiltelefonnummer;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonBolkRequest;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

public class KrrService {

    @Inject
    private KrrRepository krrRepository;

    @Inject
    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

    public void hentDigitalKontaktInformasjonBolk() {
        krrRepository.slettKrrInformasjon();
        krrRepository.iterateFnrsUnderOppfolging(1000, this::hentDigitalKontaktInformasjon);
    }

    @SneakyThrows
    private void hentDigitalKontaktInformasjon(List<String> fnrListe) {
        val req = new WSHentDigitalKontaktinformasjonBolkRequest().withPersonidentListe(fnrListe);
        val resp = digitalKontaktinformasjonV1.hentDigitalKontaktinformasjonBolk(req);

        krrRepository.lagreKRRInformasjon(mapDigitalKontaktInformasjon(
                resp.getDigitalKontaktinformasjonListe(),
                resp.getForretningsmessigUnntakListe()
        ));
    }

    private List<KrrDAO> mapDigitalKontaktInformasjon(
            List<WSKontaktinformasjon> digitalKontaktinformasjonListe,
            List<WSForretningsmessigUnntakForBolk> kontaktInformasjonIkkeFunnetListe) {

        List<KrrDAO> digitalKontaktInformasjonListe = digitalKontaktinformasjonListe
                .stream()
                .map((krrInformasjon) ->
                        new KrrDAO()
                                .setFnr(krrInformasjon.getPersonident())
                                .setReservertIKrr(safeToJaNei(Boolean.valueOf(krrInformasjon.getReservasjon())))
                                .setSistVerifisert(hentNyesteSisteVerifisert(krrInformasjon))
                                .setLagtTilIDB(Timestamp.from(Instant.now())))
                .collect(Collectors.toList());

        List<KrrDAO> digitalKontaktInformasjonIkkeFunnet = kontaktInformasjonIkkeFunnetListe
                .stream()
                .map((krrInformasjon) ->
                        new KrrDAO()
                                .setFnr(krrInformasjon.getPersonident())
                                .setReservertIKrr(safeToJaNei(Boolean.valueOf("true")))
                                .setSistVerifisert(null)
                                .setLagtTilIDB(Timestamp.from(Instant.now())))
                .collect(Collectors.toList());

        digitalKontaktInformasjonListe.addAll(digitalKontaktInformasjonIkkeFunnet);

        return digitalKontaktInformasjonListe;
    }

    static String safeToJaNei(Boolean aBoolean) {
        return TRUE.equals(aBoolean) ? "J" : "N";
    }

    private Timestamp hentNyesteSisteVerifisert(WSKontaktinformasjon digitalKontaktInformasjon) {
        Option<Timestamp> epostSisteVerifisert = Option.of(digitalKontaktInformasjon.getEpostadresse())
                .map(WSEpostadresse::getSistVerifisert)
                .flatMap(KrrService::toTimestamp);

        Option<Timestamp> mobileSisteVerifisert = Option.of(digitalKontaktInformasjon.getMobiltelefonnummer())
                .map(WSMobiltelefonnummer::getSistVerifisert)
                .flatMap(KrrService::toTimestamp);

        return nyesteAv(epostSisteVerifisert, mobileSisteVerifisert);
    }

    static Timestamp nyesteAv(Option<Timestamp> epostSisteVerifisert, Option<Timestamp> mobileSisteVerifisert) {
        Timestamp epostSistVerifisert = epostSisteVerifisert.get();
        Timestamp mobileSistVerifisert = mobileSisteVerifisert.get();

        if (epostSistVerifisert == null) {
            return mobileSistVerifisert;
        } else if (mobileSistVerifisert == null) {
            return epostSistVerifisert;
        } else {
            return epostSistVerifisert.compareTo(mobileSistVerifisert) >= 0 ? epostSistVerifisert : mobileSistVerifisert;
        }
    }

    static Option<Timestamp> toTimestamp(XMLGregorianCalendar calendar) {
        return Option.of(calendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(Calendar::getTimeInMillis)
                .map(Timestamp::new);
    }
}
