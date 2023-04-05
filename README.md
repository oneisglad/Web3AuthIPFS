# Web3AuthIPFS
IPFS WEB3AUTH ANDROID
# How to
To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

gradle
maven
sbt
leiningen

Add it in your root build.gradle at the end of repositories:

  '''
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  '''
  
# And  
  
  '''
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.oneisglad:Web3AuthIPFS:Tag'
	}
  '''
