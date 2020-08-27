package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.database.Transactor;

class TestTransactor extends Transactor {

    public TestTransactor() {
        super(null);
    }

    @Override
    @SneakyThrows
    public void inTransaction(InTransaction inTransaction) {
        inTransaction.run();
    }

}
