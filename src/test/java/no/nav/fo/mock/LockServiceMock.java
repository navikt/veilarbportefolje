package no.nav.fo.mock;

import no.nav.fo.service.LockService;

public class LockServiceMock implements LockService {

    @Override
    public void runWithLock(Runnable runnable) {
        runnable.run();
    }

}
