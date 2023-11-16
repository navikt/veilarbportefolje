package no.nav.pto.veilarbportefolje.huskeliste;

import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappInputDto;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappOutputDto;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class HuskelappService {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final MetricsClient metricsClient;


    public HuskelappRepository huskelappRepository;


    public UUID opprettHuskelapp(HuskelappInputDto inputDto) {
        //db
        //opensearch
        return null;
    }

    public List<HuskelappOutputDto> hentHuskelapp(VeilederId veilederId, EnhetId enhetId) {
        return huskelappRepository.hentHuskelapp(enhetId, veilederId);
    }

    public HuskelappOutputDto hentHuskelapp(Fnr brukerFnr, VeilederId) {
        boolean erVeilederForBruker = validerErVeilederForBruker(brukerFnr);

        if (erVeilederForBruker) {
            return huskelappRepository.hentHuskelapp(brukerFnr);
        }

        throw new RuntimeException("Veileder har ikke tilgang til å se huskelappen til bruker.");
    }

    public HuskelappOutputDto slettHuskelapp(String huskelappId) {
        return null;
    }

    public HuskelappOutputDto oppdatereStatus(String huskelappId, HuskelappStatus status) {
        return null;
    }

    public HuskelappOutputDto oppdatereArkivertDato(String huskelappId) {
        return null;
    }


}
