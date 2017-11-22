FROM docker.adeo.no:5000/bekkci/maven-builder as builder
ADD / /source
RUN build

FROM docker.adeo.no:5000/bekkci/skya-deployer as deployer
COPY --from=builder /source /deploy

FROM docker.adeo.no:5000/bekkci/backend-smoketest as smoketest

# TODO oppsett for nais