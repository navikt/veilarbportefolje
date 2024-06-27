package no.nav.pto.veilarbportefolje.huskelapp;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.huskelapp.domain.Huskelapp;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@RequiredArgsConstructor
@Service
public class HuskelappService {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final BrukerServiceV2 brukerServiceV2;
    private final HuskelappRepository huskelappRepository;


    @Transactional
    public UUID opprettHuskelapp(HuskelappOpprettRequest huskelappOpprettRequest, VeilederId veilederId) {
        try {
            Optional<NavKontor> navKontorOptional = brukerServiceV2.hentNavKontor(huskelappOpprettRequest.brukerFnr());
            NavKontor navKontor = navKontorOptional.orElseThrow(RuntimeException::new);

            huskelappRepository.deaktivereAlleHuskelappRaderPaaBruker(huskelappOpprettRequest.brukerFnr());
            UUID huskelappId = huskelappRepository.opprettHuskelapp(huskelappOpprettRequest, veilederId, EnhetId.of(navKontor.getValue()));

            AktorId aktorId = hentAktorId(huskelappOpprettRequest.brukerFnr()).orElseThrow(RuntimeException::new);
            opensearchIndexerV2.updateHuskelapp(aktorId, new HuskelappForBruker(huskelappOpprettRequest.frist(), huskelappOpprettRequest.kommentar(), LocalDate.now(), veilederId.getValue(), huskelappId.toString(), navKontor.getValue()));

            return huskelappId;
        } catch (Exception e) {
            secureLog.error("Kunne ikke opprette huskelapp for fnr: " + huskelappOpprettRequest.brukerFnr(), e);
            throw new RuntimeException("Kunne ikke opprette huskelapp");
        }
    }

    @Transactional
    public void redigerHuskelapp(HuskelappRedigerRequest huskelappRedigerRequest, VeilederId veilederId) {
        try {
            Optional<NavKontor> navKontorOptional = brukerServiceV2.hentNavKontor(huskelappRedigerRequest.brukerFnr());
            NavKontor navKontor = navKontorOptional.orElseThrow(RuntimeException::new);

            huskelappRepository.deaktivereAlleHuskelappRaderPaaBruker(huskelappRedigerRequest.brukerFnr());
            huskelappRepository.redigerHuskelapp(huskelappRedigerRequest, veilederId, EnhetId.of(navKontor.getValue()));

            AktorId aktorId = hentAktorId(huskelappRedigerRequest.brukerFnr()).orElseThrow(RuntimeException::new);
            opensearchIndexerV2.updateHuskelapp(aktorId, new HuskelappForBruker(huskelappRedigerRequest.frist(), huskelappRedigerRequest.kommentar(), LocalDate.now(), veilederId.getValue(), huskelappRedigerRequest.huskelappId().toString(), navKontor.getValue()));

        } catch (Exception e) {
            secureLog.error("Kunne ikke redigere huskelapp for fnr: " + huskelappRedigerRequest.brukerFnr() + " HuskelappId: " + huskelappRedigerRequest.huskelappId().toString(), e);
            throw new RuntimeException("Kunne ikke redigere huskelapp");
        }
    }

    public Optional<Huskelapp> hentHuskelapp(UUID huskelappId) {
        try {
            return huskelappRepository.hentAktivHuskelapp(huskelappId);
        } catch (Exception e) {
            secureLog.error("Kunne ikke hente huskelapp for id: " + huskelappId + "Stacktrace: " + e.getCause(), e);
            throw new RuntimeException("Kunne ikke hente huskelapp");
        }
    }

    public Optional<Huskelapp> hentHuskelapp(Fnr brukerFnr) {
        try {
            return huskelappRepository.hentAktivHuskelapp(brukerFnr);
        } catch (Exception e) {
            secureLog.error("Kunne ikke hente huskelapp for bruker: " + brukerFnr, e);
            throw new RuntimeException("Kunne ikke hente huskelapp");
        }
    }

    public void settHuskelappIkkeAktiv(UUID huskelappId, Fnr brukerFnr) {
        try {
            huskelappRepository.settSisteHuskelappRadIkkeAktiv(huskelappId);

            AktorId aktorId = hentAktorId(brukerFnr).orElseThrow(RuntimeException::new);
            opensearchIndexerV2.slettHuskelapp(aktorId);
        } catch (Exception e) {
            secureLog.error("Kunne ikke sette huskelapp til inaktiv for bruker: " + brukerFnr, e);
            throw new RuntimeException("Kunne ikke inaktivere huskelapp");
        }
    }

