FROM docker.adeo.no:5000/bekkci/maven-builder
# [ ARG STEP_MARKER ] se http://stash.devillo.no/projects/BEKKCI/repos/jenkins-plugin/browse
ARG STEP_MARKER

# brukes av testene
ARG testmiljo
ARG domenebrukernavn
ARG domenepassord

ADD / /source
RUN build


# TODO oppsett for nais