const slettElasticForm = document.getElementById('slettElasticForm')
slettElasticForm.addEventListener('submit', handleslettElastic);
const aktoerIdInputSlett = document.getElementById('aktoerIdInputSlett')

function handleslettElastic(e) {
    e.preventDefault()
    const aktoerId = aktoerIdInputSlett.value;
    if (!window.confirm('Dette vil fjerne brukeren fra elastic, er du sikker på at du vil fortsette?')) {
        return;
    }

    if (aktoerId && aktoerId.length > 0) {
        fetchData(
            '/veilarbportefolje/api/admin/fjernBrukerElastic',
            {method: 'DELETE', credentials: 'same-origin', body: aktoerId},
            'slettingResponse'
        )
    }
}


const oppfolgingsbrukerForm = document.getElementById('oppfolgingsbruker')
oppfolgingsbrukerForm.addEventListener('submit', handleFjernOppfolgingsbruker);

const oppfolgingsbrukerInput = document.getElementById('oppfolgingsbrukerInput')

function handleFjernOppfolgingsbruker(e) {
    e.preventDefault()

    const aktoerId = oppfolgingsbrukerInput.value;

    if (!window.confirm('Dette vil fjerne brukeren fra oversikten, er du sikker på at du vil fortsette?')) {
        return;
    }

    if (aktoerId && aktoerId.length > 0) {
        fetchData(
            '/veilarbportefolje/api/admin/oppfolgingsbruker',
            {method: 'DELETE', credentials: 'same-origin', body: aktoerId},
            'oppfolgingsbrukerResponse'
        )
    }
}

const aktoerIdForm = document.getElementById('brukerident')
aktoerIdForm.addEventListener('submit', handleAktorId);

const aktoerIdInput = document.getElementById('aktoerIdInput')

function handleAktorId(e) {
    e.preventDefault()

    const fnr = aktoerIdInput.value;
    if (fnr && fnr.length > 0) {
        fetchData(
            '/veilarbportefolje/api/admin/aktoerId',
            {method: 'POST', credentials: 'same-origin', body: fnr},
            'aktoerIdResponse'
        )
    }
}

/*
const oppfolgingRefreshForm = document.getElementById('oppfolgingRefresh');
oppfolgingRefreshForm.addEventListener('submit', handleOppfolgingRefreshForm);

function handleOppfolgingRefreshForm(e) {
    e.preventDefault();
    if (window.confirm('Dette vil hente inn data pa nytt for alle brukere under oppfolging.')) {
        fetchData(
            '/veilarbportefolje/api/admin/lastInnOppfolging',
            {method: 'POST', credentials: 'same-origin'},
            'oppfolgingRefreshResponse'
        );
    }
}

const registreringForm = document.getElementById('registrering');
registreringForm.addEventListener('submit', handleRewindRegistrering);

function handleRewindRegistrering(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese inn alle kafka meldinger fra registrering fra starten av.')) {
        fetchData(
            '/veilarbportefolje/api/admin/rewind/registrering',
            {method: 'POST', credentials: 'same-origin'},
            'registreringResponse'
        );
    }
}

const aktiviteterForm = document.getElementById('aktiviteter');
aktiviteterForm.addEventListener('submit', handleRewindAktiviteter);

function handleRewindAktiviteter(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese inn alle kafka meldinger fra aktivteter fra starten av.')) {
        fetchData(
            '/veilarbportefolje/api/admin/rewind/aktivtet',
            {method: 'POST', credentials: 'same-origin'},
            'aktiviteterResponse'
        );
    }
}

const vedtakForm = document.getElementById('vedtak');
vedtakForm.addEventListener('submit', handleRewindVedtak);

function handleRewindVedtak(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese inn alle kafka meldinger fra vedtak fra starten av.')) {
        fetchData(
            '/veilarbportefolje/api/admin/rewind/vedtak',
            {method: 'POST', credentials: 'same-origin'},
            'vedtakResponse'
        );
    }
}


const nyForVeiledereForm = document.getElementById('nyForVeileder');
nyForVeiledereForm.addEventListener('submit', handleRewindNyForVeiledere);

function handleRewindNyForVeiledere(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese inn alle kafka meldinger fra topiken fra starten av.')) {
        fetchData(
            '/veilarbportefolje/api/admin/rewind/nyForVeileder',
            {method: 'POST', credentials: 'same-origin'},
            'nyForVeilederResponse'
        );
    }
}

const cvEksistereForm = document.getElementById('cvEksistere');
cvEksistereForm.addEventListener('submit', handleRewindCVEksistere);

function handleRewindCVEksistere(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese inn alle kafka meldinger fra topiken fra starten av.')) {
        fetchData(
            '/veilarbportefolje/api/admin/rewind/cv-eksisterer',
            {method: 'POST', credentials: 'same-origin'},
            'cvEksistereResponse'
        );
    }
}

const tilordnetVeileder = document.getElementById('tilordnetVeileder');
tilordnetVeileder.addEventListener('submit', handleRewindTilordnetVeileder);

function handleRewindTilordnetVeileder(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese inn alle kafka meldinger fra topiken fra starten av.')) {
        fetchData(
            '/veilarbportefolje/api/admin/rewind/tilordnet-veileder',
            {method: 'POST', credentials: 'same-origin'},
            'tilordnetVeilederResponse'
        );
    }
}

const aktoerIdSamtykkeForm = document.getElementById('aktoerIdSamtykkeForm');
aktoerIdSamtykkeForm.addEventListener('submit', handleSamtykkeDeltCV);
const aktoerIdSamtykkeInput = document.getElementById('aktoerIdSamtykkeInput');

function handleSamtykkeDeltCV(e) {
    e.preventDefault()

    const aktoerIdSamtykke = aktoerIdSamtykkeInput.value;
    if (aktoerIdSamtykke && aktoerIdSamtykke > 0) {
        if (window.confirm(`Dette vil sette HAR_DELT_CV for: ${aktoerIdSamtykke}`)) {
            fetchData(
                '/veilarbportefolje/api/admin/settSamtykkeCV',
                {method: 'POST', credentials: 'same-origin', body: aktoerIdSamtykke},
                'aktoerIdSamtykkeResponse'
            )
        }
    }
}

 */

