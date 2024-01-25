package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriController.OppdaterFargekategoriRequest;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FargekategoriService {

    private final FargekategoriRepository fargekategoriRepository;
    private final PdlIdentRepository pdlIdentRepository;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    public UUID oppdaterFargekategoriForBruker(OppdaterFargekategoriRequest request, VeilederId sistEndretAv) {
        UUID oppdatertKategori = fargekategoriRepository.upsertFargekateori(request, sistEndretAv);

        AktorId aktorId = Optional.ofNullable(pdlIdentRepository.hentAktorId(request.fnr())).orElseThrow(RuntimeException::new);
        opensearchIndexerV2.updateFargekategori(aktorId, request.fargekategoriVerdi().name());

        return oppdatertKategori;
    }
}
