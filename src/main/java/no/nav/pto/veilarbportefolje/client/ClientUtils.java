package no.nav.pto.veilarbportefolje.client;

import no.nav.pto.veilarbportefolje.auth.DownstreamApi;
import org.jetbrains.annotations.NotNull;

import static no.nav.common.utils.UrlUtils.*;

public class ClientUtils {
    public static String getVeilarboppfolgingServiceUrl() {
        return createServiceUrl("veilarboppfolging", "pto", true);
    }

    public static String getKodeverkServiceUrl() {
        return createServiceUrl("kodeverk", "default", false);
    }

    public static String getVeilarbvedtaksstotteServiceUrl() {
        return createServiceUrl("veilarbvedtaksstotte", "pto", true);
    }

    public static String getPdlServiceUrl() {
        return createServiceUrl("pdl-api", "pdl", false);
    }

    public static String getPoaoTilgangUrl(boolean isProduction) {
        return isProduction ?
                createProdInternalIngressUrl("poao-tilgang") :
                createDevInternalIngressUrl("poao-tilgang");
    }

    static String getVeilarbveilederServiceUrl(DownstreamApi downstreamApi) {
        return createServiceUrl(downstreamApi.serviceName(), downstreamApi.namespace(), true);
    }

    @NotNull
    public static String getVeilarbveilederTokenScope(DownstreamApi downstreamApi) {
        return "api://" + downstreamApi.cluster() + "." + downstreamApi.namespace() + "." + downstreamApi.serviceName() + "/.default";
    }

    public static String getVeilarboppfolgingTokenScope(Boolean isProduction) {
        return String.format("api://%s-fss.pto.veilarboppfolging/.default", isProduction ? "prod" : "dev");
    }

    public static String getVeilarbvedtaksstotteTokenScope(boolean isProduction) {
        return String.format(
                "api://%s-fss.pto.veilarbvedtaksstotte/.default",
                isProduction ? "prod" : "dev"
        );
    }

    public static String getPdlM2MTokenScope(boolean isProduction) {
        return String.format("api://%s-fss.pdl.pdl-api/.default",
                isProduction ? "prod" : "dev"
        );
    }

    public static String getPoaoTilgangTokenScope(boolean isProduction) {
        return String.format("api://%s-gcp.poao.poao-tilgang/.default", isProduction ? "prod" : "dev");
    }

    @NotNull
    static DownstreamApi getVeilarbVeilederDownstreamApi(String cluster) {
        return new DownstreamApi(cluster, "pto", "veilarbveileder");
    }
}
