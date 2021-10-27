package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Service
public class VeilederTilordnetService extends KafkaCommonConsumerService<VeilederTilordnetDTO> implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ArbeidslisteService arbeidslisteService;
    private final ElasticServiceV2 elasticServiceV2;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    @Autowired
    public VeilederTilordnetService(OppfolgingRepository oppfolgingRepository, OppfolgingRepositoryV2 oppfolgingRepositoryV2, ArbeidslisteService arbeidslisteService, ElasticServiceV2 elasticServiceV2) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        this.arbeidslisteService = arbeidslisteService;
        this.elasticServiceV2 = elasticServiceV2;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final VeilederTilordnetDTO dto = JsonUtils.fromJson(kafkaMelding, VeilederTilordnetDTO.class);
        behandleKafkaMeldingLogikk(dto);
    }

    @Override
    protected void behandleKafkaMeldingLogikk(VeilederTilordnetDTO dto) {
        final AktorId aktoerId = dto.getAktorId();
        final VeilederId veilederId = dto.getVeilederId();

        oppfolgingRepository.settVeileder(aktoerId, veilederId);
        oppfolgingRepositoryV2.settVeileder(aktoerId, veilederId);

        elasticServiceV2.oppdaterVeileder(aktoerId, veilederId);
        log.info("Oppdatert bruker: {}, til veileder med id: {}", aktoerId, veilederId);

        // TODO: Slett oracle basert kode naar vi er over paa postgres.
        final boolean harByttetNavKontorPostgres = arbeidslisteService.brukerHarByttetNavKontorPostgres(aktoerId);
        if (harByttetNavKontorPostgres) {
            arbeidslisteService.slettArbeidslistePostgres(aktoerId);
        }

        final boolean harByttetNavKontor = arbeidslisteService.brukerHarByttetNavKontorOracle(aktoerId);
        if (harByttetNavKontor) {
            arbeidslisteService.slettArbeidsliste(aktoerId);
        }
    }

    @Override
    public boolean shouldRewind() {
        return rewind.get();
    }

    @Override
    public void setRewind(boolean rewind) {
        this.rewind.set(rewind);
    }
}
