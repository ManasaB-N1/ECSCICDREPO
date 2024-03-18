//We pass parameters to createJson.sh here
def call(family, image1, filename, region, jobname, image1tag) {
	script {
		sh "cd .."
		sh "cd /var/lib/jenkins/workspace/$jobname"
		sh "pwd"
		sh "chmod 555 /var/lib/jenkins/workspace/$jobname/libLoader/vars/createJson.sh"
		sh "sudo sh /var/lib/jenkins/workspace/$jobname/libLoader/vars/createJson.sh \"$family\" \"$image1\" \"$filename\" \"$region\" \"$image1tag\""
	}
}
