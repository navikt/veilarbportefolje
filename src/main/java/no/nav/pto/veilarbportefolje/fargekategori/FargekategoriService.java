package no.nav.pto.veilarbportefolje.fargekategori;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriController.OppdaterFargekategoriRequest;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class FargekategoriService {

    private final FargekategoriRepository fargekategoriRepository;
    private final PdlIdentRepository pdlIdentRepository;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final AktorClient aktorClient;
    private final BrukerServiceV2 brukerServiceV2;

    public Optional<FargekategoriEntity> hentFargekategoriForBruker(FargekategoriController.HentFargekategoriRequest request) {
        // TODO skal vi returnere FARGEKATEGORIVERDI.INGEN_KATEGORI om vi ikkje finn kategori i databasen?

        return fargekategoriRepository.hentFargekategoriForBruker(request.fnr());
    }

    public Optional<UUID> oppdaterFargekategoriForBruker(OppdaterFargekategoriRequest request, VeilederId sistEndretAv) {
        if (request.fargekategoriVerdi() == FargekategoriVerdi.INGEN_KATEGORI) {
            fargekategoriRepository.deleteFargekategori(request.fnr());

            AktorId aktorId = Optional.ofNullable(pdlIdentRepository.hentAktorId(request.fnr())).orElseThrow(RuntimeException::new);
            opensearchIndexerV2.slettFargekategori(aktorId);

            return Optional.empty();
        } else {
            UUID oppdatertKategori = fargekategoriRepository.upsertFargekateori(request, sistEndretAv);

            AktorId aktorId = Optional.ofNullable(pdlIdentRepository.hentAktorId(request.fnr())).orElseThrow(RuntimeException::new);
            opensearchIndexerV2.updateFargekategori(aktorId, request.fargekategoriVerdi().name());

            return Optional.of(oppdatertKategori);
        }
    }

    public Validation<String, Fnr> erVeilederForBruker(String fnr) {
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

        boolean erVeilederForBruker =
                ValideringsRegler
                        .validerFnr(fnr)
                        .map(validFnr -> erVeilederForBruker(validFnr, veilederId))
                        .getOrElse(false);

        if (erVeilederForBruker) {
            return valid(Fnr.ofValidFnr(fnr));
        }
        return invalid(format("Veileder %s er ikke veileder for bruker med fnr %s", veilederId, fnr));
    }


    public Boolean erVeilederForBruker(Fnr fnr, VeilederId veilederId) {
        return hentAktorId(fnr)
                .map(aktoerId -> erVeilederForBruker(aktoerId, veilederId))
                .getOrElse(false);
    }

    public Boolean erVeilederForBruker(AktorId aktoerId, VeilederId veilederId) {
        return brukerServiceV2
                .hentVeilederForBruker(aktoerId)
                .map(currentVeileder -> currentVeileder.equals(veilederId))
                .orElse(false);
    }

    public void fjernFargekategoriForBruker(Fnr fnr) {
        fargekategoriRepository.deleteFargekategori(fnr);
    }

    private Try<AktorId> hentAktorId(Fnr fnr) {
        return Try.of(() -> aktorClient.hentAktorId(fnr));
    }
}
