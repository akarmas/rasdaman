#ifndef TEST_MOCKS_MOCKRASSERVER_HH_
#define TEST_MOCKS_MOCKRASSERVER_HH_

#include <boost/smart_ptr.hpp>

#include "../../common/src/mock/gmock.h"

#include "../../src/server.hh"
#include "../../src/databasehost.hh"
#include "../../src/userdatabaserights.hh"

class MockRasServer:public rasmgr::Server{
public:
  MOCK_METHOD0(isClientAlive, bool(void));
  MOCK_METHOD1(isClientAlive, bool(const std::string&) );
  MOCK_METHOD4(allocateClientSession, void(const std::string&,const std::string&,const std::string&,const rasmgr::UserDatabaseRights& dbRights));
  MOCK_METHOD2(deallocateClientSession, void(const std::string&, const std::string&));
  MOCK_METHOD1(registerServer, void(const std::string&));
  MOCK_METHOD1(stop, void(bool));
  MOCK_METHOD0(isStarting, bool(void));
  MOCK_METHOD0(isFree, bool(void));
  MOCK_METHOD0(isAvailable, bool(void));
  MOCK_METHOD0(isOccupied, bool(void));
  MOCK_METHOD0(startProcess, void(void));
  MOCK_METHOD0(isAlive, bool(void));
  MOCK_METHOD0(getTotalSessionNo, boost::uint32_t(void));
  MOCK_CONST_METHOD0(getPort, boost::int32_t(void));
  MOCK_CONST_METHOD0(getHostName, std::string(void));
  MOCK_CONST_METHOD0(getServerId, std::string(void));
};

#endif