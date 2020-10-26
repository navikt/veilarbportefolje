package no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak;


import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.database.EnhetTiltakRepository;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TiltakService {
    private final EnhetTiltakRepository enhetTiltakRepository;

    @Autowired
    public TiltakService(EnhetTiltakRepository enhetTiltakRepository) {
        this.enhetTiltakRepository = enhetTiltakRepository;
    }

    public Try<EnhetTiltak> hentEnhettiltak(String enhet) {
        return enhetTiltakRepository.retrieveEnhettiltak(enhet);
    }
}
