FROM mysql:5.7

RUN mkdir -p /apps/saturn/bin \
 && mkdir -p /apps/saturn/config

ADD ./saturn-db.sh /apps/saturn/bin/saturn-db.sh
ADD ./saturn-console.sql /apps/saturn/config/saturn-console.sql

RUN chmod +x /apps/saturn/bin/saturn-db.sh

CMD ["/apps/saturn/bin/saturn-db.sh"]
