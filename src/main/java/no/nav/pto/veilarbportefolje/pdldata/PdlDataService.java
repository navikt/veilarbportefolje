package no.nav.pto.veilarbportefolje.pdldata;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PdlDataService {
    private final PdlRepository pdlRepository;
    private final PdlClient pdlClient;
    private final BrukerRepository brukerRepository;

    @Autowired
    public PdlDataService(PdlRepository pdlRepository, PdlClient pdlClient, BrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
        this.pdlRepository = pdlRepository;
        this.pdlClient = pdlClient;
    }

    public void lastInnPdlData(AktorId aktorId){
        String fodselsdag = pdlClient.rawRequest("fin fodselsdag på fnr");
        pdlRepository.upsert(aktorId, DateUtils.dateToTime(fodselsdag));
    }

    public void slettPdlData(AktorId aktorId){
        pdlRepository.slettPdlData(aktorId);
    }

    public void lastInnDataFraDbLinkTilPdlDataTabell(){
        List<OppfolgingsBruker> brukere = brukerRepository.hentAlleBrukereUnderOppfolging();
        log.info("lastInnDataFraDbLinkTilPdlDataTabell: Hentet {} oppfølgingsbrukere fra databasen", brukere.size());
        pdlRepository.saveBatch(brukere);
        log.info("lastInnDataFraDbLinkTilPdlDataTabell: fullført");
    }

}
