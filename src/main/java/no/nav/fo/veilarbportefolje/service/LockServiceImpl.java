package no.nav.fo.veilarbportefolje.service;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

import java.time.Instant;

public class LockServiceImpl implements LockService {

    private LockingTaskExecutor lockingTaskExecutor;

    public LockServiceImpl(LockingTaskExecutor lockingTaskExecutor) {
        this.lockingTaskExecutor = lockingTaskExecutor;
    }

    public void runWithLock(Runnable runnable) {
        Instant lockAtMostUntil = Instant.now().plusSeconds(60 * 60 * 3);
        LockConfiguration lockConfiguration = new LockConfiguration("indeksering", lockAtMostUntil);
        lockingTaskExecutor.executeWithLock(runnable, lockConfiguration);
    }

}
