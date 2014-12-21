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

#include <string>

#include "../../../../common/src/unittest/gtest.h"
#include "../../../src/sockets/client/clientponghandler.hh"
#include "../../../src/messages/communication.pb.h"

using namespace rasnet;
using namespace base;
using namespace zmq;


TEST(ClientPongHandler, ConstructorDestructor)
{
    ClientPongHandler* handler;
    EXPECT_NO_THROW(handler = new ClientPongHandler());
    EXPECT_NO_THROW(delete handler);
}

TEST(ClientPongHandler, canHandle)
{
    BaseMessage envelope;
    AlivePong pong;
    AlivePing ping;
    ClientPongHandler handler;

    envelope.set_type(pong.GetTypeName());
    envelope.set_data(pong.SerializeAsString());
    EXPECT_TRUE(handler.canHandle(envelope));

    envelope.Clear();
    envelope.set_type(ping.GetTypeName());
    envelope.set_data(ping.SerializeAsString());
    EXPECT_FALSE(handler.canHandle(envelope));
}

TEST(ClientPongHandler, handle)
{
    std::string addr = "inproc://id";
    ClientPongHandler handler;
    context_t ctx;
    socket_t client(ctx, ZMQ_PAIR);
    socket_t server(ctx, ZMQ_PAIR);

    server.bind(addr.c_str());
    client.connect(addr.c_str());

    BaseMessage envelope;
    envelope=envelope.default_instance();

    EXPECT_ANY_THROW(handler.handle(envelope,server));

    AlivePong pong;

    envelope.set_type(pong.GetTypeName());
    envelope.set_data(pong.SerializeAsString());

    EXPECT_NO_THROW(handler.handle(envelope, server));
}
