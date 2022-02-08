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

const oppfolgingRefreshForUserForm = document.getElementById('oppfolgingRefreshForUser');
oppfolgingRefreshForUserForm.addEventListener('submit', handleOppfolgingRefreshForUserForm);
const oppfolgingRefreshForFnr = document.getElementById('oppfolgingRefreshForFnr')

function handleOppfolgingRefreshForUserForm(e) {
    e.preventDefault();
    if (window.confirm('Dette vil hente inn data pa nytt for brukeren.')) {
        fetchData(
            '/veilarbportefolje/api/admin/lastInnOppfolgingForBruker/',
            {method: 'POST', credentials: 'same-origin', body: oppfolgingRefreshForFnr.value},
            'oppfolgingRefreshForUserResponse'
        );
    }
}

const slettOpensearchForm = document.getElementById('slettOpensearchForm')
slettOpensearchForm.addEventListener('submit', handleslettOpensearch);
const aktoerIdInputSlett = document.getElementById('aktoerIdInputSlett')

function handleslettOpensearch(e) {
    e.preventDefault()
    const aktoerId = aktoerIdInputSlett.value;
    if (!window.confirm('Dette vil fjerne brukeren fra Opensearch, er du sikker på at du vil fortsette?')) {
        return;
    }

    if (aktoerId && aktoerId.length > 0) {
        fetchData(
            '/veilarbportefolje/api/admin/fjernBrukerOpensearch',
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

const oppdaterBrukereForm = document.getElementById('oppdaterbrukere');
oppdaterBrukereForm.addEventListener('submit', handleOppdaterBrukere)

function handleOppdaterBrukere(e) {
    e.preventDefault();
    if (window.confirm('Denne operasjonen vil ta litt tid, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/indeks/AlleBrukere`,
            {method: 'POST', credentials: 'same-origin'},
            'oppdaterbrukereResponse'
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

const oppdaterbrukerAktiviteterForAlleForm = document.getElementById('oppdaterallebrukerAktiviteter');
oppdaterbrukerAktiviteterForAlleForm.addEventListener('submit', handleOppdaterBrukerAktiviteterForAlle)

function handleOppdaterBrukerAktiviteterForAlle(e) {
    e.preventDefault();
    if (window.confirm('Denne operasjonen vil ta litt tid, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/brukerAktiviteter/allUsers`,
            {method: 'PUT', credentials: 'same-origin'},
            'oppdaterallebrukerAktiviteterResponse'
        );
    }
}

const oppdaterYtelserForAlleForm = document.getElementById('oppdateralleYtelser');
oppdaterYtelserForAlleForm.addEventListener('submit', handleOppdaterYtelserForAlle)

function handleOppdaterYtelserForAlle(e) {
    e.preventDefault();
    if (window.confirm('Denne operasjonen vil ta litt tid, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/ytelser/allUsers`,
            {method: 'PUT', credentials: 'same-origin'},
            'oppdateralleYtelserResponse'
        );
    }
}

const migrerArbeidslistaForm = document.getElementById('migrerArbeidslista');
migrerArbeidslistaForm.addEventListener('submit', handleMigrerArbeidslista)

function handleMigrerArbeidslista(e) {
    e.preventDefault();
    if (window.confirm('Denne operasjonen vil ta litt tid, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/arbeidslista/migrer`,
            {method: 'PUT', credentials: 'same-origin'},
            'migrerArbeidslistaResponse'
        );
    }
}

const migrerRegistreringForm = document.getElementById('migrerRegistrering');
migrerRegistreringForm.addEventListener('submit', handleMigrerRegistrering)

function handleMigrerRegistrering(e) {
    e.preventDefault();
    if (window.confirm('Denne operasjonen vil ta litt tid, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/registrering/migrer`,
            {method: 'PUT', credentials: 'same-origin'},
            'migrerRegistreringResponse'
        );
    }
}

const migrerProfileringForm = document.getElementById('migrerProfilering');
migrerProfileringForm.addEventListener('submit', handleMigrerProfilering)

function handleMigrerProfilering(e) {
    e.preventDefault();
    if (window.confirm('Denne operasjonen vil ta litt tid, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/profilering/migrer`,
            {method: 'PUT', credentials: 'same-origin'},
            'migrerProfileringResponse'
        );
    }
}

const createIndexForm = document.getElementById('createIndexForm');
createIndexForm.addEventListener('submit', handleCreateIndexForm)

function handleCreateIndexForm(e) {
    e.preventDefault();
    if (window.confirm('Oprett ny Opensearch indeks, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/opensearch/createIndex`,
            {method: 'POST', credentials: 'same-origin'},
            'createIndexFormResponse'
        );
    }
}

const getAliasesForm = document.getElementById('getAliasesForm');
getAliasesForm.addEventListener('submit', handleGetAliasesForm)

function handleGetAliasesForm(e) {
    e.preventDefault();
    fetchData(
        `/veilarbportefolje/api/admin/opensearch/getAliases`,
        {method: 'GET', credentials: 'same-origin'},
        'getAliasesFormResponse'
    );
}

const assignAliasToIndexForm = document.getElementById('assignAliasToIndexForm');
assignAliasToIndexForm.addEventListener('submit', handleAssignAliasToIndexForm)
const indexNameEl = document.getElementById('indexName');

function handleAssignAliasToIndexForm(e) {
    e.preventDefault();
    if (window.confirm('Opprett alias for indeks, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/opensearch/assignAliasToIndex`,
            {method: 'POST', credentials: 'same-origin', body: indexNameEl.value},
            'assignAliasToIndexFormResponse'
        );
    }
}

const deleteIndexForm = document.getElementById('deleteIndexForm');
deleteIndexForm.addEventListener('submit', handleDeleteIndexForm)
const deleteIndexNameEl = document.getElementById('deleteIndexName');

function handleDeleteIndexForm(e) {
    e.preventDefault();
    if (window.confirm('Sletter alias for indeks, er du sikker?')) {
        fetchData(
            `/veilarbportefolje/api/admin/opensearch/deleteIndex`,
            {method: 'POST', credentials: 'same-origin', body: deleteIndexNameEl.value},
            'deleteIndexRespons'
        );
    }
}

const getSettingsIndexForm = document.getElementById('getSettingsIndexForm');
getSettingsIndexForm.addEventListener('submit', handlegetSettingsIndexFormForm)
const getSettingsIndexNameEl = document.getElementById('getSettingsIndexName');

function handlegetSettingsIndexFormForm(e) {
    e.preventDefault();
    fetchData(
        `/veilarbportefolje/api/admin/opensearch/getSettings`,
        {method: 'POST', credentials: 'same-origin', body: getSettingsIndexNameEl.value},
        'getSettingsIndexRespons'
    );
}

const fixReadOnlyModeForm = document.getElementById('fixReadOnlyModeForm');
fixReadOnlyModeForm.addEventListener('submit', handleFixReadOnlyModeForm)

function handleFixReadOnlyModeForm(e) {
    e.preventDefault();
    if (window.confirm('Er du sikker på at du vil kjøre en kommando relatert til ReadOnlyMode?')) {
        fetchData(
            `/veilarbportefolje/api/admin/opensearch/fixReadOnlyMode`,
            {method: 'POST', credentials: 'same-origin'},
            'fixReadOnlyModeRespons'
        );
    }
}

const forceShardAssignmentForm = document.getElementById('forceShardAssignmentForm');
forceShardAssignmentForm.addEventListener('submit', handleforceShardAssignmentForm)

function handleforceShardAssignmentForm(e) {
    e.preventDefault();
    if (window.confirm('Er du sikker på at du vil tvinge shard assignment?')) {
        fetchData(
            `/veilarbportefolje/api/admin/opensearch/forceShardAssignment`,
            {method: 'POST', credentials: 'same-origin'},
            'forceShardAssignmentRespons'
        );
    }
}

const testForm = document.getElementById('testPostgresForm')
testForm.addEventListener('submit', handleTest);
function handleTest(e) {
    e.preventDefault()
    if (window.confirm('Er du sikker på at du vil starte testen?')) {
        fetchData(
            '/veilarbportefolje/api/admin/test/postgresIndeksering',
            {method: 'POST', credentials: 'same-origin'}
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
