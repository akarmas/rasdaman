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
 * Copyright 2003 - 2015 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

#ifndef _BLOBFILESTORAGETRANSACTION_HH_
#define _BLOBFILESTORAGETRANSACTION_HH_

#include <vector>
#include <string>
#include "blobfile.hh"
#include "blobfstransactionlock.hh"
#include "blobfscommon.hh"

using blobfs::BlobData;

namespace blobfs {

/**
 * Handles blob file storage transactions: insert, remove, update. This is a
 * base class holding common functionality; subclasses implement the specific
 * commit/abort procedures.
 *
 * Every transaction has a separate working directory under $RASDATA/TRANSACTIONS,
 * e.g. insert.XXXXXX, update.XXXXXX, ...
 *
 * Commit/abort are atomic -- in case of a crash a broken state is accordingly
 * completed (either fully committed/aborted or rolled back) when the server is
 * restarted. Every rasserver process at startup scans the TRANSACTION directory
 * at startup, the first to notice an issue does the cleanup.
 */
class BlobFSTransaction
{
    friend class TestBlobFSTransaction;
    friend class TestBlobFS;

public:

    /**
     * Initialize given storage paths. The transaction directory
     * fileStorageTransactionPath is typically empty, and is initialized here.
     */
    BlobFSTransaction(BlobFSConfig& config,
                      const std::string& transactionDir,
                      const std::string& fileStorageTransactionPath) throw (r_Error);

    /**
     * Clears any transaction locks and removes the transaction directory in
     * $RASDATA/TRANSACTIONS.
     */
    virtual ~BlobFSTransaction();

    /**
     * Add blob data to the list of pending operations to be executed in this transaction.
     */
    virtual void add(BlobData& blobData) throw (r_Error) = 0;

    /**
     * To be called before committing to RASBASE. This is utilized only on remove
     * transactions -- files are moved from TILES to the transaction dir in this
     * step.
     */
    virtual void preRasbaseCommit() throw (r_Error);

    /**
     * To be called after committing to RASBASE; all file operations are
     * finalized here.
     */
    virtual void postRasbaseCommit() throw (r_Error);

    /**
     * To be called after ROLLBACK on RASBASE.
     */
    virtual void postRasbaseAbort() throw (r_Error);

    /**
     * Finalize an interrupted transaction (e.g. by a crash).
     */
    void finalizeUncompleted() throw (r_Error);

    /**
     * Given a blob ID return its absolute file path in the final $RASDATA/TILES
     * location.
     */
    const std::string getFinalBlobPath(long long blobId) throw (r_Error);

    /**
     * Given a blob ID return its absolute file path in the temporary transaction
     * directory (under $RASDATA/TRANSACTIONS).
     */
    const std::string getTmpBlobPath(long long blobId) throw (r_Error);

    // Return the right transaction object, based on the given transaction path;
    // Return NULL in case of invalid path.
    static BlobFSTransaction* getBlobFSTransaction(
                            const std::string& transactionPath, BlobFSConfig& config);

protected:

    BlobFSTransaction(BlobFSConfig& config);

    /**
     * Finalize a transaction that crashed during a commit/abort on RASBASE.
     * Determining if a commit/abort on RASBASE has actually succeeded or rollbacked
     * during a crash does not seem possible, so this checks whether every blob in the
     * transaction directory exists in RASBASE as well, in order to decide
     * whether to remove it or revert it.
     */
    void finalizeRasbaseCrash() throw (r_Error);

    /**
     * Add the blob files in the temp transaction directory to the pending blobIds.
     */
    void collectBlobIds();

    /**
     * Add a blob file path to the list of pending blobIds; returns true if successful
     * (blob file is a number > 0)
     */
    bool addBlobId(const std::string blobPath);

    /**
     * @return false if the transaction state is invalid
     * (transaction.lock exists but is not locked)
     */
    bool validState();

    /**
     * @return false if the transaction state is invalid
     * (transaction_commit.lock exists but is not locked)
     */
    bool validCommitState();

    /**
     * Return false if the transaction state is invalid
     * (transaction_abort.lock exists but is not locked)
     */
    bool validAbortState();

    /**
     * Create temporary transaction directory under $RASDATA/TRANSACTIONS
     * for a given transaction type
     */
    void initTransactionDirectory(const std::string& transactionType) throw (r_Error);

    BlobFSConfig& config;

    // Root file storage path, tiles can be organized in a flat-file scheme, or
    // nested in subdirectories in subdir TILES
    std::string transactionPath;

    // Blob ids participating in the current transaction.
    std::vector<long long> blobIds;

    // Underlying transaction lock handler, used in the commit/abort handlers.
    BlobFSTransactionLock* transactionLock;

    static const std::string FILESTORAGE_INSERT_TRANSACTIONS_SUBDIR;
    static const std::string FILESTORAGE_UPDATE_TRANSACTIONS_SUBDIR;
    static const std::string FILESTORAGE_REMOVE_TRANSACTIONS_SUBDIR;

    static const long long INVALID_DIR_INDEX = -1;

private:

    // these caches dramatically improve performance of getFinalBlobPath, however it
    // must be noted that failure can happen because of this if some $RASDATA/TILES
    // subdir is somehow removed while rasserver is running.
    long long dir1IndexCache;
    long long dir2IndexCache;
    std::stringstream blobPathStream;

};

class BlobFSInsertTransaction: public BlobFSTransaction
{
public:
    BlobFSInsertTransaction(BlobFSConfig& config,
                               const std::string& fileStorageTransactionPath = std::string()) throw (r_Error);

    virtual void add(BlobData& blobData) throw (r_Error);

    // To be called after commit to RASBASE
    virtual void postRasbaseCommit() throw (r_Error);
    // To be called before abort to RASBASE
    virtual void postRasbaseAbort() throw (r_Error);
};

class BlobFSUpdateTransaction: public BlobFSTransaction
{
public:
    BlobFSUpdateTransaction(BlobFSConfig& config,
                               const std::string& fileStorageTransactionPath = std::string()) throw (r_Error);

    virtual void add(BlobData& blobData) throw (r_Error);

    // To be called after commit to RASBASE
    virtual void postRasbaseCommit() throw (r_Error);
    // To be called before abort to RASBASE
    virtual void postRasbaseAbort() throw (r_Error);
};

class BlobFSRemoveTransaction: public BlobFSTransaction
{
public:
    BlobFSRemoveTransaction(BlobFSConfig& config,
                               const std::string& fileStorageTransactionPath = std::string()) throw (r_Error);

    virtual void add(BlobData& blobData) throw (r_Error);

    // To be called before commit to RASBASE
    virtual void preRasbaseCommit() throw (r_Error);
    // To be called after commit to RASBASE
    virtual void postRasbaseCommit() throw (r_Error);
    // To be called before abort to RASBASE
    virtual void postRasbaseAbort() throw (r_Error);
};

class BlobFSSelectTransaction: public BlobFSTransaction
{
public:
    BlobFSSelectTransaction(BlobFSConfig& config) throw (r_Error);

    virtual void add(BlobData& blobData) throw (r_Error);
};

}

#endif
