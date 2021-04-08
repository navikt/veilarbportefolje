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

const compareAktoridsForm = document.getElementById('compareAktoridsForm');
compareAktoridsForm.addEventListener('submit', handleCompareAktorIds);
const numberOfFnrsToCompare = document.getElementById('numberOfFnrsToCompare')

function handleCompareAktorIds(e) {
    e.preventDefault();
    if (window.confirm('Run comparing of aktorIds?')) {
        fetchData(
            '/veilarbportefolje/api/admin/compareAktorIds',
            {method: 'POST', credentials: 'same-origin', body: numberOfFnrsToCompare},
            'compareAktorIdsResponse'
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
