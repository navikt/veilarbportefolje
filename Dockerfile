FROM docker.adeo.no:5000/bekkci/maven-builder
ADD / /source
RUN build

FROM docker.adeo.no:5000/bekkci/skya-deployer as deployer
FROM docker.adeo.no:5000/bekkci/backend-smoketest as smoketest

# TODO oppsett for nais