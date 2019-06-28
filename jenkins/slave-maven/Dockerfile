FROM jenkins/jnlp-agent-maven

RUN apk update && apk upgrade && apk add --no-cache bash git openssh wkhtmltopdf wget ca-certificates && \
    wget -O /tmp/pandoc.tar.gz https://github.com/jgm/pandoc/releases/download/2.7.3/pandoc-2.7.3-linux.tar.gz && \
    tar xvzf /tmp/pandoc.tar.gz && \
    mv pandoc-2.7.3 /usr/lib/pandoc && \
    ln -s /usr/lib/pandoc/bin/pandoc /usr/local/bin/pandoc

RUN curl --insecure -o ./sonarscanner.zip -L https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-3.3.0.1492.zip && \
	unzip sonarscanner.zip && \
	rm sonarscanner.zip && \
	mv sonar-scanner-3.3.0.1492 /usr/lib/sonar-scanner && \
    ln -s /usr/lib/sonar-scanner/bin/sonar-scanner /usr/local/bin/sonar-scanner

RUN curl --insecure -o ./sonar-cnes-report.jar -L https://github.com/lequal/sonar-cnes-report/releases/download/2.2.0/sonar-cnes-report.jar && \
    mkdir /usr/lib/sonar-cnes-report && \
    mv sonar-cnes-report.jar /usr/lib/sonar-cnes-report
	
ENV SONAR_RUNNER_HOME=/usr/lib/sonar-scanner

ENV CNES_REPORT_HOME=/usr/lib/sonar-cnes-report

ENV PANDOC_HOME=/usr/lib/pandoc
