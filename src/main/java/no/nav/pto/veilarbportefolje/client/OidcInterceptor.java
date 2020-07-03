package no.nav.pto.veilarbportefolje.client;


import no.nav.common.sts.SystemUserTokenProvider;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class OidcInterceptor implements Interceptor {

    private final SystemUserTokenProvider systemUserTokenProvider;

    public OidcInterceptor(SystemUserTokenProvider systemUserTokenProvider) {
        this.systemUserTokenProvider = systemUserTokenProvider;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request newRequest;

        newRequest = request.newBuilder()
                .addHeader("Authorization", "Bearer " + systemUserTokenProvider.getSystemUserToken())
                .build();

        return chain.proceed(newRequest);
    }
}
