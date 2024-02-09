package no.nav.pto.veilarbportefolje.interfaces;

import no.nav.common.types.identer.EksternBrukerId;

public interface HandtereOppfolgingData<T extends EksternBrukerId> {
    void slettOppfolgingData(T ident);
}
