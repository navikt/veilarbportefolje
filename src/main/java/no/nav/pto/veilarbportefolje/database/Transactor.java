package no.nav.pto.veilarbportefolje.database;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static no.nav.common.utils.ExceptionUtils.throwUnchecked;

public class Transactor {

    private final TransactionTemplate transactionTemplate;

    public Transactor(PlatformTransactionManager platformTransactionManager) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    public void inTransaction(InTransaction inTransaction) {
        transactionTemplate.execute(transactionStatus -> {
            try {
                inTransaction.run();
                return null;
            } catch (Throwable throwable) {
                throw throwUnchecked(throwable);
            }
        });
    }

    @FunctionalInterface
    public interface InTransaction {
        void run() throws Throwable;
    }

}