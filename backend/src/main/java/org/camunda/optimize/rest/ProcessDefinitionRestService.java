package org.camunda.optimize.rest;

import org.camunda.optimize.dto.optimize.query.BranchAnalysisDto;
import org.camunda.optimize.dto.optimize.query.BranchAnalysisQueryDto;
import org.camunda.optimize.dto.optimize.query.ExtendedProcessDefinitionOptimizeDto;
import org.camunda.optimize.dto.optimize.query.HeatMapQueryDto;
import org.camunda.optimize.dto.optimize.query.HeatMapResponseDto;
import org.camunda.optimize.dto.optimize.query.ProcessDefinitionGroupOptimizeDto;
import org.camunda.optimize.dto.optimize.rest.FlowNodeNamesDto;
import org.camunda.optimize.rest.providers.Secured;
import org.camunda.optimize.service.es.reader.BranchAnalysisReader;
import org.camunda.optimize.service.es.reader.FrequencyHeatMapReader;
import org.camunda.optimize.service.es.reader.ProcessDefinitionReader;
import org.camunda.optimize.service.exceptions.OptimizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Secured
@Path("/process-definition")
@Component
public class ProcessDefinitionRestService {

  @Autowired
  private FrequencyHeatMapReader frequencyHeatMapReader;

  @Autowired
  private BranchAnalysisReader branchAnalysisReader;

  @Autowired
  private ProcessDefinitionReader processDefinitionReader;


  @POST
  @Path("/{id}/flowNodeNames")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public FlowNodeNamesDto getFlowNodeNames(
      @PathParam("id") String processDefinitionId,
      List<String> nodeIds
  ) {
    return processDefinitionReader.getFlowNodeNames(processDefinitionId, nodeIds);
  }

  /**
   * Retrieves all process definition stored in Optimize.
   *
   * @param includeXml A parameter saying if the process definition xml should be included to the response.
   * @return A collection of process definitions.
   * @throws OptimizeException Something went wrong while fetching the process definitions.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ExtendedProcessDefinitionOptimizeDto> getProcessDefinitions(
      @QueryParam("includeXml") boolean includeXml) throws OptimizeException {
    return processDefinitionReader.getProcessDefinitions(includeXml);
  }

  /**
   * Retrieves all process definition stored in Optimize and groups them by key.
   *
   * @return A collection of process definitions.
   * @throws OptimizeException Something went wrong while fetching the process definitions.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/groupedByKey")
  public List<ProcessDefinitionGroupOptimizeDto> getProcessDefinitionsGroupedByKey() throws OptimizeException {
    return processDefinitionReader.getProcessDefinitionsGroupedByKey();
  }

  /**
   * Get the process definition xml to a given process definition id.
   *
   * @param processDefinitionId The process definition id the xml should be returned.
   * @return The process definition xml requested.
   */
  @GET
  @Path("/{id}/xml")
  public String getProcessDefinitionXml(@PathParam("id") String processDefinitionId) {
    String response;
    String xml = processDefinitionReader.getProcessDefinitionXml(processDefinitionId);
    if (xml == null) {
      String notFoundErrorMessage = "Could not find xml for process definition with id :" + processDefinitionId;
      throw new NotFoundException(notFoundErrorMessage);
    } else {
      response = xml;
    }
    return response;
  }

  /**
   * Get the process definition xmls to the given process definition ids.
   *
   * @param ids List of process Definition ids for which the xmls have to be returned
   * @return The process definition xml requested.
   */
  @GET
  @Path("/xml")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Map<String, String> getProcessDefinitionsXml(@QueryParam("ids") List<String> ids) {
    return processDefinitionReader.getProcessDefinitionsXml(ids);
  }

  /**
   * Get the frequency heat map of a certain process definition.
   *
   * @param processDefinitionId Telling of which process definition the heat map is requested.
   * @return the frequency heat map.
   */
  @GET
  @Path("/{id}/heatmap/frequency")
  @Produces(MediaType.APPLICATION_JSON)
  public HeatMapResponseDto getFrequencyHeatMap(@PathParam("id") String processDefinitionId) {
    return frequencyHeatMapReader.getHeatMap(processDefinitionId);
  }

  /**
   * Get the frequency heat map of a certain process definition.
   *
   * @return the frequency heat map.
   */
  @POST
  @Path("/heatmap/frequency")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public HeatMapResponseDto getFrequencyHeatMap(HeatMapQueryDto to) {
    return frequencyHeatMapReader.getHeatMap(to);
  }

  /**
   * Get the branch analysis from the given query information.
   *
   * @return All information concerning the branch analysis.
   */
  @POST
  @Path("/correlation")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public BranchAnalysisDto getBranchAnalysis(BranchAnalysisQueryDto to) {
    return branchAnalysisReader.branchAnalysis(to);
  }

}
