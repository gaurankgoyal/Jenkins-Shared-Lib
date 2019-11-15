def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    pomFile = config.get("pomFile", "pom.xml")
    commonFun.setJobProperties(10, H/30 * * * *)
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
