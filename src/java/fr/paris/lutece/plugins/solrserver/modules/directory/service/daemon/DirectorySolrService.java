/*
 * Copyright (c) 2002-2013, Mairie de Paris
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
package fr.paris.lutece.plugins.solrserver.modules.directory.service.daemon;

import fr.paris.lutece.plugins.directory.business.Directory;
import fr.paris.lutece.plugins.directory.business.DirectoryFilter;
import fr.paris.lutece.plugins.directory.business.DirectoryHome;
import fr.paris.lutece.plugins.directory.business.IEntry;
import fr.paris.lutece.plugins.directory.business.Record;
import fr.paris.lutece.plugins.directory.business.RecordField;
import fr.paris.lutece.plugins.directory.business.RecordFieldFilter;
import fr.paris.lutece.plugins.directory.business.RecordFieldHome;
import fr.paris.lutece.plugins.directory.business.RecordHome;
import fr.paris.lutece.plugins.directory.service.DirectoryPlugin;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;


/**
 * DirectorySearchService
 */
public class DirectorySolrService
{
    private static DirectorySolrService _singleton;

    /**
    *
    * @return singleton
    */
    public static DirectorySolrService getInstance(  )
    {
        if ( _singleton == null )
        {
            _singleton = new DirectorySolrService(  );
        }

        return _singleton;
    }

    /**
     * @throws IOException
     * @throws SolrServerException
     */
    public String processIndexing( boolean bCreate ) throws SolrServerException, IOException
    {
        StringBuffer sbLogs = new StringBuffer(  );

        //TODO un service du core donne l'url
        SolrServer server = new CommonsHttpSolrServer( "http://localhost:8080/solrserver/solr" );

        server.deleteByQuery( "*:*" ); // delete everything!
        server.commit(  );

        Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>(  );

        Plugin plugin = PluginService.getPlugin( DirectoryPlugin.PLUGIN_NAME );

        for ( Directory directory : DirectoryHome.getDirectoryList( new DirectoryFilter(  ), plugin ) )
        {
            RecordFieldFilter recordFieldFilter = new RecordFieldFilter(  );
            recordFieldFilter.setIdDirectory( directory.getIdDirectory(  ) );

            for ( Record record : RecordHome.getListRecord( recordFieldFilter, plugin ) )
            {
                SolrInputDocument solrDoc = new SolrInputDocument(  );
                solrDoc.addField( "id", "dir_record_" + record.getIdRecord(  ), 1.0f );
                solrDoc.addField( "dir_i", directory.getIdDirectory(  ), 1.0f );

                List<Integer> lIdRecordList = new ArrayList<Integer>(  );
                lIdRecordList.add( record.getIdRecord(  ) );

                List<RecordField> recordFieldList = RecordFieldHome.getRecordFieldListByRecordIdList( lIdRecordList,
                        plugin );

                for ( RecordField recordField : recordFieldList )
                {
                    IEntry entry = recordField.getEntry(  );

                    if ( entry instanceof fr.paris.lutece.plugins.directory.business.EntryTypeText ||
                            entry instanceof fr.paris.lutece.plugins.directory.business.EntryTypeComment ||
                            entry instanceof fr.paris.lutece.plugins.directory.business.EntryTypeRichText ||
                            entry instanceof fr.paris.lutece.plugins.directory.business.EntryTypeTextArea )
                    {
                        String title = recordField.getEntry(  ).getTitle(  );
                        String value = recordField.getValue(  );
                        solrDoc.addField( format( title ) + "_t", value, 1.0f );
                    }
                    else if ( entry instanceof fr.paris.lutece.plugins.directory.business.EntryTypeImg )
                    {
                        if ( recordField.getFile(  ) != null )
                        {
                            String title = recordField.getEntry(  ).getTitle(  );
                            int value = recordField.getFile(  ).getIdFile(  );
                            solrDoc.addField( "attr_" + format( title ), value, 1.0f );
                        }
                    }
                }

                docs.add( solrDoc );
            }
        }

        server.add( docs );
        server.commit(  );

        return sbLogs.toString(  );
    }

    private String format( String strTitle )
    {
        return strTitle.replaceAll( " ", "_" ).toLowerCase( Locale.ENGLISH );
    }
}
