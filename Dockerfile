FROM registry01.idc-sginfra.net/stove-platform/stove-tomcat:jdk11-t8.5.78-1.2.4

MAINTAINER Cloudtechteam <sgs_ct_t@smilegate.com>

USER root

ADD build/libs/signalling2-0.0.1-SNAPSHOT.jar /stove/deploy/{project}/{project}.jar

RUN sed -i "s/{baseProject}/{project}/g" /stove/apps/tomcat/conf/server.xml && \
    chown -R stove:stove /stove && \
    chmod -R 744 /stove/deploy

USER stove

CMD ["/stove/apps/startup.sh"]