    public void sletteAlleHuskelapperPaaBruker(AktorId aktorId, Optional<Fnr> maybeFnr) {
        try {
            secureLog.info("Sletter alle huskelapper paa bruker med aktoerid: " + aktorId);
            if (maybeFnr.isPresent()) {
                huskelappRepository.slettAlleHuskelappRaderPaaBruker(maybeFnr.get());
                opensearchIndexerV2.slettHuskelapp(aktorId);
            } else {
                secureLog.warn("Kunne ikke slette huskelapper for bruker med AktørID {}. Årsak fødselsnummer-parameter var tom.", aktorId.get());
            }
        } catch (Exception e) {
            secureLog.error("Kunne ikke slette huskelapper for aktoerId: " + aktorId.toString());
            throw new RuntimeException("Kunne ikke slette huskelapper", e);
        }
    }

    public void deaktivereAlleHuskelapperPaaBruker(AktorId aktorId, Optional<Fnr> maybeFnr) {
        try {
            secureLog.info("Deaktiverer alle huskelapper paa bruker med aktoerid: " + aktorId);
            if (maybeFnr.isPresent()) {
                huskelappRepository.deaktivereAlleHuskelappRaderPaaBruker(maybeFnr.get());
                opensearchIndexerV2.slettHuskelapp(aktorId);
            } else {
                secureLog.warn("Kunne ikke deaktivere huskelapper for bruker med AktørID {}. Årsak fødselsnummer-parameter var tom.", aktorId.get());
            }
        } catch (Exception e) {
            secureLog.error("Kunne ikke deaktivere huskelapper for aktoerId: " + aktorId.toString());
            throw new RuntimeException("Kunne ikke deaktivere huskelapper", e);
        }
    }

    @Transactional
    public void oppdaterEnhetPaaHuskelapp(Fnr fnr, EnhetId nyEnhetId, VeilederId veilederId) {
        try {
            Optional<Huskelapp> huskelappForBruker = huskelappRepository.hentAktivHuskelapp(fnr);
            huskelappForBruker.ifPresent(huskelapp -> {
                HuskelappRedigerRequest huskelappRequest = new HuskelappRedigerRequest(huskelapp.huskelappId(), fnr, huskelapp.frist(), huskelapp.kommentar());

                huskelappRepository.deaktivereAlleHuskelappRaderPaaBruker(fnr);
                huskelappRepository.redigerHuskelapp(huskelappRequest, veilederId, nyEnhetId);

                AktorId aktorId = hentAktorId(huskelappRequest.brukerFnr()).orElseThrow(RuntimeException::new);
                opensearchIndexerV2.updateHuskelapp(aktorId, new HuskelappForBruker(huskelappRequest.frist(), huskelappRequest.kommentar(), LocalDate.now(), veilederId.getValue(), huskelappRequest.huskelappId().toString(), nyEnhetId.get()));
            });
        } catch (Exception e) {
            secureLog.error("Kunne ikke oppdatere enhet på huskelapp for fnr: " + fnr, e);
            throw new RuntimeException("Kunne ikke oppdatere enhet på huskelapp");
        }
    }

    public boolean brukerHarHuskelappPaForrigeNavkontor(AktorId aktoerId, Optional<Fnr> maybeFnr) {
        if (maybeFnr.isEmpty()) {
            return false;
        }

        Optional<String> navKontorPaHuskelapp = huskelappRepository.hentNavkontorPaHuskelapp(maybeFnr.get());

        if (navKontorPaHuskelapp.isEmpty()) {
            secureLog.info("Bruker {} har ikke NAV-kontor på huskelapp", aktoerId.toString());
            return false;
        }

        final Optional<String> navKontorForBruker = brukerServiceV2.hentNavKontor(aktoerId).map(NavKontor::getValue);
        if (navKontorForBruker.isEmpty()) {
            secureLog.error("Kunne ikke hente NAV-kontor for bruker {}", aktoerId.toString());
            return false;
        }

        boolean navkontorForBrukerUlikNavkontorPaHuskelapp = !navKontorForBruker.orElseThrow().equals(navKontorPaHuskelapp.orElseThrow());

        if (navkontorForBrukerUlikNavkontorPaHuskelapp) {
            secureLog.info("Bruker {} er på kontor {} mens huskelappen er lagret på et annet kontor {}", aktoerId.toString(), navKontorForBruker.get(), navKontorPaHuskelapp.get());
        } else {
            secureLog.info("Bruker {} er på kontor {} og huskelappen er lagret på samme kontor {}", aktoerId.toString(), navKontorForBruker.get(), navKontorPaHuskelapp.get());
        }

        return navkontorForBrukerUlikNavkontorPaHuskelapp;
    }

    private Optional<AktorId> hentAktorId(Fnr fnr) {
        return brukerServiceV2.hentAktorId(fnr);
    }

}
