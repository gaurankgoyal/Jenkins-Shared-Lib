def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    def mavenTool = config.mavenTool
    def pomFile = config.get("pomFile", "pom.xml")
    //pomVersion = readMavenPom().getVersion()
    commonFun.setJobProperties(env.NUM_BUILDS_KEPT, config.pollingInterval)
    def SERVER_URL = config.artifactoryServerUrl
    def secretId = 'art-secret-id'
    def sonarSecretId = 'sonar-secret-id'
    def artifactoryServer = Artifactory.newServer url: SERVER_URL, credentialsId: secretId
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo
    print (config.artifactoryServerUrl)
    print (config.pollingInterval)
    print (config.vaultUrl)
    print (config.vaultArtifactoryPath)
    print (config.vaultArtifactoryUsernameKey)
    print (config.vaultArtifcatortyPasswordKey)
    print (config.vaultSonarQubePath)
    print (config.vauktSonarQubeTokenKey)
    stage 'checkout'
    node {
	pomVersion = readMavenPom().getVersion()
	stage('Get Secret From Vault'){
		withVault(configuration: [timeout: 60, vaultCredentialId: 'vault-token', vaultUrl: config.vaultUrl], vaultSecrets: [[path: 'secret/Artifactory', secretValues: [[envVar: 'artUsername', vaultKey: 'username'], [envVar: 'artPassword', vaultKey: 'password']]]])
		{
		def description = 'Jfrog-Credentials'
		commonFun.addCredential(artUsername, artPassword, secretId, description)
		}
		withVault(configuration: [timeout: 60, vaultCredentialId: 'vault-token', vaultUrl: config.vaultUrl], vaultSecrets: [[path: 'secret/SonarQube', secretValues: [[envVar: 'sonartoken', vaultKey: 'token']]]])
		{
		def sonarDescription = 'Sonar-Credentials'
		commonFun.addSecretText(sonartoken, sonarDescription, sonarSecretId)
		}
	}
	stage('Pull Source Code') {
        	checkout scm

	}
	stage ('Artifactory configuration') {
		rtMaven.tool = mavenTool
		rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: artifactoryServer
        	rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: artifactoryServer
        	buildInfo = Artifactory.newBuildInfo()
		//pomVersion = readMavenPom().getVersion()
		//println(pomVersion)
	}
        stage ('Build') {
		rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
        	//junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
    	}
        stage ('UnitTest') {
		junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
	}
	stage ('Run SonarQube')
	{
		withMaven(maven: 'M3') {
			withCredentials([string(credentialsId: sonarSecretId , variable: 'SONAR_LOGIN')]) {
				echo "Running SonarQube Static Analysis for master"
                		sh "mvn clean package sonar:sonar -Dsonar.host.url=http://sonarqube:9000/ -Dsonar.login=${env.SONAR_LOGIN} -Dsonar.projectVersion=${pomVersion} "
				echo "SonarQube Static Analysis was SUCCESSFUL for master"
			}
		}
	}
	stage ('Publish build info') {
        artifactoryServer.publishBuildInfo buildInfo
    }

	commonFun.deleteCredential(secretId)
	commonFun.deleteCredential(sonarSecretId)	
    }
	
}
