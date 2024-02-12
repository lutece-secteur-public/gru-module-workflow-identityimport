/*
 * Copyright (c) 2002-2024, City of Paris
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
package fr.paris.lutece.plugins.workflow.modules.identityimport.task.archive;

import fr.paris.lutece.api.user.User;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistory;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistoryHome;
import fr.paris.lutece.plugins.identityimport.service.BatchService;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityStoreException;
import fr.paris.lutece.plugins.workflow.modules.identityimport.task.IdentityTask;
import fr.paris.lutece.plugins.workflowcore.business.resource.ResourceHistory;
import fr.paris.lutece.plugins.workflowcore.service.resource.IResourceHistoryService;
import fr.paris.lutece.plugins.workflowcore.service.resource.ResourceHistoryService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class BatchArchiveTask extends IdentityTask
{

    // Constants
    private static final String TASK_TITLE = "module.workflow.identityimport.archive.title";

    // Services
    private static final IResourceHistoryService _resourceHistoryService = SpringContextService.getBean( ResourceHistoryService.BEAN_SERVICE );

    @Override
    public boolean processTaskWithResult( final int nIdResourceHistory, final HttpServletRequest request, final Locale locale, final User user )
    {
        // Get resource id as parent ID for processing child actions
        final ResourceHistory resourceHistory = _resourceHistoryService.findByPrimaryKey( nIdResourceHistory );
        boolean bStatus = false;
        try
        {
            BatchService.instance( ).purgeBatch( resourceHistory.getIdResource( ) );
            final CandidateIdentityHistory candidateIdentityHistory = new CandidateIdentityHistory( );
            candidateIdentityHistory.setWfResourceHistoryId( resourceHistory.getId( ) );
            candidateIdentityHistory.setStatus( "Archived" );
            candidateIdentityHistory.setComment( this.buildHistoryComment( "Identity archived", null ) );
            CandidateIdentityHistoryHome.insert( candidateIdentityHistory );
            bStatus = true;
        }
        catch( final IdentityStoreException e )
        {
            AppLogService.error( "A problem occurred during archiving, batch not found with (id : " + resourceHistory.getIdResource( ) + ")" );
        }

        return bStatus;
    }

    @Override
    public String getTitle( Locale pLocale )
    {
        return I18nService.getLocalizedString( TASK_TITLE, pLocale );
    }
}
