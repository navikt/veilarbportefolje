package no.nav.pto.veilarbportefolje.auth;


public record DownstreamApi(String cluster, String namespace, String serviceName) {

    @Override
    public String toString(){
        return cluster + ":" + namespace + ":" + serviceName;
    }
}
