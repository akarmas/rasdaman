# -*-Makefile-*-
#
# This file is part of rasdaman community.
#
# Rasdaman community is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Rasdaman community is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
# rasdaman GmbH.
#
# For more information please see <http://www.rasdaman.org>
# or contact Peter Baumann via <baumann@rasdaman.com>.
# 
# MAKEFILE FOR:  
#   petascope
# 
# COMMENTS:
# 
##################################################################

SECORE_TARGET_DIR=`pwd`/target
WAR_DIR=$(DESTIRD)$(wardir)


if GENERATE_DOCS
secore_doc:
	mvn javadoc:javadoc 2>&1 > /dev/null || exit 0
endif

all-local:
	cp secore-core/pom.xml.in secore-core/pom.xml
	cp secore-web/pom.xml.in secore-web/pom.xml
	sed -i "s|@SECORE_TARGET_DIR@|${SECORE_TARGET_DIR}|g" secore-core/pom.xml
	sed -i "s|@SECORE_TARGET_DIR@|${SECORE_TARGET_DIR}|g" secore-web/pom.xml
	mvn package

install-exec-hook:
	mkdir -p ${WAR_DIR}/secoredb
	chmod -R 777 ${WAR_DIR}/secoredb
	cp ${SECORE_TARGET_DIR}/def.war ${WAR_DIR}/ 

clean-local:
	mvn clean
	rm -rf ${SECORE_TARGET_DIR}
	rm -f secore-core/pom.xml
	rm -f secore-web/pom.xml
