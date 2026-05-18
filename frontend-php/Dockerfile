FROM php:8.3-apache

RUN a2enmod rewrite

COPY apache/000-default.conf /etc/apache2/sites-available/000-default.conf
COPY php.ini /usr/local/etc/php/conf.d/tma.ini

WORKDIR /var/www/html

COPY public ./public
COPY src ./src
COPY views ./views

RUN chown -R www-data:www-data /var/www/html

ENV SPRING_API_BASE_URL=http://app:8080

EXPOSE 80
