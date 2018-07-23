.PHONY: udf deps install restart

DRILL_HOME ?= /usr/local/drill

udf:
	mvn --quiet clean package -DSkipTests

install:
	cp target/drill-html-tools*.jar ${DRILL_HOME}/jars/3rdparty && \
	cp deps/jsoup-1.11.3.jar ${DRILL_HOME}/jars/3rdparty

restart:
	drillbit.sh restart

deps:
	mvn dependency:copy-dependencies -DoutputDirectory=deps
