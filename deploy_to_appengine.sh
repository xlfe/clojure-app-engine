#!bash

rm target/*-standalone.war
lein ring uberwar
if [ -d "target/war" ]; then
	rm -rf target/war
fi
mkdir target/war
cd target/war
jar xf ../*standalone.war 
cd ../..
ln -s ../../../appengine-web.xml target/war/WEB-INF/
appcfg.sh update target/war
