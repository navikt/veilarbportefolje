--
-- PostgreSQL database dump
--

-- Dumped from database version 11.5 (Debian 11.5-3.pgdg90+1)
-- Dumped by pg_dump version 11.5 (Debian 11.5-3.pgdg90+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: kafka_message_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.kafka_message_type AS ENUM (
    'PRODUCED',
    'CONSUMED'
);


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: aktiviteter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktiviteter (
    aktivitetid character varying(20) NOT NULL,
    aktoerid character varying(20) NOT NULL,
    aktivitettype character varying(255),
    avtalt boolean DEFAULT true,
    fradato timestamp without time zone,
    tildato timestamp without time zone,
    status character varying(255),
    version bigint
);


--
-- Name: arbeidsliste; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsliste (
    aktoerid character varying(20) NOT NULL,
    sist_endret_av_veilederident character varying(20),
    kommentar character varying(1000),
    frist timestamp without time zone,
    endringstidspunkt timestamp without time zone,
    overskrift character varying(500),
    kategori character varying(10),
    nav_kontor_for_arbeidsliste character varying(24)
);


--
-- Name: bruker_cv; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_cv (
    aktoerid character varying(20) NOT NULL,
    har_delt_cv boolean DEFAULT false NOT NULL,
    cv_eksisterer boolean DEFAULT false NOT NULL,
    siste_melding_mottatt timestamp without time zone
);


--
-- Name: bruker_profilering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_profilering (
    aktoerid character varying(20) NOT NULL,
    profilering_resultat character varying(40),
    profilering_tidspunkt timestamp without time zone
);


--
-- Name: bruker_registrering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_registrering (
    aktoerid character varying(20) NOT NULL,
    brukers_situasjon character varying(40),
    registrering_opprettet timestamp without time zone,
    utdanning character varying(35),
    utdanning_bestatt character varying(15),
    utdanning_godkjent character varying(15)
);


--
-- Name: dialog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dialog (
    aktoerid character varying(20) NOT NULL,
    venter_pa_bruker timestamp without time zone,
    venter_pa_nav timestamp without time zone
);


--
-- Name: oppfolging_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppfolging_data (
    aktoerid character varying(20) NOT NULL,
    veilederid character varying(20),
    oppfolging boolean DEFAULT false NOT NULL,
    ny_for_veileder boolean DEFAULT false NOT NULL,
    manuell boolean DEFAULT false NOT NULL,
    startdato timestamp without time zone
);


--
-- Name: vedtakstatus; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtakstatus (
    aktoerid character varying(20) NOT NULL,
    vedtakid character varying(20) NOT NULL,
    vedtakstatus character varying(40),
    innsatsgruppe character varying(40),
    hovedmal character varying(30),
    ansvarlig_veilderident character varying(20),
    ansvarlig_veildernavn character varying(60),
    endret_tidspunkt timestamp without time zone
);


--
-- Name: ytelse_status_for_bruker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ytelse_status_for_bruker (
    aktoerid character varying(20) NOT NULL,
    utlopsdato timestamp without time zone,
    dagputlopuke integer,
    permutlopuke integer,
    aapmaxtiduke integer,
    aapunntakdagerigjen integer,
    ytelse character varying(40)
);


--
-- Name: aktorid_indeksert_data; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.aktorid_indeksert_data AS
 SELECT od.aktoerid,
    od.oppfolging,
    od.startdato,
    od.ny_for_veileder,
    od.veilederid,
    od.manuell,
    d.venter_pa_bruker,
    d.venter_pa_nav,
    v.vedtakstatus,
    bp.profilering_resultat,
    cv.har_delt_cv,
    cv.cv_eksisterer,
    br.brukers_situasjon,
    br.utdanning,
    br.utdanning_bestatt,
    br.utdanning_godkjent,
    yb.ytelse,
    yb.aapmaxtiduke,
    yb.aapunntakdagerigjen,
    yb.dagputlopuke,
    yb.permutlopuke,
    yb.utlopsdato AS ytelse_utlopsdato,
    v.ansvarlig_veildernavn AS vedtakstatus_ansvarlig_veildernavn,
    v.endret_tidspunkt AS vedtakstatus_endret_tidspunkt,
    arb.sist_endret_av_veilederident AS arb_sist_endret_av_veilederident,
    arb.endringstidspunkt AS arb_endringstidspunkt,
    arb.overskrift AS arb_overskrift,
    arb.frist AS arb_frist,
    arb.kategori AS arb_kategori
   FROM (((((((public.oppfolging_data od
     LEFT JOIN public.dialog d ON (((d.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.vedtakstatus v ON (((v.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.arbeidsliste arb ON (((arb.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.bruker_profilering bp ON (((bp.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.bruker_cv cv ON (((cv.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.bruker_registrering br ON (((br.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.ytelse_status_for_bruker yb ON (((yb.aktoerid)::text = (od.aktoerid)::text)));


--
-- Name: oppfolgingsbruker_arena; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppfolgingsbruker_arena (
    aktoerid character varying(20) NOT NULL,
    fodselsnr character varying(33),
    formidlingsgruppekode character varying(15),
    iserv_fra_dato timestamp without time zone,
    etternavn character varying(90),
    fornavn character varying(90),
    nav_kontor character varying(24),
    kvalifiseringsgruppekode character varying(15),
    rettighetsgruppekode character varying(15),
    hovedmaalkode character varying(30),
    sikkerhetstiltak_type_kode character varying(12),
    diskresjonskode character varying(6),
    har_oppfolgingssak boolean DEFAULT false NOT NULL,
    sperret_ansatt boolean DEFAULT false NOT NULL,
    er_doed boolean DEFAULT false NOT NULL,
    doed_fra_dato timestamp without time zone,
    endret_dato timestamp without time zone,
    fodsels_dato date,
    kjonn character varying(1)
);


--
-- Name: bruker; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.bruker AS
 SELECT od.aktoerid,
    od.oppfolging,
    od.startdato,
    od.ny_for_veileder,
    od.veilederid,
    od.manuell,
    oba.kjonn,
    oba.fodsels_dato,
    oba.fodselsnr,
    oba.fornavn,
    oba.etternavn,
    oba.nav_kontor,
    oba.iserv_fra_dato,
    oba.formidlingsgruppekode,
    oba.kvalifiseringsgruppekode,
    oba.rettighetsgruppekode,
    oba.hovedmaalkode,
    oba.sikkerhetstiltak_type_kode,
    oba.diskresjonskode,
    oba.har_oppfolgingssak,
    oba.sperret_ansatt,
    oba.er_doed,
    d.venter_pa_bruker,
    d.venter_pa_nav,
    v.vedtakstatus,
    bp.profilering_resultat,
    cv.har_delt_cv,
    cv.cv_eksisterer,
    br.brukers_situasjon,
    br.utdanning,
    br.utdanning_bestatt,
    br.utdanning_godkjent,
    v.ansvarlig_veildernavn AS vedtakstatus_ansvarlig_veildernavn,
    v.endret_tidspunkt AS vedtakstatus_endret_tidspunkt,
    arb.sist_endret_av_veilederident AS arb_sist_endret_av_veilederident,
    arb.endringstidspunkt AS arb_endringstidspunkt,
    arb.overskrift AS arb_overskrift,
    arb.kommentar AS arb_kommentar,
    arb.frist AS arb_frist,
    arb.kategori AS arb_kategori
   FROM (((((((public.oppfolging_data od
     LEFT JOIN public.oppfolgingsbruker_arena oba ON (((oba.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.dialog d ON (((d.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.vedtakstatus v ON (((v.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.arbeidsliste arb ON (((arb.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.bruker_profilering bp ON (((bp.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.bruker_cv cv ON (((cv.aktoerid)::text = (od.aktoerid)::text)))
     LEFT JOIN public.bruker_registrering br ON (((br.aktoerid)::text = (od.aktoerid)::text)));


--
-- Name: brukertiltak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brukertiltak (
    aktivitetid character varying(25) NOT NULL,
    aktoerid character varying(20),
    personid character varying(20),
    tiltakskode character varying(10),
    tildato timestamp without time zone,
    fradato timestamp without time zone
);


--
-- Name: feilet_kafka_melding; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feilet_kafka_melding (
    id bigint NOT NULL,
    topic character varying(100) NOT NULL,
    key character varying(40) NOT NULL,
    payload json NOT NULL,
    message_type public.kafka_message_type NOT NULL,
    message_offset bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: gruppe_aktiviter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gruppe_aktiviter (
    moteplan_id character varying(25) NOT NULL,
    veiledningdeltaker_id character varying(25) NOT NULL,
    aktoerid character varying(20),
    moteplan_startdato timestamp without time zone,
    moteplan_sluttdato timestamp without time zone,
    hendelse_id bigint,
    aktiv boolean DEFAULT false
);


--
-- Name: kafka_consumer_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kafka_consumer_record (
    id bigint NOT NULL,
    topic character varying(100) NOT NULL,
    partition integer NOT NULL,
    record_offset bigint NOT NULL,
    retries integer DEFAULT 0 NOT NULL,
    last_retry timestamp(6) without time zone,
    key bytea,
    value bytea,
    headers_json text,
    record_timestamp bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: kafka_consumer_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.kafka_consumer_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lest_arena_hendelse_aktivitet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lest_arena_hendelse_aktivitet (
    aktivitetid character varying(25) NOT NULL,
    hendelse_id bigint
);


--
-- Name: lest_arena_hendelse_ytelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lest_arena_hendelse_ytelse (
    vedtakid character varying(25) NOT NULL,
    hendelse_id bigint
);


--
-- Name: optimaliser_bruker; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.optimaliser_bruker AS
 SELECT od.aktoerid,
    od.oppfolging,
    od.startdato,
    od.ny_for_veileder,
    od.veilederid,
    oba.fodselsnr,
    oba.fornavn,
    oba.etternavn,
    oba.nav_kontor,
    oba.diskresjonskode
   FROM (public.oppfolging_data od
     LEFT JOIN public.oppfolgingsbruker_arena oba ON (((oba.aktoerid)::text = (od.aktoerid)::text)));


--
-- Name: scheduled_tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheduled_tasks (
    task_name text NOT NULL,
    task_instance text NOT NULL,
    task_data bytea,
    execution_time timestamp with time zone NOT NULL,
    picked boolean NOT NULL,
    picked_by text,
    last_success timestamp with time zone,
    last_failure timestamp with time zone,
    consecutive_failures integer,
    last_heartbeat timestamp with time zone,
    version bigint NOT NULL
);


--
-- Name: shedlock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.shedlock (
    name character varying(256) NOT NULL,
    lock_until timestamp(3) without time zone,
    locked_at timestamp(3) without time zone,
    locked_by character varying(255)
);


--
-- Name: siste_endring; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.siste_endring (
    aktoerid character varying(20) NOT NULL,
    aktivitetid character varying(20),
    siste_endring_kategori character varying(45) NOT NULL,
    siste_endring_tidspunkt timestamp without time zone,
    er_sett boolean DEFAULT false
);


--
-- Name: tiltakkodeverket; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tiltakkodeverket (
    kode character varying(10) NOT NULL,
    verdi character varying(80)
);


--
-- Name: ytelsesvedtak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ytelsesvedtak (
    vedtaksid character varying(20) NOT NULL,
    aktorid character varying(20) NOT NULL,
    personid character varying(20),
    ytelsestype character varying(15),
    saksid character varying(30),
    sakstypekode character varying(10),
    rettighetstypekode character varying(10),
    startdato timestamp without time zone,
    utlopsdato timestamp without time zone,
    antallukerigjen integer,
    antallpermitteringsuker integer,
    antallukerigjenunntak integer
);


--
-- Name: aktiviteter aktiviteter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktiviteter
    ADD CONSTRAINT aktiviteter_pkey PRIMARY KEY (aktivitetid);


--
-- Name: arbeidsliste arbeidsliste_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsliste
    ADD CONSTRAINT arbeidsliste_pkey PRIMARY KEY (aktoerid);


--
-- Name: bruker_cv bruker_cv_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_cv
    ADD CONSTRAINT bruker_cv_pkey PRIMARY KEY (aktoerid);


--
-- Name: bruker_profilering bruker_profilering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_profilering
    ADD CONSTRAINT bruker_profilering_pkey PRIMARY KEY (aktoerid);


--
-- Name: bruker_registrering bruker_registrering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_registrering
    ADD CONSTRAINT bruker_registrering_pkey PRIMARY KEY (aktoerid);


--
-- Name: brukertiltak brukertiltak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brukertiltak
    ADD CONSTRAINT brukertiltak_pkey PRIMARY KEY (aktivitetid);


--
-- Name: dialog dialog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dialog
    ADD CONSTRAINT dialog_pkey PRIMARY KEY (aktoerid);


--
-- Name: feilet_kafka_melding feilet_kafka_melding_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feilet_kafka_melding
    ADD CONSTRAINT feilet_kafka_melding_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: gruppe_aktiviter gruppe_aktiviter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gruppe_aktiviter
    ADD CONSTRAINT gruppe_aktiviter_pkey PRIMARY KEY (moteplan_id, veiledningdeltaker_id);


--
-- Name: kafka_consumer_record kafka_consumer_record_topic_partition_record_offset_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kafka_consumer_record
    ADD CONSTRAINT kafka_consumer_record_topic_partition_record_offset_key UNIQUE (topic, partition, record_offset);


--
-- Name: lest_arena_hendelse_aktivitet lest_arena_hendelse_aktivitet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lest_arena_hendelse_aktivitet
    ADD CONSTRAINT lest_arena_hendelse_aktivitet_pkey PRIMARY KEY (aktivitetid);


--
-- Name: lest_arena_hendelse_ytelse lest_arena_hendelse_ytelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lest_arena_hendelse_ytelse
    ADD CONSTRAINT lest_arena_hendelse_ytelse_pkey PRIMARY KEY (vedtakid);


--
-- Name: oppfolging_data oppfolging_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolging_data
    ADD CONSTRAINT oppfolging_data_pkey PRIMARY KEY (aktoerid);


--
-- Name: oppfolgingsbruker_arena oppfolgingsbruker_arena_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsbruker_arena
    ADD CONSTRAINT oppfolgingsbruker_arena_pkey PRIMARY KEY (aktoerid);


--
-- Name: scheduled_tasks scheduled_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_tasks
    ADD CONSTRAINT scheduled_tasks_pkey PRIMARY KEY (task_name, task_instance);


--
-- Name: shedlock shedlock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.shedlock
    ADD CONSTRAINT shedlock_pkey PRIMARY KEY (name);


--
-- Name: siste_endring siste_endring_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.siste_endring
    ADD CONSTRAINT siste_endring_pkey PRIMARY KEY (aktoerid, siste_endring_kategori);


--
-- Name: tiltakkodeverket tiltakkodeverket_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tiltakkodeverket
    ADD CONSTRAINT tiltakkodeverket_pkey PRIMARY KEY (kode);


--
-- Name: vedtakstatus vedtakstatus_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtakstatus
    ADD CONSTRAINT vedtakstatus_pkey PRIMARY KEY (aktoerid);


--
-- Name: ytelse_status_for_bruker ytelse_status_for_bruker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ytelse_status_for_bruker
    ADD CONSTRAINT ytelse_status_for_bruker_pkey PRIMARY KEY (aktoerid);


--
-- Name: ytelsesvedtak ytelsesvedtak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ytelsesvedtak
    ADD CONSTRAINT ytelsesvedtak_pkey PRIMARY KEY (vedtaksid);


--
-- Name: aktoer_aktivitet_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aktoer_aktivitet_indx ON public.aktiviteter USING btree (aktoerid);


--
-- Name: aktoer_tiltak_indx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aktoer_tiltak_indx ON public.brukertiltak USING btree (aktoerid);


--
-- Name: aktoerid_gruppe_aktiviter_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aktoerid_gruppe_aktiviter_idx ON public.gruppe_aktiviter USING btree (aktoerid);


--
-- Name: aktorid_ytelser_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aktorid_ytelser_idx ON public.ytelsesvedtak USING btree (aktorid);


--
-- Name: enhet_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX enhet_idx ON public.oppfolgingsbruker_arena USING btree (nav_kontor);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: oppfolging_data_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX oppfolging_data_idx ON public.oppfolging_data USING btree (veilederid);


--
-- Name: personid_brukertiltak_v2_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX personid_brukertiltak_v2_idx ON public.brukertiltak USING btree (aktoerid);


--
-- PostgreSQL database dump complete
--

