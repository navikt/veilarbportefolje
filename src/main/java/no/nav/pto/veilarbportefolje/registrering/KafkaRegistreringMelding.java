package no.nav.pto.veilarbportefolje.registrering;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.AktoerId;

@Data
@Accessors(chain = true)
public class KafkaRegistreringMelding {
    AktoerId aktoerId;
    BrukersSituasjon brukersSituasjon;

    public enum BrukersSituasjon {
        MISTET_JOBBEN,
        ALDRI_HATT_JOBB,
        HAR_SAGT_OPP,
        VIL_BYTTE_JOBB,
        ER_PERMITTERT,
        USIKKER_JOBBSITUASJON,
        JOBB_OVER_2_AAR,
        VIL_FORTSETTE_I_JOBB,
        AKKURAT_FULLFORT_UTDANNING,
        DELTIDSJOBB_VIL_MER,
    }

}
