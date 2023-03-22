/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swc.my_mapper;

import com.taxonic.carml.engine.RmlMapper;
import com.taxonic.carml.util.RmlMappingLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Victor Mireles Chavez
 * Semantic Web Company GmbH
 * Part of Data Market Austria   project. FFG.
 *
 * Can be run:
 *
 * mvn exec:java -Dexec.args="/path/to/ /path/to/mapping.rml.ttl output.ttl"
 */

public class cli_mapper {

    private static final Logger LOG = LoggerFactory.getLogger(cli_mapper.class);

    public static final String ENV_VARIABLE_PLACEHOLDER_PREFIX = "DMA_PLACEHOLDER_";

    public static final String DEFAULT_REPERTOIRE_PATH = "/tmp/mappings/";

    public static final String DEFAULT_DMA_ASSET_BASE_PATH = "http://base.url/for/data_assets/";

    public static final String DEFAULT_TMP_PATH = "/tmp/";

    public static final String DEFAULT_REPOSITORY_NAME = "This Repository";

    static String[] bad_embracers = { "\"", "\n" };

    static String get_env_var(String var_name, String default_val) {
        String env_var = System.getenv(var_name);
        if (env_var == null || env_var.isEmpty()) {
            LOG.warn("Env variable: {} is null or empty", var_name);
            return default_val;
        }
        return env_var;
    }

    static String add_final_slash(String in)
    {
        if (in.charAt(in.length()-1) == '/')
        {
            return in;
        }
        return in+"/";
    }

    public static String get_asset_path() {
        return add_final_slash(get_env_var("MAPPER_DMA_ASSET_BASE_PATH", DEFAULT_DMA_ASSET_BASE_PATH));
    }

    public static String get_repository_name() {
        return get_env_var("MAPPER_DMA_REPOSITORY_NAME", DEFAULT_REPOSITORY_NAME);
    }

    public static String get_temp_path() {
        return get_env_var("MAPPER_TEMP_PATH", DEFAULT_TMP_PATH);
    }

    public static String get_repertoire_path() {
        return get_env_var("MAPPER_REPERTOIRE_PATH", DEFAULT_REPERTOIRE_PATH);
    }

    /*
    This function does two things: 
       1) expand literals of the form [a,b,c] into three literals a, b and c
       2) if strings begin and end with one of bad_embracers it removes them from string.
            For example the string "\"foo\"" will be transformed into "foo"
    */
    public static LinkedList<Literal> expandAndCleanLiterals(Literal obj) {
        LOG.trace("C_______{}", obj);
        LinkedList<Literal> result = new LinkedList<Literal>();
        ValueFactory factory = SimpleValueFactory.getInstance();
        String objS = obj.stringValue();

        LOG.trace("String size " + (new Integer(objS.length())).toString());
        if ((objS.length() > 2) && ("[".equals(objS.substring(0, 1)))) {
            // System.out.println("---------------------\n\t USING EXPANSION:\n\t"+obj.toString()+"\n");
            String obj_short = objS.substring(1, objS.length() - 1);
            for (String objPart : obj_short.split(",")) {
                Literal new_obj = factory.createLiteral(objPart);
                result.add(new_obj);
            }
        } else {

            result.add(obj);
        }

        LinkedList<Literal> resultF = new LinkedList<Literal>();
        for (Literal obj2 : result) {
            objS = obj2.stringValue();
            for (String badd : bad_embracers) {
                if ((objS.length() > 2) && (badd.equals(objS.substring(0, 1))) && (badd.equals(objS.substring(objS.length() - 1, objS.length())))) {
                    // System.out.println("---------------------\n\t\t Removing Quotes:\n\t\t"+objS+"\n");
                    objS = objS.substring(1, objS.length() - 1);

                }
            }
            Literal new_obj = factory.createLiteral(StringUtils.strip(objS));
            resultF.add(new_obj);

        }
        return resultF;
    }

    public static boolean checkRDFContent(String content) {
        try {
            Model model;
            InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8.name()));
            model = Rio.parse(input, "", RDFFormat.TURTLE);
            if (model.isEmpty()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        String path_to_mapping;
        String mapping_filename;
        String outputfile;

        // path_to_mapping = "/home/mireleschavezv/SWC/DMA/RML/transformations/our_examples/";
        // mapping_filename = "ADEQUATE_Original_mapping.rml.ttl";        

        path_to_mapping = args[0];
        mapping_filename = args[1];
        outputfile = args[2];

        mapFile(path_to_mapping, mapping_filename, outputfile, "");
    }

