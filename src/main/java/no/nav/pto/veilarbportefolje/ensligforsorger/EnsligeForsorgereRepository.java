package no.nav.pto.veilarbportefolje.ensligforsorger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.VedtakOvergangsstønadArbeidsoppfølging;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgerOvergangsstønadTiltak;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgereBarn;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgereVedtakPeriode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.util.DateUtils.fnrToDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class EnsligeForsorgereRepository {
    private final JdbcTemplate db;

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    private final Cache<String, Integer> stonadstypeCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();
    private final Cache<String, Integer> vedtakresultatCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();

    private final Cache<String, Integer> periodeTypeCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();

    private final Cache<String, Integer> vettakAktivitetstypeCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();

    public void lagreEnsligeForsorgereStonad(VedtakOvergangsstønadArbeidsoppfølging vedtakOvergangsstønadArbeidsoppfølging) {
        if (vedtakOvergangsstønadArbeidsoppfølging.getStønadstype().toString().equals("OVERGANGSSTØNAD")) {
            lagreOvergangsstonad(vedtakOvergangsstønadArbeidsoppfølging);
        } else {
            log.info("Vi støtter kun overgangstønad for enslige forsorgere. Fått: " + vedtakOvergangsstønadArbeidsoppfølging.getStønadstype());
        }
    }

    public void lagreOvergangsstonad(VedtakOvergangsstønadArbeidsoppfølging vedtakOvergangsstønadArbeidsoppfølging) {

        long vedtakId = vedtakOvergangsstønadArbeidsoppfølging.getVedtakId();
        Integer stonadstypeId = hentStonadstype(vedtakOvergangsstønadArbeidsoppfølging.getStønadstype().toString());
        Integer vedtakResultatId = hentVedtakresultat(vedtakOvergangsstønadArbeidsoppfølging.getVedtaksresultat().toString());

        String sql = """
                INSERT INTO ENSLIGE_FORSORGERE(VEDTAKID, PERSONIDENT, STØNADSTYPE, VEDTAKSRESULTAT)
                VALUES (?, ?, ?, ?) ON CONFLICT (VEDTAKID)
                DO UPDATE SET(PERSONIDENT, STØNADSTYPE, VEDTAKSRESULTAT)
                = (excluded.PERSONIDENT, excluded.STØNADSTYPE, excluded.VEDTAKSRESULTAT);
                """;
        db.update(sql, vedtakId, vedtakOvergangsstønadArbeidsoppfølging.getPersonIdent(),
                stonadstypeId, vedtakResultatId);

        List<EnsligeForsorgereVedtakPeriode> ensligeForsorgereVedtakPerioder = vedtakOvergangsstønadArbeidsoppfølging.getPeriode().stream().map(vedtakPeriode -> new EnsligeForsorgereVedtakPeriode(vedtakId, vedtakPeriode.getFom(), vedtakPeriode.getTom(), hentPeriodetype(vedtakPeriode.getPeriodetype().toString()), hentVettakAktivitetstype(vedtakPeriode.getAktivitetstype().toString()))).toList();
        lagreEnsligeForsorgereVedtakPerioder(vedtakId, ensligeForsorgereVedtakPerioder);

        List<EnsligeForsorgereBarn> ef_barn = vedtakOvergangsstønadArbeidsoppfølging.getBarn().stream().map(ef_barn_dto -> new EnsligeForsorgereBarn(ef_barn_dto.getFødselsnummer(), ef_barn_dto.getTermindato())).collect(Collectors.toList());
        lagreDataForEnsligeForsorgereBarn(vedtakId, ef_barn);
    }

    private void lagreEnsligeForsorgereVedtakPerioder(long vedtakId, List<EnsligeForsorgereVedtakPeriode> vedtakPerioder) {
        if (!vedtakPerioder.isEmpty()) {
            vedtakPerioder.forEach(period -> lagreEnsligeForsorgereVedtakPeriod(vedtakId, period.periode_fra(), period.periode_til(), period.periodetype(), period.aktivitetstype()));
        }
    }

    private void lagreEnsligeForsorgereVedtakPeriod(long vedtakid, LocalDate period_fom, LocalDate period_tom, Integer periodeType, Integer aktivitetsType) {
        String sql = """
                INSERT INTO enslige_forsorgere_periode(VEDTAKID, FRA_DATO, TIL_DATO, PERIODETYPE, AKTIVITETSTYPE)
                VALUES (?, ?, ?, ?, ?)
                """;
        db.update(sql, vedtakid, period_fom, period_tom, periodeType, aktivitetsType);
    }

    private void lagreDataForEnsligeForsorgereBarn(long vedtakId, List<EnsligeForsorgereBarn> ef_barn) {
        if (!ef_barn.isEmpty()) {
            ef_barn.forEach(ef_barnet ->
                    lagreDataForEnsligeForsorgereBarnIDB(vedtakId, ef_barnet.fnr(), ef_barnet.terminDato())
            );
        }
    }

    private void lagreDataForEnsligeForsorgereBarnIDB(long vedtakId, String fnr, LocalDate terminDato) {
        String sql = """
                INSERT INTO enslige_forsorgere_barn(VEDTAKID, FNR, TERMINDATO)
                VALUES (?, ?, ?)
                """;
        db.update(sql, vedtakId, fnr, terminDato);
    }

    private Integer hentStonadstype(String stonadTypeConst) {
        return tryCacheFirst(stonadstypeCache, stonadTypeConst,
                () -> this.hentStonadstypeFraDB(stonadTypeConst));
    }

    private Integer hentStonadstypeFraDB(String stonadTypeConst) {
        String sql = "SELECT ID FROM EF_STONAD_TYPE WHERE STONAD_TYPE = :stonadType::varchar";
        Optional<Integer> stonadTypeIdOptional = Optional.of(namedDb.queryForObject(sql, new MapSqlParameterSource("stonadType", stonadTypeConst), Integer.class));

        return stonadTypeIdOptional.orElse(lagreStonadstype(stonadTypeConst));
    }

    public Integer lagreStonadstype(String stonadTypeConst) {
        try {
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            db.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO ef_stonad_type(stonad_type) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, stonadTypeConst);
                return preparedStatement;
            }, generatedKeyHolder);
            return generatedKeyHolder.getKey().intValue();
        } catch (Exception e) {
            throw new RuntimeException("Kan ikke lagre stonadstype " + e);
        }
    }

    private Integer hentVedtakresultat(String vedtakResultatConst) {
        return tryCacheFirst(vedtakresultatCache, vedtakResultatConst, () -> this.hentVedtakresultatFraDB(vedtakResultatConst));
    }

    private Integer hentVedtakresultatFraDB(String vedtakResultatConst) {
        String sql = "SELECT ID FROM ef_vedtaksresultat_type WHERE vedtaksresultat_type = :vedtakResultatConst::varchar";
        Optional<Integer> vedtakResultatTypeIdOptional = Optional.of(namedDb.queryForObject(sql, new MapSqlParameterSource("vedtaksresultat_type", vedtakResultatConst), Integer.class));

        return vedtakResultatTypeIdOptional.orElse(lagreVedtakresultat(vedtakResultatConst));
    }

    public Integer lagreVedtakresultat(String vedtakResultatTypeConst) {
        try {
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            db.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO ef_vedtaksresultat_type(vedtaksresultat_type) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, vedtakResultatTypeConst);
                return preparedStatement;
            }, generatedKeyHolder);
            return generatedKeyHolder.getKey().intValue();
        } catch (Exception e) {
            throw new RuntimeException("Kan ikke lagre vedtakresultat " + e);
        }
    }

    private Integer hentPeriodetype(String periodeTypeConst) {
        return tryCacheFirst(periodeTypeCache, periodeTypeConst, () -> this.hentPeriodetypeFraDB(periodeTypeConst));
    }

    private Integer hentPeriodetypeFraDB(String periodeTypeConst) {
        String sql = "SELECT ID FROM ef_vedtaksperiode_type WHERE periode_type = :periodeTypeConst::varchar";
        Optional<Integer> vedtakPeriodetypeIdOptional = Optional.of(namedDb.queryForObject(sql, new MapSqlParameterSource("periode_type", periodeTypeConst), Integer.class));

        return vedtakPeriodetypeIdOptional.orElse(lagreVedtakperiodeType(periodeTypeConst));
    }

    public Integer lagreVedtakperiodeType(String periodeTypeConst) {
        try {
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            db.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO ef_vedtaksperiode_type(periode_type) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, periodeTypeConst);
                return preparedStatement;
            }, generatedKeyHolder);
            return generatedKeyHolder.getKey().intValue();
        } catch (Exception e) {
            throw new RuntimeException("Kan ikke lagre vedtak periodetype " + e);
        }
    }

    private Integer hentVettakAktivitetstype(String aktivitetsType) {
        return tryCacheFirst(vettakAktivitetstypeCache, aktivitetsType, () -> this.hentVettakAktivitetstypeFraDB(aktivitetsType));
    }

    private Integer hentVettakAktivitetstypeFraDB(String aktivitetsType) {
        String sql = "SELECT ID FROM ef_aktivitet_type WHERE aktivitet_type = :aktivitetsType::varchar";
        Optional<Integer> vedtakAktivitetstypeIdOptional = Optional.of(namedDb.queryForObject(sql, new MapSqlParameterSource("aktivitet_type", aktivitetsType), Integer.class));

        return vedtakAktivitetstypeIdOptional.orElse(lagreVedtakAktivitetstype(aktivitetsType));
    }

    public Integer lagreVedtakAktivitetstype(String aktivitetsType) {
        try {
            GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
            db.update(conn -> {
                PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO ef_aktivitet_type(aktivitet_type) VALUES (?) RETURNING id", Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, aktivitetsType);
                return preparedStatement;
            }, generatedKeyHolder);
            return generatedKeyHolder.getKey().intValue();
        } catch (Exception e) {
            throw new RuntimeException("Kan ikke lagre vedtak aktivitetstype " + e);
        }
    }


    public Optional<EnsligeForsorgerOvergangsstønadTiltak> hentOvergangsstønadForEnsligeForsorger(String personIdent) {
        String sql = """
                 SELECT ef.vedtakId, efp.fra_dato, efp.til_dato,
                       vperiode_type.PERIODE_TYPE as vedtaksPeriodeType,
                       EAT.AKTIVITET_TYPE as aktivitetsType
                FROM enslige_forsorgere ef,
                     enslige_forsorgere_periode efp
                LEFT JOIN EF_VEDTAKSPERIODE_TYPE vperiode_type on efp.PERIODETYPE = vperiode_type.ID
                LEFT JOIN EF_AKTIVITET_TYPE EAT on efp.AKTIVITETSTYPE = EAT.ID
                LEFT JOIN EF_VEDTAKSRESULTAT_TYPE EVT on ef.VEDTAKSRESULTAT = EVT.ID
                LEFT JOIN EF_STONAD_TYPE EST on EST.ID = ef.STØNADSTYPE
                WHERE ef.vedtakId = efp.vedtakId
                  AND est.STONAD_TYPE = 'OVERGANGSSTØNAD'
                  AND EVT.VEDTAKSRESULTAT_TYPE = 'INNVILGET'
                  AND ef.personIdent = ? LIMIT 1;
                 """;
        return dbReadOnly.queryForList(sql, personIdent)
                .stream().map(this::mapTilTiltak)
                .findFirst();
    }

    public Optional<LocalDate> hentYngsteBarn(Long vedtakId) {
        String sql = """
                     SELECT fnr, termindato FROM enslige_forsorgere_barn WHERE vedtakid = ?            
                """;
        return dbReadOnly.queryForList(sql, vedtakId).
                stream().map(this::mapTilBarn)
                .map(barn -> hentBarnetsFødselsdato(vedtakId, barn.fnr(), barn.terminDato()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(LocalDate::compareTo);
    }

    private Optional<LocalDate> hentBarnetsFødselsdato(Long vedtakId, String fnr, LocalDate terminDato) {
        if (fnr != null) {
            try {
                return Optional.of(fnrToDate(fnr));
            } catch (ParseException e) {
                secureLog.warn("Kan ikke parse fnr for ef barn for vedtakId: " + vedtakId);

            }
        } else if (terminDato != null) {
            return Optional.of(terminDato);
        }
        return Optional.empty();

    }


    @SneakyThrows
    private EnsligeForsorgerOvergangsstønadTiltak mapTilTiltak(Map<String, Object> rs) {
        return new EnsligeForsorgerOvergangsstønadTiltak((Long) rs.get("vedtakId"),
                (String) rs.get("vedtaksPeriodeType"),
                (String) rs.get("aktivitetsType"),
                toLocalDateOrNull(String.valueOf(rs.get("fra_dato"))),
                toLocalDateOrNull(String.valueOf(rs.get("til_dato"))));
    }

    @SneakyThrows
    private EnsligeForsorgereBarn mapTilBarn(Map<String, Object> rs) {
        return new EnsligeForsorgereBarn((String) rs.get("fnr"), toLocalDateOrNull((String) rs.get("termindato")));
    }


}
