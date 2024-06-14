package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class FargekategoriControllerTestConfig {

    static final Fnr TESTBRUKER_FNR = Fnr.of("11111111111");
    static final Fnr TESTBRUKER2_FNR = Fnr.of("22222222222");
    static final Fnr TESTBRUKER3_FNR = Fnr.of("33333333333");
    static final AktorId TESTBRUKER_AKTOR_ID = AktorId.of("99988877766655");
    static final AktorId TESTBRUKER2_AKTOR_ID = AktorId.of("99988877766656");
    static final AktorId TESTBRUKER3_AKTOR_ID = AktorId.of("99988877766657");
    static final NavKontor TESTENHET = NavKontor.of("1234");
}
