/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swc.my_mapper;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.swc.my_mapper.cli_mapper.generate_random_filename;

/**
 *
/**
 *
 * @author Victor Mireles Chavez
 *         Semantic Web Company GmbH
 *         Part of Data Market Austria   project. FFG.
 * 
 * with help from: https://stackoverflow.com/questions/13703807/post-in-restful-web-service
 * 
 * Example:
 *   http://localhost:8080/RMLMapper/mapper/mappingLibrary/ADEQUATE_Original_mapping?format=xml&temp_id=abcdf12343
 *    and the contents of: 
 *       http://adequate-project.semantic-web.at:5003/www_opendataportal_at/all_course_events_2016w/blob/master/original.json
 *   In the POST body
 */
@Path("/mapper/")
public class web_mapper {

    public static final String RML_TTL_SUFFIX = ".rml.ttl";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(web_mapper.class);

    @POST
    @Path("/MapOnTheFly")
    @Consumes({"application/x-www-form-urlencoded"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response handle(@FormParam("RMLFILE") String rml, @FormParam("INPUTFILE") String input, @FormParam("Format") String format) 
    {
        String prefix = "TEMPORARY_MAPPINGS_";
        if (!cli_mapper.checkRDFContent(rml))
        {
            LOG.error("RDF malformed! ");
             return Response.status(500).entity("RDF malformed! ").build();                
        }
        System.out.println("RDF passed ");
        String filename = prefix+generate_random_filename(rml);
        String name_parts[] = filename.split("\\/");
        String clean_name = name_parts[name_parts.length - 1];
        File f;
        try {
            f = new File(cli_mapper.get_repertoire_path(), clean_name+RML_TTL_SUFFIX);
            if(f.exists() && !f.isDirectory()) { 
                LOG.debug("\n !!File exists "+ f.getAbsolutePath() +"\n\n");
            }
            FileUtils.writeStringToFile(f, rml);
            LOG.debug("\n \t!!!\n Wrote to file:  "+f.getAbsolutePath()+" \n \t!!!\n\n");
        } catch (IOException ex) {
            LOG.error(ex.getLocalizedMessage(),ex);
            return Response.status(500).entity("Could not write file _ " + ex.getLocalizedMessage()).build();
        }
                                
        String temp_id = "ID_RANDOM_1";
        String output = "";
        try {
            output = "" + cli_mapper.MapText(input,clean_name,format,temp_id, "");
            Files.deleteIfExists(Paths.get(f.getAbsolutePath()));
        } catch (IOException ex) {
            LOG.error(ex.getLocalizedMessage(),ex);
            return Response.status(400).entity(output).build();
        }
        catch (PathNotFoundException ex)
        {
            LOG.error("Path not found in RML!" + ex.getLocalizedMessage(),ex);
            return Response.status(400).entity("Input Not compliant with mapping file").build();
        }
        catch (Exception ex)
        {
            LOG.error(ex.getLocalizedMessage(),ex);
            return Response.status(500).entity("Internal error in the mapper service").build();
        }
        if (output.length() < 5) {
            return Response.status(400).entity("Input Not compliant with mapping file").build();
        }
        return Response.status(200).entity(output).build();
    }
    
    @POST
    @Path("/mappingLibrary/{param}")
    @Produces(MediaType.TEXT_PLAIN + "; charset=UTF-8")
    public Response mapService(@PathParam("param") String mapping_name,
    @QueryParam("format") String format,  @QueryParam("temp_id") String temp_id, @QueryParam("token") String token, String msg) {

        String publisherId = "";
        try {
            publisherId = getPublisherIdFromToken(token);
        } catch (Exception e) {
            LOG.error("Cannot get publisherId from the token: " + e.getLocalizedMessage(),e);
        }


        File f = new File(cli_mapper.get_repertoire_path(), mapping_name+RML_TTL_SUFFIX);
        try {
            if (!f.exists()) {
                LOG.error("Mapping not found for template name: '{}', input file: {}", mapping_name, msg);
                return Response.status(404).entity("Mapping not found").build();
            }

        } catch (Exception ex) {
            LOG.error("Mapping not accessible for template name: '{}'. Reason: {}", mapping_name, ex.getLocalizedMessage(), ex);
            LOG.error("Input file {}, content: {}.", mapping_name, msg);
            return Response.status(500).entity("Mapping not accessible").build();
        }
        String output = "";
        try {
            output = "" + cli_mapper.MapText(msg,mapping_name,format,temp_id,publisherId);
        } catch (Exception ex) {
            LOG.error("Mapping failed for template name: '{}'. Reason: {}", mapping_name, ex.getLocalizedMessage(), ex);
            LOG.error("Input file {}, content: {}.", mapping_name, msg);
            throw new WebApplicationException("Failed to map the input.");
        }
        if (output.length() < 5) {
            LOG.error("Mapping process finished for template name: '{}', but the output: '{}' is suspicious.", mapping_name, output);
            LOG.error("Input file {}, content: {}.", mapping_name, msg);
            return Response.status(400).entity("Input Not compliant with mapping file").build();
        }
        return Response.status(200).entity(output).encoding("UTF-8").build();
    }

    public String getPublisherIdFromToken(String token) throws IOException {

        LOG.info("Token: {}", token);

        //request
        URL url = new URL("https://cas.datamarket.at/cas/oauth2.0/profile?access_token=" + token);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);

        int responseCode = con.getResponseCode();
        LOG.info("Response code: {}", responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //parse json
        LOG.info("Response: {}", response.toString());

        JsonObject jobj = new Gson().fromJson(response.toString(), JsonObject.class);
        String id = jobj.get("id").getAsString();
        LOG.info("Publisher ID: {}", id);
        return id;
    }

    @POST
    @Path("/insertMapping")
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertMapping(@QueryParam("temp_id") String temp_id, String msg)            
    {
        String prefix = "INSERTED_MAPPINGS_";
        if (!cli_mapper.checkRDFContent(msg))
        {
            LOG.error("RDF malformed for temp_id: '{}' and content '{}'", temp_id, msg);
            return Response.status(500).entity("RDF malformed! ").build();
        }
        LOG.debug("RDF passed for temp_id: '{}'", temp_id);
        String filename = prefix+temp_id;
        try {
            String name_parts[] = filename.split("\\/");
            String clean_name = name_parts[name_parts.length - 1];
            File f = new File(cli_mapper.get_repertoire_path(), clean_name+RML_TTL_SUFFIX);
            if(f.exists() && !f.isDirectory()) {
                LOG.error("No mapping added. Mapping already exists under the name {}", clean_name+RML_TTL_SUFFIX);
                return Response.status(500).entity("File " + f.getAbsolutePath() + " exists").build();
            }
            FileUtils.writeStringToFile(f, msg, "UTF-8", false);
            LOG.debug("Mapping wrote to file: {}. ", f.getAbsolutePath());
        } catch (IOException ex) {
            LOG.error("Cannot wrote mapping {}. Reason: {}", temp_id, ex.getLocalizedMessage(), ex);
            return Response.status(500).entity("Cannot save mapping rule").build();
        }
        return Response.status(200).entity(filename+" now in mapping repertoire").build();
    }
                   
    @GET
    @Path("/inspectMapping/{param}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getMapping(@PathParam("param") String mapping_name) {
        if (mapping_name.charAt(0) == '.')
        {
            return Response.status(500).build();
        }
        String output = "";      
        String name_parts[] = mapping_name.split("\\/");
        String clean_name = name_parts[name_parts.length - 1];
        try {
            output = cli_mapper.test_reading(new File(cli_mapper.get_repertoire_path(), clean_name+ RML_TTL_SUFFIX));
        } catch (IOException ex) {
            return Response.status(404).build();
        }
        return Response.status(200).entity(output).build();
    }
               
    @GET
    @Path("/listMappings")
    @Produces(MediaType.TEXT_PLAIN)
    public Response listOfMappings() {
        String output = "";
        try
        {
            LOG.debug("Checking list of mappings from {}", cli_mapper.get_repertoire_path());
            File folder = new File(cli_mapper.get_repertoire_path());
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
              if (listOfFiles[i].isFile()) 
              {
                  String fname = listOfFiles[i].getName();              
                  if (!(fname.endsWith(RML_TTL_SUFFIX)) || (fname.startsWith("TEMPORARY_MAPPINGS_")))
                  {
                      continue;
                  }              
                  String mapName = fname.substring(0,fname.length()-8);
                  output += mapName+"\n";
              }
            }
            
        }
        catch (Exception e)
        {
            LOG.error(e.getLocalizedMessage(), e.getStackTrace());
            return Response.status(500).entity(e.getLocalizedMessage()).build();
        }
        return Response.status(200).entity(output).build();
    }

    @GET
    @Path("/healthCheck")  //http://localhost:8080/RMLMapper/mapper/healthCheck
    public Response healthCheck() {

        //check important resources available
        File repertoireFolder = new File(cli_mapper.get_repertoire_path());
        if (!repertoireFolder.canWrite()) {
            LOG.error("No read access to assets folder " + repertoireFolder);
            throw new WebApplicationException("No read access to repertoire folder");
        }

        File tempFolder = new File(cli_mapper.get_temp_path());
        if (!tempFolder.canWrite()) {
            LOG.error("No read access to temp folder " + tempFolder);
            throw new WebApplicationException("No read access to temp folder");
        }

        return Response.status(200).entity("OK").build();
    }
   
}