    public static String test_reading(File file) throws IOException {
        String everything = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            everything = sb.toString();

        } finally {
            br.close();
        }
        return everything;

    }

    public static String generate_random_filename(String inputText) {
        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();
        String hash = new Integer(inputText.hashCode()).toString();
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        //return date + "_" + randomUUIDString + "_" + hash;
        return date + "_" + "TEST" + "_" + hash;
    }

    // From https://stackoverflow.com/a/43049065
    private static void replace_in_file(File file, String regex, String newString) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = "", oldtext = "";
        while ((line = reader.readLine()) != null) {
            oldtext += line + "\r\n";
        }
        reader.close();
        // replace a word in a file
        String newtext = oldtext.replaceAll(regex, newString);

        //To replace a line in a file
        //String newtext = oldtext.replaceAll("This is test string 20000", "blah blah blah");
        FileWriter writer = new FileWriter(file);
        writer.write(newtext);
        writer.close();
    }

    public static String MapText(String inputText, String mappingFile, String inputType, String temp_id, String publisherId) throws IOException {
        LOG.debug("Mapping of the file - preparation...started");
        String RFNAME = "" + generate_random_filename(inputText);
        String temp_path = get_temp_path();
        File mappingRFNAMEFile = new File(temp_path, RFNAME + "_mapping.rml.ttl");

        File origMappingFile = new File(get_repertoire_path(), mappingFile + ".rml.ttl");
        test_reading(origMappingFile);

        File inputRFNAMEFile = new File(temp_path, RFNAME + "." + inputType);
        LOG.trace("Write inputText to randomly-named temporary file {}", inputRFNAMEFile.getAbsolutePath());
        FileUtils.writeStringToFile(inputRFNAMEFile, inputText, StandardCharsets.UTF_8, false);

        LOG.trace("Copy default_mapping_context_path / mappingFile .rml.ttl to {}", mappingRFNAMEFile.getAbsolutePath());
        FileUtils.copyFile(origMappingFile, mappingRFNAMEFile);

        LOG.trace("Modify RFNAME_mapping.rml.ttl so that it takes RFNAME as input");
        String rege = "rml\\:source \\\"(.*?\\.\\S*)\\\"";
        replace_in_file(mappingRFNAMEFile, rege, "rml\\:source \\\"" + inputRFNAMEFile.getAbsolutePath() + "\\\"");
        rege = "rml\\:source\\s+\\\"DMA\\_SOURCE\\_PLACEHOLDER\\\"";
        replace_in_file(mappingRFNAMEFile, rege, "rml\\:source \\\"" + inputRFNAMEFile.getAbsolutePath() + "\\\"");

        LOG.trace("Modify RFNAME_mapping.rml.ttl so that the subjects are mapped all to the same ID");
        rege = "DMA\\_ID\\_PLACEHOLDER";
        replace_in_file(mappingRFNAMEFile, rege,get_asset_path()+temp_id);

        LOG.trace("Map this file using   default_mapping_context_path / RFNAME_mapping.rml.ttl");
        File resultRFNAMEFile = new File(temp_path, "endResult_" + RFNAME + ".xml");
        mapFile(temp_path, mappingRFNAMEFile.getAbsolutePath(), resultRFNAMEFile.getAbsolutePath(), publisherId);
        // Write the result of the mapping into endResult_RFNAME.ttl
        // Return  endResult_RFNAME.ttl  (the filename of the end result)
        return test_reading(resultRFNAMEFile);
    }

    /**
     * Expand all IRI resources if they contain placeholder suffix
     * @param resource
     * @return
     */
    public static Value expandPlaceholders(Value resource, ValueFactory factory) {
        boolean somethingReplaced = false;
        String value = resource.stringValue();

        for (String envVariableName : System.getenv().keySet().stream().filter(x -> x.startsWith(ENV_VARIABLE_PLACEHOLDER_PREFIX)).collect(Collectors.toList())) {
            if (value.contains(envVariableName)) {
                LOG.trace("There was something to replace for variable {}", envVariableName);
                // Could be that the replacement value is a URI:
                String new_str = System.getenv(envVariableName);
                if (new_str.startsWith("\"") && new_str.endsWith("\"")) {
                    new_str = new_str.replaceAll("\"", "");
                }
                value = value.replaceAll(envVariableName, new_str);
                somethingReplaced = true;
            }
        }
        if (somethingReplaced) {
            if (resource instanceof IRI) {
                LOG.trace("Value {} was replaced with {} (type: {})", resource.stringValue(), resource.getClass().getName());
                return factory.createIRI(value);
            } else if (resource instanceof BNode) {
                LOG.trace("Value {} was replaced with {} (type: {})", resource.stringValue(), resource.getClass().getName());
                return factory.createBNode(value);
            } else if (resource instanceof Literal) {
                LOG.trace("Value {} was replaced with {} (type: {})", resource.stringValue(), resource.getClass().getName());
                return factory.createLiteral(value);
            } else {
                LOG.warn("Do not know how to create new value {} of type {}", value, resource.getClass().getName());
            }
        }
        return resource;

    }

    public static void mapFile(String mappingPath, String mappingFile, String outputFile, String publisherId) throws IOException {
        LOG.debug("Mapping of the file...started");
        LOG.debug("PARAMETERS:\n\t" + mappingPath +
                "\n\t" + mappingFile
                + "\n\t" + outputFile);

        // Create mapper with specific basepath for logical sources.
        RmlMapper mapper = RmlMapper.newBuilder().fileResolver(Paths.get(mappingPath)).build();
        Model result = mapper.map(RmlMappingLoader
                .build()
                .load(Paths.get(mappingFile), RDFFormat.TURTLE));
        LOG.debug("_________________ Result ___________");
        LinkedList<Statement> expanded_result = new LinkedList<Statement>();
        ValueFactory factory = SimpleValueFactory.getInstance();

        if (!publisherId.isEmpty()) {
            LOG.info("There is publisherId: {}", publisherId);

            //enrich with publisherId triples for dataset
            Set<Resource> datasets = result.filter(null, RDF.TYPE, DCAT.DATASET).subjects();
            if (datasets == null || datasets.isEmpty()) {
                LOG.warn("There is no dataset, no publisherId added to dataset");
            } else if (datasets.size() > 1) {
                LOG.warn("There are more datasets {}, no publisherId added to dataset", datasets);
            }
            Resource dataset = datasets.stream().findFirst().get();
            result.add(dataset, factory.createIRI("http://www.dma.com/vocabularies/publisherId"), factory.createLiteral(publisherId));

            //enrich with publisherId triples for catalogue
            Set<Resource> catalogs = result.filter(null, RDF.TYPE, DCAT.CATALOG).subjects();
            if (catalogs == null || catalogs.isEmpty()) {
                LOG.warn("There is no catalog, no publisherId added to catalog");
            } else if (catalogs.size() > 1) {
                LOG.warn("There are more catalogs {}, no publisherId added to catalog", catalogs);
            }
            Resource catalog = catalogs.stream().findFirst().get();
            result.add(catalog, factory.createIRI("http://www.dma.com/vocabularies/publisherId"), factory.createLiteral(publisherId));

        } else {
            LOG.info("PublisherId is not available");
        }

        for (Statement s : result) {
            Value cur_obj = s.getObject();
            if (cur_obj.getClass() == SimpleLiteral.class) {
                LinkedList<Literal> new_objs = expandAndCleanLiterals((Literal) cur_obj);
                for (Literal new_obj : new_objs) {
                    Statement newStatement = factory.createStatement((Resource) expandPlaceholders(s.getSubject(), factory), (IRI) expandPlaceholders(s.getPredicate(), factory), expandPlaceholders(cur_obj, factory));
                    expanded_result.add(newStatement);
                }
            } else {
                Statement newStatement = factory.createStatement((Resource) expandPlaceholders(s.getSubject(), factory), (IRI) expandPlaceholders(s.getPredicate(), factory), expandPlaceholders(cur_obj, factory));
                expanded_result.add(newStatement);
            }

        }

        LOG.debug("_________________Expanded Result ___________");
        for (Statement s : expanded_result) {
            LOG.debug(s.getSubject().toString() + " ; \t ");
            LOG.debug(s.getPredicate().toString() + " ; \t ");
            LOG.debug(s.getObject().stringValue() + "  \n");
        }

        LOG.debug("WRITING TO: {}", outputFile);
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outputFile),StandardCharsets.UTF_8);
        try {
            LOG.debug("Starting {}", outputFile);
            RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out); // RDFXML
            writer.startRDF();
            int i = 0;
            for (Statement st : expanded_result) {
                writer.handleStatement(st);
                i++;
            }
            LOG.debug("Number of statements in the resulting file : {}\n", String.valueOf(i));
            writer.endRDF();
        } catch (RDFHandlerException e) {
            throw e;
        } finally {
            out.close();
        }
        LOG.debug("Mapping of the file...done");

    }
}
