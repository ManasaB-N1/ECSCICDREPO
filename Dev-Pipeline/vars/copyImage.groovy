def call(app_image_ecr_uat, uatimagetag, app_image_ecr_prod, prod_ecr_image_tag) {
	script {        
		echo "$app_image_ecr_uat, $uatimagetag, $app_image_ecr_prod, $prod_ecr_image_tag"
		sh "sudo docker image pull $app_image_ecr_uat:$uatimagetag"
        sh "sudo docker tag $app_image_ecr_uat:$uatimagetag $app_image_ecr_prod:$prod_ecr_image_tag"        
        sh "sudo docker push $app_image_ecr_prod:$prod_ecr_image_tag"
	}
}