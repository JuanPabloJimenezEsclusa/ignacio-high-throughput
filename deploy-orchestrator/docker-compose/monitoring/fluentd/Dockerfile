FROM fluent/fluentd:v1.19-debian

ARG FLUENTD_VERSION=1.19

USER root

# https://github.com/uken/fluent-plugin-elasticsearch
RUN gem install elasticsearch --no-document \
    && gem install fluent-plugin-elasticsearch --no-document \
    && gem cleanup

USER fluent

COPY fluent.conf /fluentd/fluent.conf

CMD ["fluentd", "-c", "/fluentd/fluent.conf", "-v"]
