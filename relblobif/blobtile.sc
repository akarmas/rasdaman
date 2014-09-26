/*
* This file is part of rasdaman community.
*
* Rasdaman community is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Rasdaman community is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
*
* Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
rasdaman GmbH.
*
* For more information please see <http://www.rasdaman.org>
* or contact Peter Baumann via <baumann@rasdaman.com>.
*/
#include "config.h"
#include "mymalloc/mymalloc.h"
// This is -*- C++ -*-
/*************************************************************
 *
 *
 * PURPOSE:
 * implements blobtile interface using the PostgreSQL DBMS.
 *
 *
 * COMMENTS:
 * - uses LO blob method, this has been tested to be faster than bytea
 * - RMDBG macros generate so weird output that I added DEBUG macros
 *   although this is very ugly (2 trace facilities in parallel)
 * - exceptions thrown are r_Error, either directly or through common
 *   function generateException() (reladminif) which throws r_Ebase_dbms
 *
 ************************************************************/

using namespace std;

#include <stdio.h>
#include <stdlib.h>             /* atoi */
#include <set>

// simple trace facility
// trace macros are activated through externally defined compile variable DEBUG
#include "debug-srv.hh"
#include "reladminif/sqlglobals.h"
#include "reladminif/sqlitewrapper.hh"

// all the DBMS independent code is factored out and
// will be included in the resulting .c file
//#include "blobtile.cc"
#include "blobtile.hh"
#include "raslib/rmdebug.hh"
#include "reladminif/objectbroker.hh"
#include "reladminif/dbref.hh"
#include "reladminif/sqlerror.hh"
#include "inlinetile.hh"
#include "tilecache.hh"

// -- enterprise start
#define DB_STORAGE 0
#define FILE_STORAGE 1
// -- enterprise end

using namespace std;

// update blob in ras_tiles table, identified by variable myOId (from blobtile.cc), update map ref
void
BLOBTile::updateInDb() throw (r_Error)
{
    RMDBGENTER(3, RMDebug::module_blobif, "BLOBTile", "updateInDb() " << myOId);
    ENTER( "BLOBTile::updateInDb" );
    if (!fileStorageInitialized)
        fileStorage = initFileStorage();

    long long tile;
    long long indbmyoid = 0;
    short   dataformat = 0;

    // (1) --- get tuple
    dataformat = dataFormat;
    indbmyoid = myOId.getCounter();

    SQLiteQuery query("SELECT BlobId FROM RAS_TILES WHERE BlobId = %lld", indbmyoid);
    if (query.nextRow())
    {
        tile = query.nextColumnLong();
        if (query.nextRow())
        {
            LEAVE( "BLOBTile::updateInDb() more than one tile with id " << indbmyoid << " found." );
            RMInit::logOut << "BLOBTile::updateInDb() more than one tile with id " << indbmyoid << " found." << endl;
            throw r_Ebase_dbms( SQLITE_ERROR, "more than one tile with same id found in database" );
        }
    }
    else
    {
        LEAVE( "BLOBTile::updateInDb() no tile with id " << indbmyoid << " found." );
        RMInit::logOut << "BLOBTile::updateInDb() no tile with id " << indbmyoid << " found." << endl;
        throw r_Ebase_dbms( SQLITE_NOTFOUND, "tile not found in database" );
    }

    if (TileCache::cacheLimit > 0)
    {
        CacheValue* value = new CacheValue( cells, size, true, myOId, tile, this );
        value->setFileStorage(true);
        TileCache::insert( myOId, value );
    }
    else
    {
        fileStorage.update(cells, size, indbmyoid);
    }

    SQLiteQuery::executeWithParams("UPDATE RAS_TILES SET DataFormat = %d WHERE BlobId = %lld", dataformat, indbmyoid);

    // (4) --- update map ref
    DBObject::updateInDb();

    LEAVE( "BLOBTile::updateInDb, myOId=" << myOId );
    RMDBGEXIT(3, RMDebug::module_blobif, "BLOBTile", "updateInDb() " << myOId);
} // updateInDb()

// insert new blob into ras_tiles table, update map ref
// tuple is identified by blobtile.cc var myOId
// data is taken from buffer 'cells' containing 'size' bytes
void
BLOBTile::insertInDb() throw (r_Error)
{
    RMDBGENTER(3, RMDebug::module_blobif, "BLOBTile", "insertInDb() " << myOId);
    ENTER( "BLOBTile::insertInDb" );
    if (!fileStorageInitialized)
        fileStorage = initFileStorage();

    long long blobOid = myOId.getCounter();
    short   dataformat2  = dataFormat;

    TALK( "myOId.getCounter = " << blobOid );

    fileStorage.insert(cells, size, blobOid);

    // (2) --- insert tuple into db
    SQLiteQuery::executeWithParams("INSERT INTO RAS_TILES ( BlobId, DataFormat ) VALUES  ( %lld, %d )", blobOid, dataformat2);

    // (3) --- update map ref
    DBObject::insertInDb();

    if (TileCache::cacheLimit > 0)
    {
        CacheValue* value = new CacheValue( cells, size, true, myOId, blobOid, this );
        value->setFileStorage(FILE_STORAGE);
        TileCache::insert( myOId, value );
    }

    LEAVE( "BLOBTile::insertInDb(), myOId=" << myOId );
    RMDBGEXIT(3, RMDebug::module_blobif, "BLOBTile", "insertInDb() " << myOId);
} // insertInDb()