const startAiven = document.getElementById('startAiven');
startAiven.addEventListener('submit', handleStartAiven);

function handleStartAiven(e) {
    e.preventDefault();
    if (window.confirm('Vil du starte konsumering av Aiven?')) {
        fetchData(
            '/veilarbportefolje/api/admin/start/aiven-konsumering',
            {method: 'POST', credentials: 'same-origin'},
            'startAivenResponse'
        );
    }
}

const stoppAiven = document.getElementById('stoppAiven');
stoppAiven.addEventListener('submit', handleStoppAiven);

function handleStoppAiven(e) {
    e.preventDefault();
    if (window.confirm('Vil du stoppe konsumering av Aiven?')) {
        fetchData(
            '/veilarbportefolje/api/admin/stopp/aiven-konsumering',
            {method: 'POST', credentials: 'same-origin'},
            'stoppAivenResponse'
        );
    }
}

const oppdaterBrukerForm = document.getElementById('oppdaterbruker');
oppdaterBrukerForm.addEventListener('submit', handleOppdaterBruker)
const fnrInputOppdater = document.getElementById('fnrInputOppdater');

function handleOppdaterBruker(e) {
    e.preventDefault();

    const fnr = fnrInputOppdater.value;
    if (fnr && fnr.length > 0) {
        fetchData(
            `/veilarbportefolje/api/admin/indeks/bruker`,
            {method: 'PUT', credentials: 'same-origin', body: fnr},
            'oppdaterbrukerResponse'
        );
    }
}


const oppdaterbrukerAktiviteterForm = document.getElementById('oppdaterbrukerAktiviteter');
oppdaterbrukerAktiviteterForm.addEventListener('submit', handleOppdaterBrukerAktiviteter)
const fnrInputOppdaterAktiviteter = document.getElementById('fnroppdaterbrukerAktiviteter');

function handleOppdaterBrukerAktiviteter(e) {
    e.preventDefault();

    const fnr = fnrInputOppdaterAktiviteter.value;
    if (fnr && fnr.length > 0) {
        fetchData(
            `/veilarbportefolje/api/admin/brukerAktiviteter`,
            {method: 'PUT', credentials: 'same-origin', body: fnr},
            'oppdaterbrukerAktiviteterResponse'
        );
    }
}

function sjekkStatus(resp) {
    if (!resp.ok) {
        console.log('resp', resp);
        throw new Error(`${resp.status} ${resp.statusText}`);
    }
    return resp;
}

function fetchData(url, init, dataContainerId) {
    fetch(url, init)
        .then(sjekkStatus)
        .then(resp => resp.text())
        .then(resp => document.getElementById(dataContainerId).innerHTML = resp)
        .catch(e => alert(e))
}
