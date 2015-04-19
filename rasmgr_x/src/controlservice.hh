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

#ifndef RASMGR_X_SRC_CONTROLSERVICE_HH_
#define RASMGR_X_SRC_CONTROLSERVICE_HH_

#include <boost/shared_ptr.hpp>
#include <string>

#include "rasnet/src/messages/rasmgr_rasctrl_service.pb.h"
#include "controlcommandexecutor.hh"

namespace rasmgr
{

class ControlService:public ::rasnet::service::RasMgrRasCtrlService
{
public:
    ControlService(boost::shared_ptr<ControlCommandExecutor> commandExecutor);

    virtual ~ControlService();

    virtual void ExecuteCommand(::google::protobuf::RpcController* controller,
                                const ::rasnet::service::RasCtrlRequest* request,
                                ::rasnet::service::RasCtrlResponse* response,
                                ::google::protobuf::Closure* done);
private:
    boost::shared_ptr<ControlCommandExecutor> commandExecutor;

};

} /* namespace rasmgr */

#endif /* RASMGR_X_SRC_CONTROLSERVICE_HH_ */
