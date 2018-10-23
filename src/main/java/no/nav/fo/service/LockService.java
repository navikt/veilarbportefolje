package no.nav.fo.service;

public interface LockService {

    void runWithLock(Runnable runnable);

}