// delete one tuple from ras_tiles table, update map ref
// tuple is identified by blobtile.cc var myOId
void
BLOBTile::deleteFromDb() throw (r_Error)
{
    RMDBGENTER(3, RMDebug::module_blobif, "BLOBTile", "deleteFromDb() " << myOId);
    ENTER( "BLOBTile::deleteFromDb" );

    if (!fileStorageInitialized)
        fileStorage = initFileStorage();

    long long blobId;    // blob tuple primary key
    long long blobOid;    // blob oid "ptr"

    // get counter value (primary key) from oid
    blobId = myOId.getCounter();

    SQLiteQuery query("SELECT BlobId FROM RAS_TILES WHERE BlobId = %ld", blobId);
    if (query.nextRow())
    {
        blobOid = query.nextColumnLong();
        if (query.nextRow())
        {
            LEAVE( "BLOBTile::deleteFromDb() more than one tile with id " << blobId << " found." );
            RMInit::logOut << "BLOBTile::deleteFromDb() more than one tile with id " << blobId << " found." << endl;
            throw r_Ebase_dbms( SQLITE_ERROR, "more than one tile with same id found in database" );
        }
    }
    else
    {
        LEAVE( "BLOBTile::deleteFromDb() no tile with id " << blobId << " found." );
        RMInit::logOut << "BLOBTile::deleteFromDb() no tile with id " << blobId << " found." << endl;
        throw r_Ebase_dbms( SQLITE_NOTFOUND, "tile not found in database" );
    }

    // (2) --- delete blob identified by blobOid
    fileStorage.remove(blobId);

    // remove from cache if necessary
    if (TileCache::cacheLimit > 0)
    {
        TileCache::removeKey(myOId);
    }

    SQLiteQuery::executeWithParams("DELETE FROM RAS_TILES WHERE BlobId = %lld", blobId);

    // update map ref
    DBObject::deleteFromDb();

    LEAVE( "BLOBTile::deleteFromDb, myOId=" << myOId );
    RMDBGEXIT(3, RMDebug::module_blobif, "BLOBTile", "deleteFromDb() " << myOId);
}

// delete a range of tuple(s) from ras_tiles table, update map ref
// tuples are identified by target and a range
void
BLOBTile::kill(const OId& target, unsigned int range)
{
    RMDBGENTER(0, RMDebug::module_blobif, "BLOBTile", "kill(" << target << ", " << range <<")");
    ENTER( "BLOBTile::kill, target=" << target << ", range=" << range );

    if (!fileStorageInitialized)
        fileStorage = initFileStorage();

    long indbmyOId5;
    long indbmyOId6;
    long blobOid;    // blob oid "ptr"

    DBObject* targetobj = NULL;

    if (range == 0)     // single tuple
    {
        // (1) --- delete form cache
        targetobj = ObjectBroker::isInMemory(target);
        if (targetobj)
        {
            targetobj->setPersistent(false);
        }

        // (2) --- free blob
        indbmyOId5 = target.getCounter();
        SQLiteQuery query("SELECT BlobId FROM RAS_TILES WHERE BlobId = %lld", indbmyOId5);
        if (query.nextRow())
        {
            blobOid = query.nextColumnLong();
            if (query.nextRow())
            {
                LEAVE( "BLOBTile::kill() more than one tile with id " << indbmyOId5 << " found." );
                RMInit::logOut << "BLOBTile::kill() more than one tile with id " << indbmyOId5 << " found." << endl;
                throw r_Ebase_dbms( SQLITE_ERROR, "more than one tile with same id found in the database." );
            }
        }
        else
        {
            LEAVE( "BLOBTile::kill() no tile with id " << indbmyOId5 << " found." );
            RMInit::logOut << "BLOBTile::kill() no tile with id " << indbmyOId5 << " found." << endl;
            throw r_Ebase_dbms( SQLITE_NOTFOUND, "tile not found in database" );
        }

        // delete blob identified by blobOid
        fileStorage.remove(indbmyOId5);

        // remove from cache if necessary
        if (TileCache::cacheLimit > 0)
        {
            TileCache::removeKey(indbmyOId5);
        }

        SQLiteQuery::executeWithParams("DELETE FROM RAS_TILES WHERE BlobId = %lld", indbmyOId5);
    }
    else
    {
        // (1) --- iterate over cache and remove
        DBObjectPMap& mapRef = ObjectBroker::getMap(target.getType());
        DBObjectPMap::iterator it = mapRef.begin();
        DBObjectPMap::iterator theEnd = mapRef.end();
        OId end(target.getCounter() + range, target.getType());
        while (it != theEnd)
        {
            if (target <= (const OId&)(*it).first && (*it).first <= (const OId&)end)
            {
                (*it).second->setPersistent(false);
            }
        }

        // (2) --- iterate over db and remove
        SQLiteQuery query("SELECT BlobId FROM RAS_TILES WHERE %lld <= BlobId AND BlobId <= %lld", indbmyOId5, indbmyOId6);

        // loop over elements & delete each one
        while (query.nextRow())
        {
            blobOid = query.nextColumnLong();

            // delete blob identified by blobOid
            fileStorage.remove(blobOid);

            // remove from cache if necessary
            if (TileCache::cacheLimit > 0)
            {
                TileCache::removeKey(blobOid);
            }
        }

        // (3) --- delete tuples in db
        indbmyOId5 = target.getCounter();
        indbmyOId6 = end.getCounter();
        SQLiteQuery::executeWithParams("DELETE FROM RAS_TILES WHERE %lld <= BlobId AND BlobId <= %lld", indbmyOId5, indbmyOId6);
    }

    LEAVE( "BLOBTile::kill" );
    RMDBGEXIT(0, RMDebug::module_blobif, "BLOBTile", "kill(" << target << " " << target.getType() << ")");
}

