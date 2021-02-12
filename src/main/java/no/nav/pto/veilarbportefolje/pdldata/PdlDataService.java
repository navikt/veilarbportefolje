package no.nav.pto.veilarbportefolje.pdldata;

import no.nav.common.client.pdl.PdlClient;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PdlDataService {
    private final PdlRepository pdlRepository;
    private final PdlClient pdlClient;

    @Autowired
    public PdlDataService(PdlRepository pdlRepository, PdlClient pdlClient) {
        this.pdlRepository = pdlRepository;
        this.pdlClient = pdlClient;
    }

    public void lastInnPdlData(AktorId aktorId){
        String fodselsdag = pdlClient.rawRequest("fin fodselsdag på fnr");
        pdlRepository.upsert(aktorId, DateUtils.dateToTimestamp(fodselsdag));
    }

    public void slettPdlData(AktorId aktorId){
        pdlRepository.slettPdlData(aktorId);
    }

    public void lastInnPdlDataPaAlleBrukereUtenData(){
        String fodselsdager = pdlClient.rawRequest("fin fodselsdag på fnr");
        //pdlRepository.upsert(aktorId, DateUtils.dateToTimestamp(fodselsdag));
    }

}
