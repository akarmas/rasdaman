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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU  General Public License for more details.
 *
 * You should have received a copy of the GNU  General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2014 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

package petascope.wms2.service.getcapabilities;

import org.jetbrains.annotations.NotNull;
import petascope.wms2.service.base.Controller;
import petascope.wms2.service.base.Validator;

import java.util.List;

/**
 * Controller for the get capabilities request. It parses the raw request into the typed one and handles it
 * to return a response for the frontend
 *
 * @author <a href="mailto:dumitru@rasdaman.com">Alex Dumitru</a>
 */
public class GetCapabilitiesController extends Controller<GetCapabilitiesRequest, GetCapabilitiesParser,
    GetCapabilitiesHandler, GetCapabilitiesResponse> {

    /**
     * Constructor for the class
     *
     * @param parser     the parser for this controller
     * @param validators the validators for this controller
     * @param handler    the handler for this controller
     */
    public GetCapabilitiesController(@NotNull GetCapabilitiesParser parser,
                                     @NotNull List<Validator> validators,
                                     @NotNull GetCapabilitiesHandler handler) {
        super(parser, validators, handler);
    }
}
