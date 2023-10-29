package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlDokument;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PdlPersonValideringException;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivAktor;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivFnr;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlBrukerdataKafkaService extends KafkaCommonConsumerService<PdlDokument> {
    private final PdlService pdlService;

    private final PdlIdentRepository pdlIdentRepository;
    private final BrukerServiceV2 brukerService;
    private final BarnUnder18AarService barnUnder18AarService;
    private final OpensearchIndexer opensearchIndexer;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final UnleashService unleashService;

    @Override
    @SneakyThrows
    public void behandleKafkaMeldingLogikk(PdlDokument pdlDokument) {
        if (pdlDokument == null) {
            secureLog.info("""
                        Fikk tom endrings melding fra PDL.
                        Dette er en tombstone som ignoreres fordi alle historiske identer lenket til nye identer slettes ved en oppdatering.
                    """);
            return;
        }

        List<PDLIdent> pdlIdenter = pdlDokument.getHentIdenter().getIdenter();
        List<AktorId> aktorIder = hentAktorider(pdlIdenter);
        List<Fnr> fnrs = hentFnrs(pdlIdenter);
        List<Fnr> inaktiveFnr = hentInaktiveFnr(pdlIdenter);

        if (pdlIdentRepository.harAktorIdUnderOppfolging(aktorIder)) {
            AktorId aktivAktorId = hentAktivAktor(pdlIdenter);
            secureLog.info("Det oppsto en PDL endring aktoer: {}", aktivAktorId);

            handterIdentEndring(pdlIdenter);

            handterBrukerDataEndring(pdlDokument.getHentPerson(), pdlIdenter);

            if (!FeatureToggle.stoppOpensearchIndeksering(unleashService)) {
                oppdaterOpensearch(aktivAktorId, pdlIdenter);
            }
        }

        if (barnUnder18AarService.erFnrBarnAvForelderUnderOppfolging(fnrs)) {
            log.info("Det oppsto en PDL endring for barn");
            Fnr aktivtFnr = hentAktivFnr(pdlIdenter);
            barnUnder18AarService.handterBarnIdentEndring(aktivtFnr, inaktiveFnr);

            handterBarnEndring(pdlDokument.getHentPerson(), pdlIdenter);

            if (!FeatureToggle.stoppOpensearchIndeksering(unleashService)) {
                List<Fnr> foreldreTilBarn = barnUnder18AarService.finnForeldreTilBarn(aktivtFnr);

                foreldreTilBarn.forEach(fnrForelder -> {
                            Optional<AktorId> aktorIdForelder = brukerService.hentAktorId(fnrForelder);
                            if (aktorIdForelder.isPresent()) {
                                opensearchIndexer.indekser(aktorIdForelder.get());
                            } else {
                                secureLog.warn("Kunne ikke indeksere forelder med fnr {} til barn med fnr {}", fnrForelder, aktivtFnr);
                            }
                        }
                );
            }
        }
    }

    private void handterBrukerDataEndring(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData personFraKafka,
                                          List<PDLIdent> pdlIdenter) {
        Fnr aktivFnr = hentAktivFnr(pdlIdenter);
        AktorId aktivAktorId = hentAktivAktor(pdlIdenter);
        try {
            PDLPerson person = PDLPerson.genererFraApiRespons(personFraKafka);
            pdlService.lagreBrukerData(aktivFnr, person);
        } catch (PdlPersonValideringException e) {
            if (isDevelopment().orElse(false)) {
                secureLog.info(String.format("Ignorerer dårlig datakvalitet i dev, bruker: %s", aktivAktorId), e);
                return;
            }
            secureLog.warn(String.format("Fikk pdl validerings error på aktor: %s, prøver å laste inn data på REST", aktivAktorId), e);
            pdlService.hentOgLagreBrukerData(aktivFnr);
        }
        List<Fnr> inaktiveFnr = hentInaktiveFnr(pdlIdenter);
        pdlService.slettPDLBrukerData(inaktiveFnr);
    }

    private void handterBarnEndring(PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData personFraKafka, List<PDLIdent> pdlIdenter) {
        Fnr aktivtFnrBarn = hentAktivFnr(pdlIdenter);
        try {
            PDLPerson person = PDLPerson.genererFraApiRespons(personFraKafka);
            PDLPersonBarn barn = new PDLPersonBarn();
            barn.setErIlive(!person.isErDoed());
            barn.setFodselsdato(person.getFoedsel());
            barn.setDiskresjonskode(person.getDiskresjonskode());
            pdlService.lagreBrukerDataPaBarn(aktivtFnrBarn, barn);
        } catch (PdlPersonValideringException e) {
            if (isDevelopment().orElse(false)) {
                secureLog.info(String.format("Ignorerer dårlig datakvalitet i dev, bruker: %s", aktivtFnrBarn), e);
                return;
            }
            secureLog.warn(String.format("Fikk pdl validerings error på fnr: %s, prøver å laste inn data på REST", aktivtFnrBarn), e);
            pdlService.hentOgLagreBrukerDataPaBarn(aktivtFnrBarn);
        }
    }


    private void handterIdentEndring(List<PDLIdent> pdlIdenter) {
        pdlIdentRepository.upsertIdenter(pdlIdenter);
    }

    private void oppdaterOpensearch(AktorId aktivAktorId, List<PDLIdent> pdlIdenter) {
        List<AktorId> inaktiveAktorider = hentInaktiveAktorider(pdlIdenter);

        opensearchIndexerV2.slettDokumenter(inaktiveAktorider);
        opensearchIndexer.indekser(aktivAktorId);
    }

    public static List<AktorId> hentAktorider(List<PDLIdent> identer) {
        return identer.stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.AKTORID.equals(pdlIdent.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(AktorId::new)
                .toList();
    }

    private static List<Fnr> hentFnrs(List<PDLIdent> identer) {
        return identer.stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.FOLKEREGISTERIDENT.equals(pdlIdent.getGruppe()))
                .map(PDLIdent::getIdent)
                .map(Fnr::new)
                .toList();
    }

    private static List<AktorId> hentInaktiveAktorider(List<PDLIdent> identer) {
        return identer.stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.AKTORID.equals(pdlIdent.getGruppe()))
                .filter(PDLIdent::isHistorisk)
                .map(PDLIdent::getIdent)
                .map(AktorId::new)
                .toList();
    }

    private static List<Fnr> hentInaktiveFnr(List<PDLIdent> identer) {
        return identer.stream()
                .filter(pdlIdent -> PDLIdent.Gruppe.FOLKEREGISTERIDENT.equals(pdlIdent.getGruppe()))
                .filter(PDLIdent::isHistorisk)
                .map(PDLIdent::getIdent)
                .map(Fnr::new)
                .toList();
    }
}
