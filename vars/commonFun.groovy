String artifactoryServerUrl() {
    return 'http://localhost:8081/artifactory'
}


def setJobProperties(numToKeep, pollSCMSchedule) {
    def jobProperties = []
    jobProperties.add(disableConcurrentBuilds())
    if (numToKeep == null || numToKeep.isEmpty()) {
        numToKeep = '10'
    }
    // Set Build retention
    jobProperties.add([$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: numToKeep]],)
    if (pollSCMSchedule == null || pollSCMSchedule.isEmpty()) {
        pollSCMSchedule = 'H/30 * * * *' // By default, poll SCM every 30 minitues
    }
    // Set Poll SCM schedule
    else {
        jobProperties.add(pipelineTriggers([[$class: 'GitHubPushTrigger'], pollSCM(pollSCMSchedule)]))
    }
    properties(jobProperties)
}
