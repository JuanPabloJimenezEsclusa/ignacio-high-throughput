FROM fluent/fluentd:v1.19-debian

USER root

# https://github.com/uken/fluent-plugin-elasticsearch
RUN gem install fluent-plugin-elasticsearch --no-document --version "6.0.0" \
    && gem cleanup

USER fluent

COPY fluent.conf /fluentd/fluent.conf

CMD ["fluentd", "-c", "/fluentd/fluent.conf", "-v"]
