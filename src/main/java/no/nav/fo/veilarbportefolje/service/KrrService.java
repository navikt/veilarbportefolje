package no.nav.fo.veilarbportefolje.service;

import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.fo.veilarbportefolje.database.KrrRepository;
import no.nav.fo.veilarbportefolje.domene.KrrDAO;
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
import static no.nav.fo.veilarbportefolje.util.MetricsUtils.timed;

@Slf4j
public class KrrService {

    private KrrRepository krrRepository;
    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;
    private LockService lockService;

    @Inject
    public KrrService(KrrRepository krrRepository, DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1, LockService lockService) {
        this.krrRepository = krrRepository;
        this.digitalKontaktinformasjonV1 = digitalKontaktinformasjonV1;
        this.lockService = lockService;
    }

    public void hentDigitalKontaktInformasjonBolk() {
        lockService.runWithLock(this::hentDigitalKontaktInformasjonBolkWithLock);
    }

    private void hentDigitalKontaktInformasjonBolkWithLock() {
        log.info("Indeksering: Starter henting av KRR informasjon...");
        krrRepository.slettKrrInformasjon();
        krrRepository.iterateFnrsUnderOppfolging(1000, this::hentDigitalKontaktInformasjon);
        log.info("Indeksering: Fullført henting av KRR informasjon");
    }

    void hentDigitalKontaktInformasjon(List<String> fnrListe) {
        timed("indeksering.oppdatering.krr.bolk",
                (error) -> log.error("Feil ved henting fra KRR", error),
                () -> {
                    val req = new WSHentDigitalKontaktinformasjonBolkRequest().withPersonidentListe(fnrListe);
                    val resp = digitalKontaktinformasjonV1.hentDigitalKontaktinformasjonBolk(req);

                    krrRepository.lagreKRRInformasjon(mapDigitalKontaktInformasjon(
                            resp.getDigitalKontaktinformasjonListe(),
                            resp.getForretningsmessigUnntakListe()
                    ));
                });
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

    static Timestamp nyesteAv(Option<Timestamp> epostSisteVerifisert, Option<Timestamp> mobileSisteVerifisert) {
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
