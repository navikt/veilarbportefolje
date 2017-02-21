@Library('common') import common
def commonLib = new common()

def notifyFailed(reason, error) {
    def commons = new common()
    changelog = commons.getChangeString()
    chatmsg = "**[veilarbportefolje ${version}](https://app-t4.adeo.no/veilarbportefolje/) ${reason} **\n\n${changelog}"
    mattermostSend channel: ${appName}, color: '#FF0000', message: chatmsg
    currentBuild.result = 'FAILED'
    step([$class: 'StashNotifier'])
    throw error
}

node {
    commonLib.setupTools("Maven 3.3.3", "java8")

    stage('Checkout') {
        checkout([$class: 'GitSCM', branches: [[name: "*/master"]], doGenerateSubmoduleConfigurations: false, extensions: [], gitTool: 'Default', submoduleCfg: [], userRemoteConfigs: [[url: 'ssh://git@stash.devillo.no:7999/fo/veilarbportefolje.git']]])
        step([$class: 'StashNotifier'])

        pom = readMavenPom file: 'pom.xml'
        if (useSnapshot == 'true') {
            version = pom.version.replace("-SNAPSHOT", ".${currentBuild.number}-SNAPSHOT")
        } else {
            version = pom.version.replace("-SNAPSHOT", ".${currentBuild.number}")
        }
        sh "mvn versions:set -DnewVersion=${version}"
    }

    stage('Build (java)') {
        try {
            sh "mvn clean install -DskipTests"
        } catch(Exception e) {
            notifyFailed("Bygg feilet", e)
        }
    }

    stage('Run tests (java)') {
        try {
            sh "mvn test"
        } catch(Exception e) {
            step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
            notifyFailed("Java-tester feilet", e)
        }
    }

    stage('Deploy nexus') {
        try {
            sh "mvn -B deploy -DskipTests"
            currentBuild.description = "Version: ${version}"
            sh "mvn versions:set -DnewVersion=${pom.version}"
            if (useSnapshot != 'true') {
                sh "git tag -a ${version} -m ${version} HEAD && git push --tags"
            }
        } catch(Exception e) {
            notifyFailed("Deploy av artifakt til nexus feilet", e)
        }
    }
}

stage("Deploy app") {
    callback = "${env.BUILD_URL}input/Deploy/"
    node {
        def author = sh(returnStdout: true, script: 'git --no-pager show -s --format="%an <%ae>" HEAD').trim()
        def deploy = commonLib.deployApp('veilarbportefolje', version, "${miljo}", callback, author).key

        try {
            timeout(time: 15, unit: 'MINUTES') {
                input id: 'deploy', message: "deployer ${deploy}, deploy OK?"
            }
        } catch(Exception e) {
            msg = "Deploy feilet [" + deploy + "](https://jira.adeo.no/browse/" + deploy + ")"
            notifyFailed(msg, e)
        }
    }
}

chatmsg = "**[veilarbportefolje ${version}](https://app-t4.adeo.no/veilarbportefolje/) Bygg og deploy OK**\n\n${commonLib.getChangeString()}"
mattermostSend channel: 'Natthauk-ops', color: '#00FF00', message: chatmsg
