FROM bitnami/kafka:3

USER root
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /wait
RUN chmod +x /wait

COPY ./entrypoint.sh /

CMD /wait && /entrypoint.sh