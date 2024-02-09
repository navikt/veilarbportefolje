package no.nav.pto.veilarbportefolje.ensligforsorger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgerOvergangsstønadTiltak;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Stønadstype;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.VedtakOvergangsstønadArbeidsoppfølging;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.output.EnsligeForsorgerOvergangsstønadTiltakDto;
import no.nav.pto.veilarbportefolje.ensligforsorger.mapping.AktivitetsTypeTilAktivitetsplikt;
import no.nav.pto.veilarbportefolje.interfaces.HandtereOppfolgingData;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.ensligforsorger.mapping.PeriodetypeTilBeskrivelse.mapPeriodetypeTilBeskrivelse;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnsligeForsorgereService extends KafkaCommonConsumerService<VedtakOvergangsstønadArbeidsoppfølging>
        implements HandtereOppfolgingData<Fnr> {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final EnsligeForsorgereRepository ensligeForsorgereRepository;

    private final AktorClient aktorClient;

    @Override
    protected void behandleKafkaMeldingLogikk(VedtakOvergangsstønadArbeidsoppfølging melding) {
        if (!melding.stønadstype().toString().equals(Stønadstype.OVERGANGSSTØNAD.toString())) {
            log.info("Vi støtter kun overgangstønad for enslige forsorgere. Fått: " + melding.stønadstype());
            return;
        }

        int fjernTidligereVedtak = ensligeForsorgereRepository.fjernTidligereOvergangsstønadVedtak(melding.personIdent());
        if (fjernTidligereVedtak > 0) {
            secureLog.info("Fjernet tidligere vedtak for bruker: {}", melding.personIdent());
        }

        secureLog.info("Oppdatere enslige forsorgere stønad for bruker: {}", melding.personIdent());
        ensligeForsorgereRepository.lagreOvergangsstonad(melding);

        Optional<EnsligeForsorgerOvergangsstønadTiltakDto> ensligeForsorgerOvergangsstønadTiltakDto = hentEnsligeForsorgerOvergangsstønadTiltak(melding.personIdent());
        AktorId aktorId = aktorClient.hentAktorId(Fnr.of(melding.personIdent()));

        if (ensligeForsorgerOvergangsstønadTiltakDto.isPresent()) {
            opensearchIndexerV2.updateOvergangsstonad(aktorId, ensligeForsorgerOvergangsstønadTiltakDto.get());
        } else {
            opensearchIndexerV2.deleteOvergansstonad(aktorId);
        }
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

    public void slettOppfolgingData(Fnr fnr) {
        ensligeForsorgereRepository.fjernTidligereOvergangsstønadVedtak(fnr.get());

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
}
