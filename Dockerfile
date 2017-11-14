FROM docker.adeo.no:5000/bekkci/maven-builder
ADD / /source
RUN build


# TODO oppsett for nais