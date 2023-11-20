package no.nav.pto.veilarbportefolje.huskelapp;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.Huskelapp;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappInputDto;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOutputDto;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@RequiredArgsConstructor
@Service
public class HuskelappService {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final AktorClient aktorClient;


    public HuskelappRepository huskelappRepository;


    public UUID opprettHuskelapp(HuskelappInputDto inputDto, VeilederId veilederId) {
        try {
            UUID huskelappId = huskelappRepository.opprettHuskelapp(inputDto, veilederId);

            arkivereHuskelapp(huskelappId);

            AktorId aktorId = hentAktorId(inputDto.brukerFnr()).getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
            opensearchIndexerV2.updateHuskelapp(aktorId, new Huskelapp(inputDto.kommentar(), inputDto.frist()));

            return huskelappId;
        } catch (Exception e) {
            secureLog.error("Kunne ikke opprette huskelapp for fnr: " + inputDto.brukerFnr());
            throw new RuntimeException("Kunne ikke opprette huskelapp", e);
        }
    }

    public List<HuskelappOutputDto> hentHuskelapp(VeilederId veilederId, EnhetId enhetId) {
        try {
            return huskelappRepository.hentHuskelapp(enhetId, veilederId);
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke hente huskelapper", e);
        }
    }

    public Optional<HuskelappOutputDto> hentHuskelapp(UUID huskelappId) {
        try {
            return huskelappRepository.hentHuskelapp(huskelappId);
        } catch (Exception e) {
            secureLog.error("Kunne ikke hente huskelapp for id: " + huskelappId);
            throw new RuntimeException("Kunne ikke hente huskelapp", e);
        }
    }

    public HuskelappOutputDto hentHuskelapp(Fnr brukerFnr) {
        try {
            return huskelappRepository.hentHuskelapp(brukerFnr).orElse(null);
        } catch (Exception e) {
            secureLog.error("Kunne ikke hente huskelapp for bruker: " + brukerFnr);
            throw new RuntimeException("Kunne ikke hente huskelapp", e);
        }
    }

    public void slettHuskelapp(UUID huskelappId) {
        try {
            huskelappRepository.slettHuskelapp(huskelappId);
        } catch (Exception e) {
            secureLog.error("Kunne ikke slette huskelapp med id: " + huskelappId);
            throw new RuntimeException("Kunne ikke slette huskelapp", e);
        }
    }

    public void oppdatereStatus(UUID huskelappId, Fnr brukerFnr, HuskelappStatus status) {
        try {
            huskelappRepository.oppdatereStatus(huskelappId, status);

            AktorId aktorId = hentAktorId(brukerFnr).getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
            opensearchIndexerV2.sletteHuskelapp(aktorId);
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke oppdatere huskelapp", e);
        }
    }

    private void arkivereHuskelapp(UUID huskelappId) {
        try {
            oppdatereArkivertDato(huskelappId);
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke arkivere huskelapp", e);
        }
    }

    private void oppdatereArkivertDato(UUID huskelappId) {
        try {
            huskelappRepository.oppdatereArkivertDato(huskelappId);
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    private Try<AktorId> hentAktorId(Fnr fnr) {
        return Try.of(() -> aktorClient.hentAktorId(fnr));
    }
}
