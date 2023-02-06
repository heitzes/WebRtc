FROM registry01.idc-sginfra.net/stove-platform/stove-tomcat:jdk11-t8.5.78-1.2.4
MAINTAINER Cloudtechteam <sgs_ct_t@smilegate.com>

ENV JAR_PATH /stove/deploy/{project}/{project}.jar

USER root

ADD build/libs/{project}.jar /stove/deploy/{project}/{project}.jar

RUN chown -R stove:stove /stove && \
    chmod -R 744 /stove/deploy

USER stove

CMD ["/stove/apps/startup.sh"]