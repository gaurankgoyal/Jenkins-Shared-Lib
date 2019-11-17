def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    pomFile = config.get("pomFile", "pom.xml")
    commonFun.setJobProperties(env.NUM_BUILDS_KEPT, "H/10 * * * *")
    SERVER_URL = commonFun.artifactoryServerUrl()
    CREDENTIALS = "atrifactory"
    artifactoryServer = Artifactory.newServer url: SERVER_URL, credentialsId: CREDENTIALS
    print (artifactoryServer)
    rtMaven = Artifactory.newMavenBuild()
    print (rtMaven)
   // rtMaven.tool = mavenTool
    print (SERVER_URL) 
    print (CREDENTIALS)
    stage 'checkout'
    node {
	stage('Pull Source Code') {
        	checkout scm

	}
	pomVersion = readMavenPom().getVersion()
	println(pomVersion)
        stage 'Build'
        docker.image(config.environment).inside {
        	sh config.mainScript
        }
        stage 'UnitTest'
        sh config.postScript
    }
}
