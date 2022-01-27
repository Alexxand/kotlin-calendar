FROM openjdk:8-jdk
RUN mkdir /app
COPY calendar-1.0.tar /app/
RUN cd /app && tar xvf calendar-1.0.tar
WORKDIR /app/calendar-1.0/bin
CMD ["./calendar"]