package no.nav.fo.service;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.database.KrrRepository;
import no.nav.fo.domene.KrrDTO;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSForretningsmessigUnntakForBolk;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonBolkRequest;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
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
        List<WSKontaktinformasjon> digitalKontaktinformasjonListe = digitalKontaktinformasjonV1.hentDigitalKontaktinformasjonBolk(req).getDigitalKontaktinformasjonListe();
        List<WSForretningsmessigUnntakForBolk> kontaktInformasjonIkkeFunnetListe = digitalKontaktinformasjonV1.hentDigitalKontaktinformasjonBolk(req).getForretningsmessigUnntakListe();

        krrRepository.lagreKRRInformasjon(mapDigitalKontaktInformasjon(digitalKontaktinformasjonListe, kontaktInformasjonIkkeFunnetListe));
    }

    private List<KrrDTO> mapDigitalKontaktInformasjon(
                    List<WSKontaktinformasjon> digitalKontaktinformasjonListe,
                    List<WSForretningsmessigUnntakForBolk> kontaktInformasjonIkkeFunnetListe)
    {
        List<KrrDTO> digitalKontaktInformasjonListe = digitalKontaktinformasjonListe.stream().map((krrInformasjon) -> {
                    KrrDTO krrDTO = new KrrDTO();
                    krrDTO.setFnr(krrInformasjon.getPersonident());
                    krrDTO.setReservertIKrr(safeToJaNei(Boolean.valueOf(krrInformasjon.getReservasjon())));
                    krrDTO.setSistVerifisert(hentNyesteSisteVerifisert(krrInformasjon));
                    krrDTO.setLagtTilIDB(Timestamp.from(Instant.now()));
                    return krrDTO;
       }).collect(Collectors.toList());

        digitalKontaktInformasjonListe.addAll(
                    kontaktInformasjonIkkeFunnetListe.stream().map((krrInformasjon) -> {
                    KrrDTO krrDTO = new KrrDTO();
                    krrDTO.setFnr(krrInformasjon.getPersonident());
                    krrDTO.setReservertIKrr(safeToJaNei(Boolean.valueOf("true")));
                    krrDTO.setSistVerifisert(null);
                    krrDTO.setLagtTilIDB(Timestamp.from(Instant.now()));
                    return krrDTO;
        }).collect(Collectors.toList()));

        return digitalKontaktInformasjonListe;
    }

    static String safeToJaNei(Boolean aBoolean) {
        return TRUE.equals(aBoolean) ? "J" : "N";
    }

    private Timestamp hentNyesteSisteVerifisert(WSKontaktinformasjon digitalKontaktInformasjon) {
        Timestamp epostSisteVerifisert = null;
        Timestamp mobileSisteVerifisert = null;
        if(digitalKontaktInformasjon.getMobiltelefonnummer() != null){
            mobileSisteVerifisert = new Timestamp(digitalKontaktInformasjon.getMobiltelefonnummer().getSistVerifisert().toGregorianCalendar().getTimeInMillis());
        }
        if(digitalKontaktInformasjon.getEpostadresse() != null){
            epostSisteVerifisert = new Timestamp(digitalKontaktInformasjon.getEpostadresse().getSistVerifisert().toGregorianCalendar().getTimeInMillis());
        }

        if(epostSisteVerifisert != null && mobileSisteVerifisert != null){
            return  mobileSisteVerifisert.compareTo(epostSisteVerifisert) == '1' ? mobileSisteVerifisert : epostSisteVerifisert;
        } else if(mobileSisteVerifisert != null){
            return  mobileSisteVerifisert;
        }
        return  epostSisteVerifisert;
     }
}
