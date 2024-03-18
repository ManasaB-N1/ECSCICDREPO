//Using the newly created Task Definition we create a new deployment
def call(cluster, service, region, taskDefinitionArn, desiredCount) {
	script {
		sh "aws ecs update-service --cluster $cluster --service $service --region $region --desired-count 0"
		sh "sleep 300"
		sh "aws ecs update-service --cluster $cluster --service $service --region $region --task-definition $taskDefinitionArn --desired-count $desiredCount"
	}
}
