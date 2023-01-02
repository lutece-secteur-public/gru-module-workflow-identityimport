/*
 * Copyright (c) 2002-2022, City of Paris
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

import fr.paris.lutece.plugins.workflowcore.business.config.ITaskConfigDAO;
import fr.paris.lutece.util.sql.DAOUtil;

public class IdentityImportTaskConfigDAO implements ITaskConfigDAO<IdentityImportTaskConfig>
{

    // Constants
    private static final String SQL_QUERY_SELECT = "SELECT id_task, id_workflow, id_state1, id_state2, id_state3 FROM workflow_task_identity_import_cf WHERE id_task = ?";
    private static final String SQL_QUERY_INSERT = "INSERT INTO workflow_task_identity_import_cf ( id_task, id_workflow, id_state1, id_state2, id_state3 ) VALUES ( ?, ?, ?, ?, ? ) ";
    private static final String SQL_QUERY_DELETE = "DELETE FROM workflow_task_identity_import_cf WHERE id_task = ? ";
    private static final String SQL_QUERY_UPDATE = "UPDATE workflow_task_identity_import_cf SET id_workflow = ?, id_state1 = ?, id_state2 = ?, id_state3 = ?  WHERE id_task = ?";

    @Override
    public void insert( IdentityImportTaskConfig config )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT ) )
        {
            daoUtil.setInt( 1, config.getIdTask( ) );
            daoUtil.setInt( 2, config.getIdWorkflow( ) );
            daoUtil.setInt( 3, config.getIdState1( ) );
            daoUtil.setInt( 4, config.getIdState2( ) );
            daoUtil.setInt( 5, config.getIdState3( ) );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public void store( IdentityImportTaskConfig config )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_UPDATE ) )
        {
            daoUtil.setInt( 1, config.getIdWorkflow( ) );
            daoUtil.setInt( 2, config.getIdState1( ) );
            daoUtil.setInt( 2, config.getIdState2( ) );
            daoUtil.setInt( 2, config.getIdState3( ) );
            daoUtil.setInt( 4, config.getIdTask( ) );
            daoUtil.executeUpdate( );
        }
    }

    @Override
    public IdentityImportTaskConfig load( int nIdTask )
    {

    	IdentityImportTaskConfig idImportTaskConfig = null;

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT ) )
        {
            daoUtil.setInt( 1, nIdTask );
            daoUtil.executeQuery( );
            if ( daoUtil.next( ) )
            {
                idImportTaskConfig = new IdentityImportTaskConfig( );
                int i = 1;
                idImportTaskConfig.setIdTask( daoUtil.getInt( i++ ) );
                idImportTaskConfig.setIdWorkflow( daoUtil.getInt( i++ ) );
                idImportTaskConfig.setIdState1( daoUtil.getInt( i++ ) );
                idImportTaskConfig.setIdState2( daoUtil.getInt( i++ ) );
                idImportTaskConfig.setIdState3( daoUtil.getInt( i++ ) );
            }
        }

        return idImportTaskConfig;
    }

    @Override
    public void delete( int nIdTask )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE ) )
        {
            daoUtil.setInt( 1, nIdTask );
            daoUtil.executeUpdate( );
        }
    }

}
