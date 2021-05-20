package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class VeilederTilordnetService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ArbeidslisteService arbeidslisteService;
    private final ArbeidslisteService arbeidslisteServicePostgres;
    private final ElasticServiceV2 elasticServiceV2;

    public VeilederTilordnetService(OppfolgingRepository oppfolgingRepository, OppfolgingRepositoryV2 oppfolgingRepositoryV2, ArbeidslisteService arbeidslisteService, ElasticServiceV2 elasticServiceV2,@Qualifier("PostgresArbeidslisteService") ArbeidslisteService arbeidslisteServicePostgres) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        this.arbeidslisteService = arbeidslisteService;
        this.arbeidslisteServicePostgres = arbeidslisteServicePostgres;
        this.elasticServiceV2 = elasticServiceV2;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final VeilederTilordnetDTO dto = JsonUtils.fromJson(kafkaMelding, VeilederTilordnetDTO.class);
        final AktorId aktoerId = dto.getAktorId();

        oppfolgingRepository.settVeileder(aktoerId, dto.getVeilederId());
        oppfolgingRepositoryV2.settVeileder(aktoerId, dto.getVeilederId());

        elasticServiceV2.oppdaterVeileder(aktoerId, dto.getVeilederId());

        // TODO: Slett oracle basert kode naar vi er over paa postgres.
        final boolean harByttetNavKontorPostgres = arbeidslisteServicePostgres.brukerHarByttetNavKontor(aktoerId);
        if (harByttetNavKontorPostgres) {
            arbeidslisteServicePostgres.slettArbeidsliste(aktoerId);
        }

        final boolean harByttetNavKontor = arbeidslisteService.brukerHarByttetNavKontor(aktoerId);
        if (harByttetNavKontor) {
            arbeidslisteService.slettArbeidsliste(aktoerId);
        }
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
