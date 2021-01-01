FROM oracle/graalvm-ce:20.3.0-java11
USER root
RUN id -u demiourgos728 1>/dev/null 2>&1 || (( getent group 0 1>/dev/null 2>&1 || ( type groupadd 1>/dev/null 2>&1 && groupadd -g 0 root || addgroup -g 0 -S root )) && ( type useradd 1>/dev/null 2>&1 && useradd --system --create-home --uid 1001 --gid 0 demiourgos728 || adduser -S -u 1001 -G root demiourgos728 ))
COPY target/native-image/root /opt/
RUN ["chmod", "u+x,g+x", "/opt/root"]
EXPOSE 8081
USER 1001:0
ENTRYPOINT ["/opt/root"]
CMD []
