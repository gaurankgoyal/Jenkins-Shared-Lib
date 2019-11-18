def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    def mavenTool = "M3"
    def pomFile = config.get("pomFile", "pom.xml")
    commonFun.setJobProperties(env.NUM_BUILDS_KEPT, "H/10 * * * *")
    def SERVER_URL = commonFun.artifactoryServerUrl()
    def CREDENTIALS = "atrifactory"
    def artifactoryServer = Artifactory.newServer url: SERVER_URL, credentialsId: 'art-secret-id'
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo
    print (SERVER_URL) 
    print (CREDENTIALS)

    stage 'checkout'
    node {
	stage('Get Secret From Vault'){
		withVault(configuration: [timeout: 60, vaultCredentialId: 'vault-token', vaultUrl: 'http://e746f51dee0e:8200'], vaultSecrets: [[path: 'secret/Artifactory', secretValues: [[envVar: 'artUsername', vaultKey: 'username'], [envVar: 'artPassword', vaultKey: 'password']]]])
		{
		print (artUsername)
		print (artPassword)
		commonFun.addCredential(artUsername, artPassword)
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
        	junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
    	}
        stage 'UnitTest'
	stage ('Publish build info') {
        artifactoryServer.publishBuildInfo buildInfo
    }
        sh config.postScript

	commonFun.deleteCredential()	
    }
	
}
