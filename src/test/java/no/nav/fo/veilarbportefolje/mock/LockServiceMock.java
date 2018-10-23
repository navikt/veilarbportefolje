package no.nav.fo.veilarbportefolje.mock;

import no.nav.fo.veilarbportefolje.service.LockService;

public class LockServiceMock implements LockService {

    @Override
    public void runWithLock(Runnable runnable) {
        runnable.run();
    }

}
