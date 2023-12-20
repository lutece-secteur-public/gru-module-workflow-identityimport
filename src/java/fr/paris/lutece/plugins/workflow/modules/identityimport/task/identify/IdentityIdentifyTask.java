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
package fr.paris.lutece.plugins.workflow.modules.identityimport.task.identify;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentity;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityAttributeHome;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistory;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistoryHome;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHome;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AttributeDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AuthorType;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.IdentityDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.RequestAuthor;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.ResponseStatus;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.ResponseStatusType;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.IdentityChangeRequest;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.IdentityChangeResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.search.IdentitySearchResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.util.Constants;
import fr.paris.lutece.plugins.identitystore.v3.web.service.IdentityService;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityStoreException;
import fr.paris.lutece.plugins.workflow.modules.identityimport.task.IdentityTask;
import fr.paris.lutece.plugins.workflowcore.business.resource.ResourceHistory;
import fr.paris.lutece.plugins.workflowcore.service.resource.IResourceHistoryService;
import fr.paris.lutece.plugins.workflowcore.service.resource.ResourceHistoryService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class IdentityIdentifyTask extends IdentityTask
{

    // Constants
    private static final String TASK_TITLE = "module.workflow.identityimport.identify.title";

    // Services
    private static final IResourceHistoryService _resourceHistoryService = SpringContextService.getBean( ResourceHistoryService.BEAN_SERVICE );

    private final IdentityService identityService = SpringContextService.getBean( "identityService.rest.httpAccess.v3" );

    @Override
    public boolean processTaskWithResult( final int nIdResourceHistory, final HttpServletRequest request, final Locale locale, final User user )
    {
        // Get resource id as parent ID for processing child actions
        final ResourceHistory resourceHistory = _resourceHistoryService.findByPrimaryKey( nIdResourceHistory );
        boolean bStatus = false;
        final String selectedCustomerId = request.getParameter( Constants.PARAM_ID_CUSTOMER );
        if ( selectedCustomerId != null )
        {
            final Optional<CandidateIdentity> optIdentity = CandidateIdentityHome.findByPrimaryKey( resourceHistory.getIdResource( ) );
            if ( optIdentity.isPresent( ) )
            {
                final CandidateIdentity candidateIdentity = optIdentity.get( );
                candidateIdentity.setAttributes( CandidateIdentityAttributeHome.getCandidateIdentityAttributesList( candidateIdentity.getId( ) ) );

                final RequestAuthor requestAuthor = new RequestAuthor( );
                requestAuthor.setName( AppPropertiesService.getProperty( "identityimport_config.request.author" ) );
                requestAuthor.setType( AuthorType.application );

                try
                {
                    ResponseStatus status;
                    String header;
                    /* If override attribute -> update identity before identifying */
                    if ( request.getParameterMap( ).entrySet( ).stream( ).anyMatch( entry -> entry.getKey( ).startsWith( "override-" ) ) )
                    {
                        final IdentityChangeRequest updateRequest = new IdentityChangeRequest( );
                        final IdentityDto identity = new IdentityDto( );
                        identity.setCustomerId( selectedCustomerId );
                        final String lastUpdateDate = request.getParameter( "last_update_date" );
                        identity.setLastUpdateDate( Timestamp.valueOf( lastUpdateDate ) );
                        updateRequest.setIdentity( identity );
                        final List<String> keys = request.getParameterMap( ).keySet( ).stream( )
                                .filter( key -> key.startsWith( "override-" ) && !key.endsWith( "-certif" ) )
                                .map( key -> StringUtils.removeStart( key, "override-" ) ).collect( Collectors.toList( ) );
                        identity.getAttributes( ).addAll( keys.stream( ).map( key -> {
                            final String value = request.getParameter( "override-" + key );
                            final String certif = request.getParameter( "override-" + key + "-certif" );
                            final String timestamp = request.getParameter( "override-" + key + "-timestamp-certif" );

                            final AttributeDto attributeDto = new AttributeDto( );
                            attributeDto.setKey( key );
                            attributeDto.setValue( value );
                            attributeDto.setCertifier( certif );
                            attributeDto.setCertificationDate( new Timestamp( Long.parseLong( timestamp ) ) );
                            return attributeDto;
                        } ).collect( Collectors.toList( ) ) );
                        final IdentityChangeResponse response = identityService.updateIdentity( selectedCustomerId, updateRequest,
                                candidateIdentity.getClientAppCode( ), requestAuthor );
                        status = response.getStatus( );
                        header = "Identité sélectionnée et mise à jour.\n\nAPI UPDATE identity";
                    }
                    /* No update needed, just get the identity to check if it still exists */
                    else
                    {
                        final IdentitySearchResponse response = identityService.getIdentity( selectedCustomerId, candidateIdentity.getClientAppCode( ),
                                requestAuthor );
                        status = response.getStatus( );
                        header = "Identité sélectionnée.\n\nAPI GET identity";
                    }

                    /* Complete workflow history with custom fields */
                    final CandidateIdentityHistory candidateIdentityHistory = new CandidateIdentityHistory( );
                    candidateIdentityHistory.setWfResourceHistoryId( resourceHistory.getId( ) );
                    candidateIdentityHistory.setStatus( status.getType( ).name( ) );
                    candidateIdentityHistory.setComment( this.buildHistoryComment( header, status ) );
                    CandidateIdentityHistoryHome.insert( candidateIdentityHistory );

                    /* Process response */
                    if ( status.getType( ) == ResponseStatusType.OK || status.getType( ) == ResponseStatusType.SUCCESS
                            || status.getType( ) == ResponseStatusType.INCOMPLETE_SUCCESS )
                    {
                        candidateIdentity.setCustomerId( selectedCustomerId );
                        bStatus = true;
                    }
                    CandidateIdentityHome.update( candidateIdentity );
                }
                catch( final IdentityStoreException e )
                {
                    AppLogService.error(
                            "A problem occurred during identification, candidate identity not identified (id : " + resourceHistory.getIdResource( ) + ")" );
                }
            }
            else
            {
                AppLogService
                        .error( "A problem occurred during identification, candidate identity not identified (id : " + resourceHistory.getIdResource( ) + ")" );
            }
        }
        else
        {
            AppLogService.error( "A problem occurred during identification, candidate identity not identified (id " + resourceHistory.getIdResource( ) + ")" );
        }

        return bStatus;
    }

    @Override
    public String getTitle( Locale pLocale )
    {
        return I18nService.getLocalizedString( TASK_TITLE, pLocale );
    }
}
