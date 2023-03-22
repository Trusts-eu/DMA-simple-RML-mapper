# fetch basic image
FROM maven:3.3.9-jdk-8

# application placed into /opt/app
RUN mkdir -p /opt/app && mkdir /mapping_files/ && mkdir /temp_mappings/
WORKDIR /opt/app

# selectively add the POM file and
# install dependencies
COPY my_mapper/pom.xml /opt/app/
# RUN mvn install

# rest of the project
COPY my_mapper/src /opt/app/src
ENV MAVEN_CONFIG /var/maven/.m2
RUN mvn -Duser.home=/var/maven clean package

# sample mapping files
COPY sample_mappings/* /default_mappings_files/
ENV MAPPER_REPERTOIRE_PATH /mapping_files/
# temporary directory for instances of mappings
ENV MAPPER_TEMP_PATH /temp_mappings/
# the DMA-specific configuration:
ENV MAPPER_DMA_ASSET_BASE_PATH http://dma.at/data_assets/

# local application port
EXPOSE 8080

ADD start.sh /start.sh
# for openshift
RUN chmod -R 777 /var/maven && chmod -R 777 /temp_mappings/ && chmod -R 777 /mapping_files/ && chmod +x /start.sh
# execute it
CMD ["/start.sh"]
