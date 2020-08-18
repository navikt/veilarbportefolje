package no.nav.pto.veilarbportefolje.mock;

import no.nav.common.leaderelection.LeaderElectionClient;

public class LeaderElectionClientMock implements LeaderElectionClient {
    @Override
    public boolean isLeader() {
        return true;
    }
}
