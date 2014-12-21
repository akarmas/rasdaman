/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

#include <stdexcept>

#include "servercontroller.hh"


namespace rasnet
{
using std::runtime_error;

ServerController::ServerController()
{
    Reset();
}

ServerController::~ServerController()
{}

void ServerController::Reset()
{
    this->failed = false;
    this->failureReason = "";
}

bool ServerController::Failed() const
{
    return this->failed;
}

std::string ServerController::ErrorText() const
{
    return this->failureReason;
}

void ServerController::StartCancel()
{
    throw runtime_error("This functionality is not implemented.");
}

void ServerController::SetFailed(const std::string& reason)
{
    this->failed = true;
    this->failureReason = reason;
}

bool ServerController::IsCanceled() const
{
    throw runtime_error("This functionality is not implemented.");
}

void ServerController::NotifyOnCancel(google::protobuf::Closure* callback)
{
    throw runtime_error("This functionality is not implemented.");
}

} /* namespace rnp */
