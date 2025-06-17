package no.nav.pto.veilarbportefolje.ensligforsorger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.ensligforsorger.client.EnsligForsorgerClient;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgerOvergangsstønadTiltak;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.OvergangsstønadBarn;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.OvergangsstønadPeriode;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.OvergangsstønadResponseDto;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.Stønadstype;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.*;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.output.EnsligeForsorgerOvergangsstønadTiltakDto;
import no.nav.pto.veilarbportefolje.ensligforsorger.mapping.AktivitetsTypeTilAktivitetsplikt;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import static no.nav.pto.veilarbportefolje.ensligforsorger.mapping.PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnsligeForsorgereService extends KafkaCommonNonKeyedConsumerService<VedtakOvergangsstønadArbeidsoppfølging> {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final EnsligeForsorgereRepository ensligeForsorgereRepository;
    private final AktorClient aktorClient;
    private final EnsligForsorgerClient ensligForsorgerClient;

    @Override
    protected void behandleKafkaMeldingLogikk(VedtakOvergangsstønadArbeidsoppfølging melding) {
        if (!erStonadstypeOvergangsstonad(melding.stønadstype().toString())) {
            return;
        }

        String personIdent = melding.personIdent();

        fjernTidligereVedtakOmOvergangsstonad(personIdent);

        secureLog.info("Oppdatere enslige forsorgere stønad for bruker: {}", personIdent);
        ensligeForsorgereRepository.lagreOvergangsstonad(melding);
        oppdaterOvergangsstonadIOpenSearch(personIdent);
    }

    public Optional<EnsligeForsorgerOvergangsstønadTiltakDto> hentEnsligeForsorgerOvergangsstønadTiltak(String personIdent) {
        Optional<EnsligeForsorgerOvergangsstønadTiltak> ensligeForsorgerOvergangsstønadTiltakOptional = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(personIdent, true);

        if (ensligeForsorgerOvergangsstønadTiltakOptional.isPresent()) {
            EnsligeForsorgerOvergangsstønadTiltak ensligeForsorgerOvergangsstønadTiltak = ensligeForsorgerOvergangsstønadTiltakOptional.get();
            return Optional.of(getEnsligeForsorgereDto(ensligeForsorgerOvergangsstønadTiltak));
        }
        return Optional.empty();
    }

    public Map<Fnr, EnsligeForsorgerOvergangsstønadTiltakDto> hentEnsligeForsorgerOvergangsstønadTiltak(List<Fnr> personIdents) {
        Map<Fnr, EnsligeForsorgerOvergangsstønadTiltakDto> result = new HashMap<>();
        List<EnsligeForsorgerOvergangsstønadTiltak> ensligeForsorgerOvergangsstønadTiltaks = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(personIdents, true);
        ensligeForsorgerOvergangsstønadTiltaks.forEach(tiltak -> {
            result.putIfAbsent(tiltak.personIdent(), getEnsligeForsorgereDto(tiltak));
        });
        return result;
    }

    public void slettEnsligeForsorgereData(AktorId aktorId) {
        Fnr fnr = aktorClient.hentFnr(aktorId);

        if (fnr != null) {
            ensligeForsorgereRepository.fjernTidligereOvergangsstønadVedtak(fnr.get());
        }
    }

    private EnsligeForsorgerOvergangsstønadTiltakDto getEnsligeForsorgereDto(EnsligeForsorgerOvergangsstønadTiltak ensligeForsorgerOvergangsstønadTiltak) {
        Optional<LocalDate> yngsteBarn = ensligeForsorgereRepository.hentYngsteBarn(ensligeForsorgerOvergangsstønadTiltak.vedtakid());

        Optional<Boolean> harAktivitetsplikt = AktivitetsTypeTilAktivitetsplikt.harAktivitetsplikt(ensligeForsorgerOvergangsstønadTiltak.vedtaksPeriodetype(), ensligeForsorgerOvergangsstønadTiltak.aktivitetsType());
        String vedtakPeriodeBeskrivelse = mapPeriodetypeTilBeskrivelse(ensligeForsorgerOvergangsstønadTiltak.vedtaksPeriodetype());
        return new EnsligeForsorgerOvergangsstønadTiltakDto(
                vedtakPeriodeBeskrivelse,
                harAktivitetsplikt.orElse(null),
                ensligeForsorgerOvergangsstønadTiltak.til_dato(),
                yngsteBarn.orElse(null)
        );
    }

    private boolean erStonadstypeOvergangsstonad(String stonadstype) {
        if (stonadstype.equals(Stønadstype.OVERGANGSSTØNAD.toString())) {
            return true;
        }

        log.info("Vi støtter kun overgangstønad for enslige forsorgere. Fått: " + stonadstype);
        return false;
    }

    private void fjernTidligereVedtakOmOvergangsstonad(String personIdent) {
        int fjernTidligereVedtak = ensligeForsorgereRepository.fjernTidligereOvergangsstønadVedtak(personIdent);
        if (fjernTidligereVedtak > 0) {
            secureLog.info("Fjernet tidligere vedtak for bruker: {}", personIdent);
        }
    }

    private void oppdaterOvergangsstonadIOpenSearch(String personIdent) {
        Optional<EnsligeForsorgerOvergangsstønadTiltakDto> ensligeForsorgerOvergangsstønadTiltakDto = hentEnsligeForsorgerOvergangsstønadTiltak(personIdent);
        AktorId aktorId = aktorClient.hentAktorId(Fnr.of(personIdent));

        if (ensligeForsorgerOvergangsstønadTiltakDto.isPresent()) {
            opensearchIndexerV2.updateOvergangsstonad(aktorId, ensligeForsorgerOvergangsstønadTiltakDto.get());
        } else {
            opensearchIndexerV2.deleteOvergansstonad(aktorId);
        }
    }

    public void hentOgLagreEnsligForsorgerDataFraApi(AktorId aktorId) {
        Fnr fnr = aktorClient.hentFnr(aktorId);
        Optional<OvergangsstønadResponseDto> overgangsstønadResponseDto = ensligForsorgerClient.hentEnsligForsorgerOvergangsstonad(fnr);
        List<OvergangsstønadPeriode> ensligForsorgerPerioder = overgangsstønadResponseDto.get().getData().getPerioder();
        if(fnr != null && !ensligForsorgerPerioder.isEmpty()) {
            for(OvergangsstønadPeriode periode: ensligForsorgerPerioder) {
                VedtakOvergangsstønadArbeidsoppfølging overgangsstønadDto = ensligForsorgerDataMapper(fnr, periode);
                ensligeForsorgereRepository.lagreOvergangsstonad(overgangsstønadDto);
                secureLog.info("Hentet overgangsstønad for bruker {} med perioder {} ", fnr, periode);
            }
        } else {
            secureLog.info("Data om enslig forsorger for brukeren {} finnes ikke", aktorId);
        }
    }

    private VedtakOvergangsstønadArbeidsoppfølging ensligForsorgerDataMapper(Fnr personIdent, OvergangsstønadPeriode ensligForsorgerPeriode) {
        Periode periode = new Periode(
                ensligForsorgerPeriode.getStønadFraOgMed(),
                ensligForsorgerPeriode.getStønadTilOgMed(),
                ensligForsorgerPeriode.getPeriodeType(),
                ensligForsorgerPeriode.getAktivitet());

        List<OvergangsstønadBarn> ensligForsorgersBarn =  ensligForsorgerPeriode.getBarn();
        List<Barn> barnListe = new ArrayList<>();

        for(OvergangsstønadBarn barn: ensligForsorgersBarn) {
            barnListe.add(new Barn(
                    barn.getPersonIdent(),
                    barn.getFødselTermindato()
            ));
        }

        return new VedtakOvergangsstønadArbeidsoppfølging(
                ensligForsorgerPeriode.getBehandlingId(),
                personIdent.get(),
                barnListe,
                Stønadstype.OVERGANGSSTØNAD,
                List.of(periode),
                Vedtaksresultat.INNVILGET
        );
    }
}
