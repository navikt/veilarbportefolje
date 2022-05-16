package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.util.FodselsnummerUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlBrukerdataKafkaService extends KafkaCommonConsumerService<Personhendelse> {
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;
    private final OpensearchIndexer opensearchIndexer;

    @Override
    public void behandleKafkaMeldingLogikk(Personhendelse melding) {
        if (melding == null || melding.getPersonidenter().size() == 0) {
            log.info("""
                            Fikk tom endrings melding fra PDL.
                            Dette er en tombstone som kan ignoreres hvis man sletter alle historiske identer lenket til nye identer.
                    """);
            return;
        }
        Fnr fnr = Fnr.of(String.valueOf(melding.getFolkeregisteridentifikator().getIdentifikasjonsnummer()));
        PDLPerson person = mapTilPdlIdenter(melding, fnr);
        List<Fnr> alleFolkeregistrerteIdenter = hentAlleFolkeregistrerteIdenter(melding);

        if (pdlIdentRepository.harFnrUnderOppfolging(alleFolkeregistrerteIdenter)) {
            AktorId aktorId = pdlIdentRepository.hentAktorId(alleFolkeregistrerteIdenter);

            log.info("Det oppsto en brukerdata endring aktoer: {}", aktorId);
            pdlPersonRepository.upsertPerson(person);
            opensearchIndexer.indekser(aktorId);
        }
    }

    private List<Fnr> hentAlleFolkeregistrerteIdenter(Personhendelse melding) {
        return melding.getPersonidenter().stream()
                .map(CharSequence::toString)
                .map(Fnr::new).toList();
    }

    private PDLPerson mapTilPdlIdenter(Personhendelse melding, Fnr fnr) {
        return new PDLPerson()
                .setFornavn(melding.getNavn().getFornavn().toString())
                .setEtternavn(melding.getNavn().getEtternavn().toString())
                .setMellomnavn(melding.getNavn().getMellomnavn().toString())
                .setFoedsel(melding.getFoedsel().getFoedselsdato())
                .setErDoed(melding.getDoedsfall().getDoedsdato() != null)
                .setKjonn(Kjonn.valueOf(FodselsnummerUtils.lagKjonn(fnr.get())));
        // TODO: Purr på PDL om å legge til kjoenn i kafka meldingen
    }
}
