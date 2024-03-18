//The image built from buildImage is pushed to ECR with proper tag
def call(region, ecr_repo, service1_local_image, service1_image_tag) {
	script {        
		echo "$region, $ecr_repo, $service1_local_image, $service1_image_tag"
        sh "sudo docker tag $service1_local_image:latest $ecr_repo:$service1_image_tag"        
        sh "sudo docker push $ecr_repo:$service1_image_tag"
	}
}
