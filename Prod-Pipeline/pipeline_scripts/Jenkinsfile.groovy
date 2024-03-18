@Library("BuildScripts") _

pipeline{
  agent any
  environment {
    AWS_SECRET=credentials('6e3a1df3-f115-4a49-929c-b0200b33e324')
    REGION=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.REGION').trim()
    FAMILY=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.FAMILY').trim()
    APP_IMAGE_ECR_UAT=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.APP_IMAGE_ECR_UAT').trim()
    APP_IMAGE_ECR_PROD=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.APP_IMAGE_ECR_PROD').trim()
    SERVICE=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.SERVICE').trim()
    CLUSTER=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.CLUSTER').trim()
    FROM_EMAIL=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.FROM_EMAIL').trim()
    TO_EMAIL=sh (returnStdout: true, script: 'aws ssm get-parameters --names \$AWS_SECRET --region ap-south-1 --with-decryption | jq -r .Parameters[0].Value | jq -r .Secrets.TO_EMAIL').trim() 
    DESIRED_COUNT="1" 
    FILENAME = "${env.FAMILY}-${env.BUILD_NUMBER}.json" // Do not change this
  }
  stages {
    stage("Copy Image") {
      steps {
        script {
          try {
              timeout(time: 60, unit: 'SECONDS') {
                uatImageTag = input message: "UAT ECR Image Tag",
                          parameters: [string(name: 'UAT ECR Image Tag', description: "Please provide the UAT ECR Image Tag")]
                echo "$uatImageTag"
            }
          }
          catch(err) {
              echo "${err}"
              echo "No Tag number was specified"
              echo "${STAGE_NAME} Stage Failed in ${JOB_BASE_NAME} Jenkins Job"
          }
          appTag = incrementalTagging("${env.APP_IMAGE_ECR_PROD}")
          echo "${appTag}"
          copyImage("${env.APP_IMAGE_ECR_UAT}", "$uatImageTag", "${env.APP_IMAGE_ECR_PROD}", "$appTag")         
        }
      }
    }
    stage("Create Task Definition") {
      steps {
        script{
          createJson("${env.FAMILY}", "${env.APP_IMAGE_ECR_PROD}", "${env.FILENAME}", "${env.REGION}", "${JOB_BASE_NAME}","$appTag")
          newTaskArn = registerTaskDefinition("${env.REGION}", "${env.FILENAME}")
          echo "${newTaskArn}"
        }
      }
    }
    stage("Deploy Image") {
      steps {
        script {
          echo "Deploying Latest Docker Image into ECS"
          deployImage("${env.CLUSTER}","${env.SERVICE}", "${env.REGION}", "$newTaskArn", "${env.DESIRED_COUNT}")         
        }
      }
    }  
  }
  post {
    always {
        sh 'docker system prune -a -f'        
        sh 'sudo rm -rf /var/lib/jenkins/caches'
        cleanWs()
    }
    failure{
      slackSend( message: "${JOB_BASE_NAME} Job Failed")
    }
    aborted{
      slackSend( message: "${JOB_BASE_NAME} Job aborted")
    }
    success{
      slackSend( message: "${JOB_BASE_NAME} Job executed Successfully")
    }  
  } 
}
