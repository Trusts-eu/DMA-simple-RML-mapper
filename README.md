

___



**Simple web service that wraps CARML, an implementation of the RML spec.**

## An overview of how this service interacts with others can be found here: https://docs.google.com/drawings/d/1xOvSQl3GlpL0fwHpCoH4PNkhW9VtGt3zxkv4-IJAQNI/edit?usp=sharing

# TO RUN:
## Using Docker:
build container and run binding port 8080

`docker build --rm -t dma/metadata_mapper . `

`docker run --name=mappercontainer -v mappingsvol:/mapping_files --user 9265065:9265065 -p 8080:8080 -it dma/metadata_mapper `

## In local machine:
set environment variables (see below), then:

`cd my_mapper`

`mvn clean package; java -jar target/my_mapper-jar-with-dependencies.jar`


# A quick overview of the API:


* http://localhost:8080/RMLMapper/mapper/listMappings  (GET)

    Outputs the list of available mappings

* http://localhost:8080/RMLMapper/mapper/inspectMapping/MAPPINGNAME   (GET)

    Shows the contents of the MAPPINGNAME RML

* http://localhost:8080/RMLMapper/mapper/insertMapping/?temp_id=MAPPINGNAME  (POST)

    Inserts the contents of the POST body into the mappings, with name MAPPINGNAME only if it is valid RDF

* http://localhost:8080/RMLMapper/mapper/mappingLibrary/MAPPINGNAME?format=FMT&temp_id=TEMPID (POST)

    Maps the file given in the POST body using mapping MAPPINGNAME. The format of the file is specified with format keyworkd and can be either xml or json. the temp_id keyword gives the name of the resource that is being described by the contents of the file.

* http://localhost:8080/RMLMapper/mapper/MapOnTheFly  (POST)

    Use this POST method for quick testing. It has x-www-form-urlencoded input that has three fields: RMLFILE INPUTFILE and Format. The values of the first two are URLEncoded files, the last is a string either "json" and "xml"

# Environment variables

These two variables are for the operation of the mapper:

* `MAPPER_REPERTOIRE_PATH`

    points to a folder where the available mappings and all uploaded (using insertMapping) will be saved. Recommended to copy to this location the contents of sample_mappings.

* `MAPPER_TEMP_PATH`   

    points to a folder where temporary mapping files will be written when performing a mapping. Every mapping operation (with the mappintLibrary/... command) will make a copy of both the input and the mapping files in this directory, do some preprocessing on them, and return the output to the user.  It is recommended that this directory be cleaned periodically of old files (e.g. cron job that every hour deletes files older than 10 minutes), as these files are assigned names at random and thus can't be reused anyhow.

These two are to set node-specific metadata settings

To get the possible values for a controlled vocabulary, go to the SPARQL uri provided for each such field, and enter the following query:  
```
 PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
 SELECT ?u ?l WHERE {?uri a skos:Concept. ?uri skos:prefLabel ?l.
                     FILTER (lang(?l) = 'en')} 
```
The list shows the labels in English (change the 'en' to 'de' to see German labels). What should be included in the environment variable is the URI.

The URI of a dataset will be formed as follows:
``` DMA_PLACEHOLDER_DMA_NODE/DMA_PLACEHOLDER_REPOSITORY_LOCATION/DMA_PLACEHOLDER_DATASET_ID ```
And The URI of a data distribution as follows:
``` DMA_PLACEHOLDER_DMA_NODE/DMA_PLACEHOLDER_REPOSITORY_LOCATION/DMA_PLACEHOLDER_DATASET_ID/DMA_PLACEHOLDER_DISTRIBUTION_DEFAULT_ID ```

Which necesitates setting the following env variables:

* `ENV DMA_PLACEHOLDER_DMA_NODE`
    base location of the node, including protocol
* `ENV DMA_PLACEHOLDER_REPOSITORY_LOCATION`
    path to data within node

Additionally, the following variables must also be set, which describe the Catalog of the node.

* `ENV DMA_PLACEHOLDER_CATALOG_NAME`
   Text, name of the catalog
* `ENV DMA_PLACEHOLDER_CATALOG_DESCRIPTION`
   Text, description of the catalog
* `ENV DMA_PLACEHOLDER_CATALOG_LANGUAGE`
   from the "language" controlled vocabulary [https://dma.poolparty.biz/PoolParty/sparql/language](https://dma.poolparty.biz/PoolParty/sparql/language)
* `ENV DMA_PLACEHOLDER_CATALOG_RIGHTS`
   from the "rights" vocabulary:  [https://dma.poolparty.biz/PoolParty/sparql/access-rights](https://dma.poolparty.biz/PoolParty/sparql/access-rights)
* `ENV DMA_PLACEHOLDER_CATALOG_KEYWORD`
   from the "keywords" controlled vocabulary  [https://dma.poolparty.biz/PoolParty/sparql/Eurovoc4.6](https://dma.poolparty.biz/PoolParty/sparql/Eurovoc4.6)
* `ENV DMA_PLACEHOLDER_CLIENT_NUMBER`
   agreed upon when registering to DMA
* `ENV DMA_PLACEHOLDER_PRICE_MODEL`  
   choose from the "price model" vocabulary  [https://dma.poolparty.biz/PoolParty/sparql/price-model](https://dma.poolparty.biz/PoolParty/sparql/price-model)
* `ENV DMA_PLACEHOLDER_CATALOG_IDENTIFIER`
   identifier of the catalog, to be agreed upon when registering to DMA

# Known Issues

* https://redmine.datamarket.at/issues/313

# Credits
This software was originaly developed by Victor Mireles and Tomas Knap from Semantic Web Company, in terms of the [Data Market Austria](https://datamarket.at/) project in 2018. Said project was funded by the Austrian Research Funding Agency (FFG) and the Federal Ministry for Transportations, Innovationd and Technology (BMVIT).

Further testing and reuse was done in terms of project [TRUSTS: Trusted secure data sharing space](https://www.trusts-data.eu/), which received funding from the European Union's Horizon 2020 research and innovation programme under grant agreement [No 871481](https://cordis.europa.eu/project/id/871481).





