import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.*
import hudson.util.Secret
import jenkins.model.Jenkins



String artifactoryServerUrl() {
    return 'http://5844c6e375f6:8081/artifactory'
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


def addCredential(artUsername, artPassword) {

   Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(
   CredentialsScope.GLOBAL, // Scope
   "art-cred", // id
   "artifactory-credentials", // description
   artUsername, // username
   artPassword // password
   )
   SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)

}
