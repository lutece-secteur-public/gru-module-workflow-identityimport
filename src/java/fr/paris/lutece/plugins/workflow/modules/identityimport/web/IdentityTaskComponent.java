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
package fr.paris.lutece.plugins.workflow.modules.identityimport.web;

import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistory;
import fr.paris.lutece.plugins.identityimport.business.CandidateIdentityHistoryHome;
import fr.paris.lutece.plugins.workflow.web.task.NoFormTaskComponent;
import fr.paris.lutece.plugins.workflowcore.service.task.ITask;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.util.html.HtmlTemplate;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public abstract class IdentityTaskComponent extends NoFormTaskComponent
{
    // MARKERS
    private static final String MARK_STATUS = "status";
    private static final String MARK_COMMENT = "comment";
    private static final String TEMPLATE_CANDIDATE_IDENTITY_HISTORY = "/admin/plugins/identityimport/candidate_identity_history.html";

    @Override
    public String getDisplayTaskInformation( int pNIdHistory, HttpServletRequest pRequest, Locale pLocale, ITask pTask )
    {
        /* Complete workflow history with custom fields */
        final Optional<CandidateIdentityHistory> candidateIdentityHistory = CandidateIdentityHistoryHome.selectByWfHistory( pNIdHistory );
        if ( candidateIdentityHistory.isPresent( ) )
        {
            final Map<String, Object> model = new HashMap<>( );
            final CandidateIdentityHistory candidateIdentityHistory1 = candidateIdentityHistory.get( );
            model.put( MARK_STATUS, candidateIdentityHistory1.getStatus( ) );
            final String htmlComment = candidateIdentityHistory1.getComment( ) != null ? candidateIdentityHistory1.getComment( ).replace( "\n", "<br>" )
                    : "Vide";
            model.put( MARK_COMMENT, htmlComment );
            final HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_CANDIDATE_IDENTITY_HISTORY, pLocale, model );
            return template.getHtml( );
        }
        return StringUtils.EMPTY;
    }
}
