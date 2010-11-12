cd ../common/ && git pull origin master && mvn clean install && mvn eclipse:eclipse
cd ../common-persistence/ && git pull origin master && mvn clean install && mvn eclipse:eclipse
cd ../common-social/ && git pull origin master && mvn clean install && mvn eclipse:eclipse
cd ../common-webapp/ && git pull origin master && mvn clean install && mvn eclipse:eclipse
cd ../atlas-model/ && git pull origin master && mvn clean install && mvn eclipse:eclipse
cd ../atlas-feeds/ && git pull origin master && mvn clean install && mvn eclipse:eclipse
cd ../atlas-persistence/ && git pull origin master && mvn clean install -DskipTests && mvn eclipse:eclipse
cd ../atlas-applications/ && git pull origin master && mvn clean install && mvn eclipse:eclipse
cd ../atlas-client/ && git pull origin master && mvn clean install && mvn eclipse:eclipse
cd ../atlas/ && git pull origin master && mvn clean install -DskipTests && mvn eclipse:eclipse
