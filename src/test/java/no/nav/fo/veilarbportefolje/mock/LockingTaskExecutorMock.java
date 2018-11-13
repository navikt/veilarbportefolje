package no.nav.fo.veilarbportefolje.mock;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

public class LockingTaskExecutorMock implements LockingTaskExecutor {

    @Override
    public void executeWithLock(Runnable task, LockConfiguration lockConfig) {
        task.run();
    }
}
