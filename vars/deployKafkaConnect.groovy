def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    configFolder = config.get("configFolder")
    
    node("tools01") {
        stage("Checkout") {
            checkout scm
        }
        jsonFiles = findFiles glob: "${configFolder}/*.json"
        changedFiles = sh(script: "git diff --name-only HEAD HEAD~1", returnStdout: true).trim()
        withFolderProperties{
            index = 0
            for (jsonfile in jsonFiles) {
                index = index + 1
                config = readJSON file: jsonfile.path
                connector = config["name"]
                stage("${index}. ${connector}") {
                    if (!changedFiles.contains(jsonfile.path) && (env.Deploy_All == "false")) {
                        println("Skip ${connector} for no change")
                    } else {
                        try {
                            println("Delete ${connector}")
                            sh "curl -s -X DELETE ${env.kafka_connect_url}/${connector}"
                        } catch(error) {
                            println("Cannot delete ${connector}")   
                        }
                        properties = readFile jsonfile.path
                        println("Deploy ${connector}")
                        try {
                            httpRequest acceptType: 'APPLICATION_JSON', consoleLogResponseBody: true, 
                                        contentType: 'APPLICATION_JSON', httpMode: 'POST', 
                                        requestBody: properties.replace('${env}', env.environment), responseHandle: 'NONE', timeout: 30, 
                                        url: "${env.kafka_connect_url}"
                        } catch(error) {
                            slackSend channel: "#flow-alerts", color: 'danger', 
                                        message: ":warning: Kafka Connect Configuration is deployed failed. - ${env.JOB_NAME} (<${env.BUILD_URL}/console|Open>)",
                                        tokenCredentialId: commonLib.getSlackTokenId("Kafka")
                            throw error
                        }
                    }
                }
            }
            slackSend channel: "#flow-alerts", color: 'good', 
                    message: ":inbox_tray: Kafka Connect Configuration is deployed on ${env.environment} successfully!", 
                    tokenCredentialId: commonLib.getSlackTokenId("Kafka")
        }
    }
}


