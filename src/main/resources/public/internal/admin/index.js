const aktoerIdForm = document.getElementById('brukerident')
aktoerIdForm.addEventListener('submit', handleAktoerId);

const aktoerIdInput = document.getElementById('aktoerIdInput')

function handleAktoerId(e) {
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

const indekseringForm = document.getElementById('indeksering');
indekseringForm.addEventListener('submit', handleIndeksering);

function handleIndeksering(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese fra databasen og opprette en ny indeks, dette kan ta en del tid. Er du sikker?')) {
        fetchData(
            '/veilarbportefolje/api/admin/indeks',
            {method: 'POST', credentials: 'same-origin'},
            'indekseringResponse'
        );
    }
}

const hovedindekseringForm = document.getElementById('hovedindeksering');
hovedindekseringForm.addEventListener('submit', handleHovedIndeksering);

function handleHovedIndeksering(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese filer fra arena inn til databasen, lese fra databasen og så opprette en ny indeks, dette kan ta en del tid. Er du sikker?')) {
        fetchData(
            '/veilarbportefolje/api/admin/hovedindeksering',
            {method: 'GET', credentials: 'same-origin'},
            'hovedIndekseringResponse'
        );
    }
}

const oppdaterBrukerForm = document.getElementById('bruker');
oppdaterBrukerForm.addEventListener('submit', handleOppdaterBruker)
const fnrInput = document.getElementById('fnrInput');

function handleOppdaterBruker(e) {
    e.preventDefault();

    const fnr = fnrInput.value;
    if (fnr && fnr.length > 0) {
        fetchData(
            `/veilarbportefolje/api/admin/indeks/bruker`,
            {method: 'PUT', credentials: 'same-origin', body: fnr},
            'oppdaterBrukerResponse'
        );
    }
}

const aktivitetForm = document.getElementById('aktivitet');
aktivitetForm.addEventListener('submit', handleSlettAktivitet)
const aktivitetIdInput = document.getElementById('aktivitetInput');

function handleSlettAktivitet(e) {
    e.preventDefault();

    const id = aktivitetIdInput.value;
    if (id && id.length > 0 && window.confirm('Dette vil slette aktiviteten fra portefølje. Er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/aktivitet/${id}`,
            {method: 'DELETE', credentials: 'same-origin'},
            'slettAktivitetResponse'
        )
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
