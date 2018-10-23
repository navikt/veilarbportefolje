package no.nav.fo.veilarbportefolje.service;

public interface LockService {

    void runWithLock(Runnable runnable);

}
