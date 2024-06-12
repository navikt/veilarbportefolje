package no.nav.pto.veilarbportefolje.fargekategori;

import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

@TestConfiguration
public class FargekategoriControllerTestConfig {

    static final Fnr TESTBRUKER_FNR = Fnr.of("11111111111");
    static final Fnr TESTBRUKER2_FNR = Fnr.of("22222222222");
    static final Fnr TESTBRUKER3_FNR = Fnr.of("33333333333");
    static final AktorId TESTBRUKER_AKTOR_ID = AktorId.of("99988877766655");
    static final AktorId TESTBRUKER2_AKTOR_ID = AktorId.of("99988877766656");
    static final AktorId TESTBRUKER3_AKTOR_ID = AktorId.of("99988877766657");
    static final NavKontor TESTENHET = NavKontor.of("1234");
    static final NavKontor TESTENHET2 = NavKontor.of("4321");
    static final NavIdent TESTVEILEDER = NavIdent.of("Z999999");

    @Bean
    @Qualifier("fargekategoriControllerTestAuthService")
    AuthContextHolder fargekategoriControllerTestAuthService() {
        AuthContextHolder authContextHolder = AuthContextHolderThreadLocal.instance();
        authContextHolder.setContext(
                new AuthContext(UserRole.INTERN, TestDataUtils.generateJWT(TESTVEILEDER.get(), UUID.randomUUID().toString()))
        );

        return authContextHolder;
    }
}
