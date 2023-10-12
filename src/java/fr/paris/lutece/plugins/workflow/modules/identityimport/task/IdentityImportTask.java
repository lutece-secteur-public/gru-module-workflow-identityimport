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
package fr.paris.lutece.plugins.workflow.modules.identityimport.task;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentity;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityAttributeHome;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistory;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistoryHome;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHome;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AuthorType;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.IdentityDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.RequestAuthor;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.ResponseStatus;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.IdentityChangeRequest;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.crud.IdentityChangeResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.util.ResponseStatusFactory;
import fr.paris.lutece.plugins.identitystore.v3.web.service.IdentityService;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityStoreException;
import fr.paris.lutece.plugins.workflow.modules.identityimport.mapper.IdentityMapper;
import fr.paris.lutece.plugins.workflowcore.business.resource.ResourceHistory;
import fr.paris.lutece.plugins.workflowcore.service.resource.IResourceHistoryService;
import fr.paris.lutece.plugins.workflowcore.service.resource.ResourceHistoryService;
import fr.paris.lutece.plugins.workflowcore.service.task.SimpleTask;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;

public class IdentityImportTask extends SimpleTask
{

    private final static Logger _logger = Logger.getLogger( IdentityImportTask.class );

    // Constants
    private static final String TASK_TITLE = "module.workflow.identityimport.title";

    // Services
    private static final IResourceHistoryService _resourceHistoryService = SpringContextService.getBean( ResourceHistoryService.BEAN_SERVICE );

    private final IdentityService identityService = SpringContextService.getBean( "identityService.rest.httpAccess.v3" );

    @Override
    public boolean processTaskWithResult( int nIdResourceHistory, HttpServletRequest request, Locale locale, User user )
    {
        // Get resource id as parent ID for processing child actions
        final ResourceHistory resourceHistory = _resourceHistoryService.findByPrimaryKey( nIdResourceHistory );
        boolean bStatus = false;

        final Optional<CandidateIdentity> optIdentity = CandidateIdentityHome.findByPrimaryKey( resourceHistory.getIdResource( ) );
        if ( optIdentity.isPresent( ) )
        {
            final CandidateIdentity candidateIdentity = optIdentity.get( );
            candidateIdentity.setAttributes( CandidateIdentityAttributeHome.getCandidateIdentityAttributesList( candidateIdentity.getId( ) ) );

            /*
             * 1. Call IDS import API ...IdentityService.importIdentity( identity );
             *
             * 2. if the result is : - "inserted" or "identity selected" >> return true
             * 
             * 3. Otherwise return false (will set the state to "manual import")
             */
            final IdentityChangeRequest identityChangeRequest = new IdentityChangeRequest( );
            final IdentityDto identity = IdentityMapper.mapToIdentity( candidateIdentity );
            identityChangeRequest.setIdentity( identity );

            final RequestAuthor requestAuthor = new RequestAuthor( );
            requestAuthor.setName( AppPropertiesService.getProperty( "identityimport_config.request.author" ) );
            requestAuthor.setType( AuthorType.application );

            try
            {
                final IdentityChangeResponse response = identityService.importIdentity( identityChangeRequest, candidateIdentity.getClientAppCode( ),
                        requestAuthor );
                final ResponseStatus status = response.getStatus( );
                /* Complete workflow history with custom fields */
                final CandidateIdentityHistory candidateIdentityHistory = new CandidateIdentityHistory( );
                candidateIdentityHistory.setWfResourceHistoryId( resourceHistory.getId( ) );
                candidateIdentityHistory.setStatus( status.getType( ).name( ) );
                candidateIdentityHistory.setComment( status.getMessage( ) );
                CandidateIdentityHistoryHome.insert( candidateIdentityHistory );
                /* Process response */
                if ( ResponseStatusFactory.success( ).equals( status ) || ResponseStatusFactory.incompleteSuccess( ).equals( status ) )
                {
                    candidateIdentity.setCustomerId( response.getCustomerId( ) );
                    bStatus = true;
                }
                else
                {
                    bStatus = false;
                }
            }
            catch( IdentityStoreException e )
            {
                _logger.error( "A problem occured during import, candidate identity not imported (id : " + resourceHistory.getIdResource( ) + ")" );
            }

            CandidateIdentityHome.update( candidateIdentity );
        }
        else
        {
            _logger.error( "A problem occured during import, candidate identity not found (id : " + resourceHistory.getIdResource( ) + ")" );
            bStatus = false;
        }
        return bStatus;
    }

    @Override
    public String getTitle( Locale pLocale )
    {
        return I18nService.getLocalizedString( TASK_TITLE, pLocale );
    }
}
