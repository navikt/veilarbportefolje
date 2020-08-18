package no.nav.pto.veilarbportefolje.krr;

import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.KrrDTO;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSEpostadresse;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSForretningsmessigUnntakForBolk;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSMobiltelefonnummer;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonBolkRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonBolkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

@Slf4j
@Service
public class KrrService {

    private final KrrRepository krrRepository;
    private final DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

    @Autowired
    public KrrService(KrrRepository krrRepository, DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1) {
        this.krrRepository = krrRepository;
        this.digitalKontaktinformasjonV1 = digitalKontaktinformasjonV1;
    }

    @SneakyThrows
    public void hentDigitalKontaktInformasjonBolk() {
        log.info("Indeksering: Starter henting av KRR informasjon...");
        krrRepository.slettKrrInformasjon();
        krrRepository.iterateFnrsUnderOppfolging(50, this::hentDigitalKontaktInformasjon);
        log.info("Indeksering: Fullført henting av KRR informasjon");
    }

    @SneakyThrows
    void hentDigitalKontaktInformasjon(List<String> fnrListe) {

        WSHentDigitalKontaktinformasjonBolkRequest req = new WSHentDigitalKontaktinformasjonBolkRequest().withPersonidentListe(fnrListe);
        WSHentDigitalKontaktinformasjonBolkResponse res = digitalKontaktinformasjonV1.hentDigitalKontaktinformasjonBolk(req);

        List<KrrDTO> Kfoo = mapDigitalKontaktInformasjon(
                res.getDigitalKontaktinformasjonListe(),
                res.getForretningsmessigUnntakListe()
        );

        krrRepository.lagreKRRInformasjon(Kfoo);
    }

    private List<KrrDTO> mapDigitalKontaktInformasjon(
            List<WSKontaktinformasjon> digitalKontaktinformasjonListe,
            List<WSForretningsmessigUnntakForBolk> kontaktInformasjonIkkeFunnetListe) {

        List<KrrDTO> digitalKontaktInformasjonListe = digitalKontaktinformasjonListe
                .stream()
                .map((krrInformasjon) ->
                        new KrrDTO()
                                .setFnr(krrInformasjon.getPersonident())
                                .setReservertIKrr(safeToJaNei(Boolean.valueOf(krrInformasjon.getReservasjon())))
                                .setSistVerifisert(hentNyesteSisteVerifisert(krrInformasjon))
                                .setLagtTilIDB(Timestamp.from(Instant.now())))
                .collect(Collectors.toList());

        List<KrrDTO> digitalKontaktInformasjonIkkeFunnet = kontaktInformasjonIkkeFunnetListe
                .stream()
                .map((krrInformasjon) ->
                        new KrrDTO()
                                .setFnr(krrInformasjon.getPersonident())
                                .setReservertIKrr(safeToJaNei(Boolean.valueOf("true")))
                                .setSistVerifisert(null)
                                .setLagtTilIDB(Timestamp.from(Instant.now())))
                .collect(Collectors.toList());

        digitalKontaktInformasjonListe.addAll(digitalKontaktInformasjonIkkeFunnet);

        return digitalKontaktInformasjonListe;
    }

    private static String safeToJaNei(Boolean aBoolean) {
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

    public static Timestamp nyesteAv(Option<Timestamp> epostSisteVerifisert, Option<Timestamp> mobileSisteVerifisert) {
        return Stream.of(epostSisteVerifisert, mobileSisteVerifisert)
                .filter(Option::isDefined)
                .map(Option::get)
                .max()
                .getOrNull();
    }

    private static Option<Timestamp> toTimestamp(XMLGregorianCalendar calendar) {
        return Option.of(calendar)
                .map(XMLGregorianCalendar::toGregorianCalendar)
                .map(Calendar::getTimeInMillis)
                .map(Timestamp::new);
    }
}