// read tuple from ras_tiles, identified by blobtile.cc var myOId
// allocates necessary mem into ptr 'cells' and fills it; must be freed elsewhere
// external var 'size' is set to the number of bytes read
void
BLOBTile::readFromDb() throw (r_Error)
{
    RMDBGENTER(3, RMDebug::module_blobif, "BLOBTile", "readFromDb() " << myOId);
    ENTER( "BLOBTile::readFromDb" );

    if (!fileStorageInitialized)
        fileStorage = initFileStorage();

#ifdef RMANBENCHMARK
    DBObject::readTimer.resume();
#endif

    long long blobOid;
    long long  indbmyOId3;
    short   dataformat3;

    indbmyOId3 = myOId.getCounter();
    SQLiteQuery query("SELECT DataFormat FROM RAS_TILES WHERE BlobId = %lld", indbmyOId3);
    if (query.nextRow())
    {
        blobOid = indbmyOId3;
        dataformat3 = query.nextColumnInt();
        if (query.nextRow())
        {
            LEAVE( "BLOBTile::readFromDb() more than one tile with id " << indbmyOId3 << " found." );
            RMInit::logOut << "BLOBTile::readFromDb() more than one tile with id " << indbmyOId3 << " found." << endl;
            throw r_Ebase_dbms( SQLITE_ERROR, "more than one tile with same id found in database" );
        }
    }
    else
    {
        LEAVE( "BLOBTile::readFromDb() no tile with id " << indbmyOId3 << " found." );
        RMInit::logOut << "BLOBTile::readFromDb() no tile with id " << indbmyOId3 << " found." << endl;
        throw r_Error(r_Error::r_Error_ObjectUnknown);
    }

    // we have a tuple, extract data format
    dataFormat = (r_Data_Format)dataformat3;
    currentFormat = (r_Data_Format)dataformat3;
    TALK( "got dataFormat " << dataFormat );

    if (TileCache::cacheLimit > 0 && TileCache::contains( blobOid ))
    {
        CacheValue* cached = TileCache::get( blobOid );
        if (size == 0)
        {
            size = cached->getSize();
        }
        cells = cached->getData();
        cached->addReferencingTile(this);

        TALK("BLOBTile::readFromDb() - data cached, copying cells: " << (void*)cached->getData() << ", to new cells: " << (void*)cells);
    }
    else
    {
        // (2) --- open, read, close blob
        fileStorage.retrieve(blobOid, &cells, &size);

        if (TileCache::cacheLimit > 0)
        {
            CacheValue* value = new CacheValue( cells, size, false, myOId, blobOid, this, dataFormat );
            value->setFileStorage(true);
            TileCache::insert( blobOid, value );
        }
    }
    RMDBGIF(20, RMDebug::module_blobif, "BLOBTileOutput", for (int a = 0; a < size; a++)\
            RMInit::dbgOut << " " << hex << (int)(cells[a]); RMInit::dbgOut << dec << endl;)

    DBObject::readFromDb();

#ifdef RMANBENCHMARK
    DBObject::readTimer.pause();
#endif

    LEAVE( "BLOBTile::readFromDb" );
    RMDBGEXIT(3, RMDebug::module_blobif, "BLOBTile", "readFromDb() " << myOId);
}


void BLOBTile::writeCachedToDb(CacheValue* value)
{
    ENTER( "BLOBTile::write()");

    if (value && value->isUpdate())
    {
        long blobOid = -1;
        r_Bytes size = value->getSize();
        char* cells = value->getData();
        OId myOId = value->getOId();

        blobOid = value->getBlobOid();
        fileStorage.update(cells, size, myOId.getCounter());
    }

    delete value;
    LEAVE("BLOBTile::write()");
}

bool BLOBTile::checkMixedStorageSupport()
{
    bool ret = true;
    return ret;
}