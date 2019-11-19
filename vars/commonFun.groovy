import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.*
import hudson.util.Secret
import jenkins.model.Jenkins
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*


String artifactoryServerUrl() {
    return 'http://artifactory:8081/artifactory'
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


def addCredential(artUsername, artPassword, secretId, description) {

   Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(
   CredentialsScope.GLOBAL, // Scope
   secretId, // id
   description, // description
   artUsername, // username
   artPassword // password
   )
   SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)

}

def addSecretText(token, sonarDescription, sonarSecretId) {
	Credentials secretText = (Credentials) new StringCredentialsImpl(
	CredentialsScope.GLOBAL,
	sonarSecretId, // id
	sonarDescription, // description
	Secret.fromString(token) // secret
	)

	SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), secretText)

}

def deleteCredential(secretId){

	def credentialsStore = jenkins.model.Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
	allCreds = credentialsStore.getCredentials(Domain.global())

	allCreds.each{
  		if (it.id == secretId){
    		println ("Found ID")
    		credentialsStore.removeCredentials(Domain.global(), it)
  		}
	}
}
