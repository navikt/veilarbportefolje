package no.nav.pto.veilarbportefolje.client;

import no.nav.pto.veilarbportefolje.auth.DownstreamApi;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import org.jetbrains.annotations.NotNull;

import static no.nav.common.utils.UrlUtils.*;

public class ClientUtils {

    public static String getVeilarbvedtaksstotteServiceUrl(EnvironmentProperties environmentProperties) {
        //return createServiceUrl("veilarbvedtaksstotte", "pto", true);
        return environmentProperties.getVeilarbvedtaksstotteUrl();
    }

    public static String getPdlServiceUrl(EnvironmentProperties environmentProperties) {
        //return createServiceUrl("pdl-api", "pdl", false);
        return environmentProperties.getPdlUrl();
    }

    public static String getPoaoTilgangUrl(EnvironmentProperties environmentProperties) {
        //return isProduction ? createProdInternalIngressUrl("poao-tilgang") : createDevInternalIngressUrl("poao-tilgang");
        return environmentProperties.getPoaoTilgangUrl();
    }

    static String getVeilarbveilederServiceUrl(EnvironmentProperties environmentProperties) {
        //return createServiceUrl("veilarbveileder", "pto", true);
        return environmentProperties.getVeilarbveilederUrl();
    }

    @NotNull
    public static String getVeilarbveilederTokenScope(EnvironmentProperties environmentProperties) {
        //return "api://" + cluster + ".pto.veilarbveileder/.default";
        return environmentProperties.getVeilarbveilederScope();
    }

    public static String getVeilarboppfolgingTokenScope(EnvironmentProperties environmentProperties) {
        //return String.format("api://%s-fss.pto.veilarboppfolging/.default", isProduction ? "prod" : "dev");
        return environmentProperties.getVeilarboppfolgingScope();
    }

    public static String getVeilarbvedtaksstotteTokenScope(EnvironmentProperties environmentProperties) {
        //return String.format("api://%s-fss.pto.veilarbvedtaksstotte/.default", isProduction ? "prod" : "dev");
        return environmentProperties.getVeilarbvedtaksstotteScope();
    }

    public static String getPdlM2MTokenScope(EnvironmentProperties environmentProperties) {
        //return String.format("api://%s-fss.pdl.pdl-api/.default", isProduction ? "prod" : "dev");
        return environmentProperties.getPdlScope();
    }

    public static String getPoaoTilgangTokenScope(EnvironmentProperties environmentProperties) {
        //return String.format("api://%s-gcp.poao.poao-tilgang/.default", isProduction ? "prod" : "dev");
        return environmentProperties.getPoaoTilgangScope();
    }
}
