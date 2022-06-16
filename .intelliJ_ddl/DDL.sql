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

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: bruker_identer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_identer (
    person character varying(25) NOT NULL,
    ident character varying(30) NOT NULL,
    historisk boolean NOT NULL,
    gruppe character varying(30) NOT NULL
);


--
-- Name: aktive_identer; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.aktive_identer AS
 SELECT bi_a.ident AS aktorid,
    bi_f.ident AS fnr
   FROM (public.bruker_identer bi_a
     JOIN public.bruker_identer bi_f ON (((bi_a.person)::text = (bi_f.person)::text)))
  WHERE (((bi_a.gruppe)::text = 'AKTORID'::text) AND (NOT bi_a.historisk) AND ((bi_f.gruppe)::text = 'FOLKEREGISTERIDENT'::text) AND (NOT bi_f.historisk));


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
-- Name: bruker_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_data (
    freg_ident character varying(30) NOT NULL,
    fornavn character varying(90),
    mellomnavn character varying(90),
    etternavn character varying(90),
    kjoenn character varying(5),
    er_doed boolean,
    foedselsdato date,
    foedeland character varying(10),
    innflyttingtilnorgefraland character varying(10),
    angittflyttedato date,
    talespraaktolk character varying(20),
    tegnspraaktolk character varying(20),
    tolkbehovsistoppdatert date
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
-- Name: bruker_statsborgerskap; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bruker_statsborgerskap (
    freg_ident character varying(30) NOT NULL,
    statsborgerskap character varying(20),
    gyldig_fra date,
    gyldig_til date
);


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
-- Name: dialog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dialog (
    aktoerid character varying(20) NOT NULL,
    venter_pa_bruker timestamp without time zone,
    venter_pa_nav timestamp without time zone
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
-- Name: nom_skjerming; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nom_skjerming (
    fodselsnr character varying(33) NOT NULL,
    er_skjermet boolean DEFAULT false,
    skjermet_fra timestamp without time zone,
    skjermet_til timestamp without time zone
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
-- Name: oppfolgingsbruker_arena_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppfolgingsbruker_arena_v2 (
    fodselsnr character varying(33) NOT NULL,
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
    sperret_ansatt boolean DEFAULT false,
    er_doed boolean DEFAULT false,
    endret_dato timestamp without time zone
);


--
-- Name: pdl_person_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pdl_person_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


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
-- Name: utkast_14a_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.utkast_14a_status (
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
-- Name: bruker_data bruker_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_data
    ADD CONSTRAINT bruker_data_pkey PRIMARY KEY (freg_ident);


--
-- Name: bruker_identer bruker_identer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_identer
    ADD CONSTRAINT bruker_identer_pkey PRIMARY KEY (ident);


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
-- Name: bruker_statsborgerskap bruker_statsborgerskap_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_statsborgerskap
    ADD CONSTRAINT bruker_statsborgerskap_pkey PRIMARY KEY (freg_ident);


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
-- Name: nom_skjerming nom_skjerming_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nom_skjerming
    ADD CONSTRAINT nom_skjerming_pkey PRIMARY KEY (fodselsnr);


--
-- Name: oppfolging_data oppfolging_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolging_data
    ADD CONSTRAINT oppfolging_data_pkey PRIMARY KEY (aktoerid);


--
-- Name: oppfolgingsbruker_arena_v2 oppfolgingsbruker_arena_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppfolgingsbruker_arena_v2
    ADD CONSTRAINT oppfolgingsbruker_arena_v2_pkey PRIMARY KEY (fodselsnr);


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
-- Name: utkast_14a_status vedtakstatus_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utkast_14a_status
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
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: nav_kontor_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX nav_kontor_idx ON public.oppfolgingsbruker_arena_v2 USING btree (nav_kontor);


--
-- Name: oppfolging_data_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX oppfolging_data_idx ON public.oppfolging_data USING btree (veilederid);


--
-- Name: personid_brukertiltak_v2_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX personid_brukertiltak_v2_idx ON public.brukertiltak USING btree (aktoerid);


--
-- Name: bruker_statsborgerskap fk_freg_ident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bruker_statsborgerskap
    ADD CONSTRAINT fk_freg_ident FOREIGN KEY (freg_ident) REFERENCES public.bruker_data(freg_ident) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

