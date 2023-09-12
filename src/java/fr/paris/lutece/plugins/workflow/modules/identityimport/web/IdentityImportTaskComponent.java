/*
 * Copyright (c) 2002-2023, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.workflow.modules.identityimport.web;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;

import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistory;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistoryHome;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.workflow.modules.identityimport.task.IdentityImportTaskConfig;
import fr.paris.lutece.plugins.workflow.web.task.NoFormTaskComponent;
import fr.paris.lutece.plugins.workflowcore.business.config.ITaskConfig;
import fr.paris.lutece.plugins.workflowcore.business.state.State;
import fr.paris.lutece.plugins.workflowcore.service.task.ITask;
import fr.paris.lutece.portal.service.admin.AdminUserService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.workflow.WorkflowService;
import fr.paris.lutece.portal.web.constants.Messages;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.beanvalidation.BeanValidationUtil;
import fr.paris.lutece.util.html.HtmlTemplate;

public class IdentityImportTaskComponent extends NoFormTaskComponent
{
    // MARKERS
    private static final String MARK_WORKFLOW_ID = "workflow_id";
    private static final String MARK_STATUS = "status";
    private static final String MARK_COMMENT = "comment";
    private static final String MARK_STATE_ID_1 = "state_id_1";
    private static final String MARK_STATE_ID_2 = "state_id_2";
    private static final String MARK_STATE_ID_3 = "state_id_3";
    private static final String MARK_WORKFLOW_STATES = "json_workflow_states";

    // PARAMETERS
    private static final String PARAM_STATE1 = "state1";
    private static final String PARAM_STATE2 = "state2";
    private static final String PARAM_STATE3 = "state3";
    private static final String PARAM_WORKFLOW = "workflow";

    // TEMPLATES
    private static final String TEMPLATE_TASK_IDENTITYIMPORT_CONFIG = "admin/plugins/workflow/modules/identityimport/identityimport_task_config.html";
    private static final String TEMPLATE_CANDIDATE_IDENTITY_HISTORY = "/admin/plugins/identityimport/candidate_identity_history.html";

    @Override
    public String getDisplayConfigForm( HttpServletRequest request, Locale locale, ITask task )
    {
        User user = AdminUserService.getAdminUser( request );
        if ( user == null )
        {
            throw new AppException( "Access Denied" );
        }

        // model
        final Map<String, Object> model = new HashMap<>( );

        // get config
        final IdentityImportTaskConfig config = findTaskConfig( task.getId( ) );

        // get enabled workflow list
        final ReferenceList workflowsRefList = WorkflowService.getInstance( ).getWorkflowsEnabled( user, locale );

        // remove first blank item
        workflowsRefList.remove( 0 );

        // get all available states and actions
        Map<String, Collection<State>> mapStates = new HashMap<>( );

        for ( ReferenceItem workflowItem : workflowsRefList )
        {
            // STATES
            Collection<State> workflowStates = WorkflowService.getInstance( ).getAllStateByWorkflow( Integer.valueOf( workflowItem.getCode( ) ), user );

            // put in global map
            mapStates.put( workflowItem.getCode( ), workflowStates );

        }

        model.put( MARK_WORKFLOW_STATES, getJsonStates( workflowsRefList, mapStates ) );

        model.put( MARK_WORKFLOW_ID, config.getIdWorkflow( ) );
        model.put( MARK_STATE_ID_1, config.getIdState1( ) );
        model.put( MARK_STATE_ID_2, config.getIdState2( ) );
        model.put( MARK_STATE_ID_3, config.getIdState3( ) );

        final HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_TASK_IDENTITYIMPORT_CONFIG, locale, model );
        return template.getHtml( );
    }

    @Override
    public String validateConfig( ITaskConfig config, HttpServletRequest request )
    {
        String workflow = request.getParameter( PARAM_WORKFLOW );
        String state1 = request.getParameter( PARAM_STATE1 );
        String state2 = request.getParameter( PARAM_STATE2 );
        String state3 = request.getParameter( PARAM_STATE3 );

        if ( StringUtils.isBlank( workflow ) || StringUtils.isBlank( state1 ) || StringUtils.isBlank( state2 ) || StringUtils.isBlank( state3 ) )
        {
            return AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP );
        }
        else
            if ( config instanceof IdentityImportTaskConfig )
            {
                final IdentityImportTaskConfig taskConfig = (IdentityImportTaskConfig) config;
                taskConfig.setIdWorkflow( Integer.parseInt( workflow ) );
                taskConfig.setIdState1( Integer.parseInt( state1 ) );
                taskConfig.setIdState2( Integer.parseInt( state2 ) );
                taskConfig.setIdState3( Integer.parseInt( state3 ) );

                // Check mandatory fields
                Set<ConstraintViolation<ITaskConfig>> constraintViolations = BeanValidationUtil.validate( taskConfig );

                if ( CollectionUtils.isNotEmpty( constraintViolations ) )
                {
                    return AdminMessageService.getMessageUrl( request, Messages.MANDATORY_FIELDS, AdminMessage.TYPE_STOP );
                }
            }

        return StringUtils.EMPTY;
    }

    /**
     * get config
     * 
     * @param id
     *            id Config
     * @return ActionsBatchTaskConfig
     */
    private IdentityImportTaskConfig findTaskConfig( int id )
    {
        final IdentityImportTaskConfig config = this.getTaskConfigService( ).findByPrimaryKey( id );
        return config == null ? new IdentityImportTaskConfig( ) : config;
    }

    @Override
    public String getDisplayTaskInformation( int pNIdHistory, HttpServletRequest pRequest, Locale pLocale, ITask pTask )
    {
        /* Complete workflow history with custom fields */
        final Optional<CandidateIdentityHistory> candidateIdentityHistory = CandidateIdentityHistoryHome.selectByWfHistory(pNIdHistory);
        if(candidateIdentityHistory.isPresent()){
            final Map<String, Object> model = new HashMap<>( );
            final CandidateIdentityHistory candidateIdentityHistory1 = candidateIdentityHistory.get();
            model.put( MARK_STATUS, candidateIdentityHistory1.getStatus() );
            model.put( MARK_COMMENT, candidateIdentityHistory1.getComment() );
            HtmlTemplate template = AppTemplateService.getTemplate(TEMPLATE_CANDIDATE_IDENTITY_HISTORY, pLocale, model );
            return template.getHtml( );
        }
        return StringUtils.EMPTY;
    }

    /**
     * generate Json for cascade selects in template
     * 
     * example : {"workflows":[{"id":1, "name":"dotation", "states":[{"id":12, "name":"statut11", ...
     * 
     * @param workflowsRefList
     * @param mapStates
     * @return json
     */
    private String getJsonStates( ReferenceList workflowsRefList, Map<String, Collection<State>> mapStates )
    {
        ObjectMapper mapper = new ObjectMapper( );
        ObjectNode root = mapper.createObjectNode( );

        ArrayNode jsonWfList = mapper.createArrayNode( );
        for ( ReferenceItem workflowItem : workflowsRefList )
        {
            ObjectNode jsonWf = mapper.createObjectNode( );
            jsonWf.put( "id", workflowItem.getCode( ) );
            jsonWf.put( "name", workflowItem.getName( ) );

            ArrayNode jsonStatesList = mapper.createArrayNode( );
            for ( State state : mapStates.get( workflowItem.getCode( ) ) )
            {
                ObjectNode jsonState = mapper.createObjectNode( );
                jsonState.put( "id", state.getId( ) );
                jsonState.put( "name", state.getName( ) );

                jsonStatesList.add( jsonState );
            }

            jsonWf.set( "states", jsonStatesList );
            jsonWfList.add( jsonWf );
        }

        root.set( "workflows", jsonWfList );

        return root.toPrettyString( );
    }

}
