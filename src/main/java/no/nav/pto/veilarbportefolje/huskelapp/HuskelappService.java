package no.nav.pto.veilarbportefolje.huskelapp;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.Huskelapp;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOutputDto;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.huskelapp.domain.HuskelappStatus;
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


    public UUID opprettHuskelapp(HuskelappOpprettRequest huskelappOpprettRequest, VeilederId veilederId) {
        try {
            UUID huskelappId = huskelappRepository.opprettHuskelapp(huskelappOpprettRequest, veilederId);

            AktorId aktorId = hentAktorId(huskelappOpprettRequest.brukerFnr()).getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
            opensearchIndexerV2.updateHuskelapp(aktorId, new Huskelapp(huskelappOpprettRequest.kommentar(), huskelappOpprettRequest.frist()));

            return huskelappId;
        } catch (Exception e) {
            secureLog.error("Kunne ikke opprette huskelapp for fnr: " + huskelappOpprettRequest.brukerFnr());
            throw new RuntimeException("Kunne ikke opprette huskelapp", e);
        }
    }

    public void redigerHuskelapp(HuskelappRedigerRequest huskelappRedigerRequest, VeilederId veilederId) {
        UUID endringsId = UUID.fromString("");
        try {
            endringsId = huskelappRepository.redigerHuskelapp(huskelappRedigerRequest, veilederId);

            AktorId aktorId = hentAktorId(huskelappRedigerRequest.brukerFnr()).getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
            opensearchIndexerV2.updateHuskelapp(aktorId, new Huskelapp(huskelappRedigerRequest.kommentar(), huskelappRedigerRequest.frist()));

        } catch (Exception e) {
            secureLog.error("Kunne ikke redigere huskelapp for fnr: " + huskelappRedigerRequest.brukerFnr() + " HuskelappId: " + huskelappRedigerRequest.huskelappId().toString() + " med endringsId: " + endringsId.toString());
            throw new RuntimeException("Kunne ikke redigere huskelapp", e);
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

	public void slettHuskelapp(UUID huskelappId, Fnr brukerFnr) {
		try {
			huskelappRepository.settHuskelappIkkeAktiv(huskelappId);

			AktorId aktorId = hentAktorId(brukerFnr).getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
			opensearchIndexerV2.sletteHuskelapp(aktorId);
		} catch (Exception e) {
			throw new RuntimeException("Kunne ikke oppdatere huskelapp", e);
		}
	}

    public void slettHuskelapperPaaBruker(Fnr fnr) {
        try {
            huskelappRepository.slettHuskelapperPaaBruker(fnr);

			AktorId aktorId = hentAktorId(fnr).getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
			opensearchIndexerV2.sletteHuskelapp(aktorId);
        } catch (Exception e) {
            secureLog.error("Kunne ikke slette huskelapper for fnr: " + fnr.toString());
            throw new RuntimeException("Kunne ikke slette huskelapp", e);
        }
    }

    private Try<AktorId> hentAktorId(Fnr fnr) {
        return Try.of(() -> aktorClient.hentAktorId(fnr));
    }
}
