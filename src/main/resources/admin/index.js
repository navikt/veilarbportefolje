const indekseringForm = document.getElementById('registrering');
indekseringForm.addEventListener('submit', handleRewindRegistrering);

function handleRewindRegistrering(e) {
    e.preventDefault();
    if (window.confirm('Dette vil lese inn alle kafka meldinger fra registrering fra starten av.')) {
        fetchData(
            '/veilarbindexer/api/admin/rewind/registrering',
            {method: 'POST', credentials: 'same-origin'},
            'registreringResponse'
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
