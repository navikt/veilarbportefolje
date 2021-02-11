package no.nav.pto.veilarbportefolje.pdldata;

import no.nav.common.client.pdl.PdlClient;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class PdlDataService {
    private final PdlRepository pdlRepository;
    private final PdlClient pdlClient;

    @Autowired
    public PdlDataService(PdlRepository pdlRepository, PdlClient pdlClient) {
        this.pdlRepository = pdlRepository;
        this.pdlClient = pdlClient;
    }

    public void initBruker(AktorId aktorId){
        String fodselsdag = pdlClient.rawRequest("fin fodselsdag på fnr");
        pdlRepository.upsert(aktorId, Timestamp.valueOf(fodselsdag));
    }

}
