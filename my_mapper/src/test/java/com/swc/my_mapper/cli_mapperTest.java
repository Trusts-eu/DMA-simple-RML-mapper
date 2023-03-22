package com.swc.my_mapper;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Tomas Knap
 */
@Ignore
public class cli_mapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(cli_mapperTest.class);
    private static ValueFactory VF = SimpleValueFactory.getInstance();


    @BeforeClass
    public static void setUp() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("DMA_PLACEHOLDER_DATASET_ID", "ID1234");
        env.put("DMA_PLACEHOLDER_REPOSITORY_LOCATION", "repoLocationX");
        env.put("DMA_PLACEHOLDER_DMA_NODE", "nodeX.eu");
        env.put("DMA_PLACEHOLDER_DOCUMENTATION_URL", "http://documentation.nodeX.eu");
        setEnv(env);
    }

    @Test
    public void tryRegexDmaSource() {

        String input = "rml:source \"DMA_SOURCE_PLACEHOLDER\"";
        String rege = "rml\\:source \\\"DMA\\_SOURCE\\_PLACEHOLDER\\\"";
        String newString = "rml\\:source \\\"file\\\"";
        String newtext = input.replaceAll(rege, newString );
        assertEquals("rml:source \"file\"", newtext);
    }


    @Test
    public void tryRegexDmaSourceTab() {

        String input = "rml:source\t\t\n\"DMA_SOURCE_PLACEHOLDER\"";
        String rege = "rml\\:source\\s+\\\"DMA\\_SOURCE\\_PLACEHOLDER\\\"";
        String newString = "rml\\:source \\\"file\\\"";
        String newtext = input.replaceAll(rege, newString );
        assertEquals("rml:source \"file\"", newtext);
    }


    @Test
    public void expandPlaceholdersSubjectDMAIdCorrectMatch() {

        Value r = cli_mapper.expandPlaceholders(VF.createIRI("http://DMA_PLACEHOLDER_DMA_NODE/DMA_PLACEHOLDER_REPOSITORY_LOCATION/DMA_PLACEHOLDER_DATASET_ID"), VF);
        assertEquals("http://nodeX.eu/repoLocationX/ID1234", r.stringValue());
    }

    @Test
    public void expandPlaceholdersSubjectDMAIdMissingEnvVariable() {

        Value r = cli_mapper.expandPlaceholders(VF.createIRI("http://DMA_PLACEHOLDER_DMA_NODE/DMA_PLACEHOLDER_REPOSITORY_LOCATION/XXX_PLACEHOLDER_DATASET_ID"), VF);
        assertEquals("http://nodeX.eu/repoLocationX/XXX_PLACEHOLDER_DATASET_ID", r.stringValue());
    }

    @Test
    public void expandPlaceholdersLiteralDMAIdCorrectMatch() {

        Value r = cli_mapper.expandPlaceholders(VF.createLiteral("DMA_PLACEHOLDER_DATASET_ID"), VF);
        assertEquals("ID1234", r.stringValue());
    }

    @Test
    public void expandPlaceholdersLiteralDMAIdWrongPrefixSoNotReplaced() {

        Value r = cli_mapper.expandPlaceholders(VF.createLiteral("XXX_PLACEHOLDER_DATASET_ID"), VF);
        assertEquals("XXX_PLACEHOLDER_DATASET_ID", r.stringValue());
    }


    @Test
    public void mapAdequateOriginalFile() {

        String path_to_mapping = (new java.io.File("src/test/resources/sample_mappings/")).getAbsolutePath();
        String mapping_filename = new File(path_to_mapping, "ADEQUATE_Original_mapping.rml.ttl").getAbsolutePath();
        String outputfile = "output.ttl";

        try {
            cli_mapper.mapFile(path_to_mapping, mapping_filename, outputfile, "");
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

        //TODO assert on output

    }

    @Test
    public void mapDmaServicesFile() {

        String path_to_mapping = (new java.io.File("src/test/resources/sample_mappings/")).getAbsolutePath();
        String mapping_filename = new File(path_to_mapping, "DMA_Services_mapping.rml.ttl").getAbsolutePath();
        String outputfile = "output.ttl";

        try {
            cli_mapper.mapFile(path_to_mapping, mapping_filename, outputfile, "");
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

        //TODO assert on output

    }

    @Test
    public void getPublisherId() throws IOException {
        String token = "AT-24977-tDJ2ZJCIjaZnVISOdQTqGbNC-KTasvAE";
        web_mapper web_mapper = new web_mapper();
        web_mapper.getPublisherIdFromToken(token);
    }

    @Test
    @Ignore
    public void mapEodc1File() {

        String path_to_mapping = (new java.io.File("src/test/resources/sample_mappings/")).getAbsolutePath();
        String mapping_filename = new File(path_to_mapping, "EODC1_mapping.rml.ttl").getAbsolutePath();
        String outputfile = "output.ttl";

        try {
            cli_mapper.mapFile(path_to_mapping, mapping_filename, outputfile, "");
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

        //TODO assert on output

    }

    protected static void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }
}
