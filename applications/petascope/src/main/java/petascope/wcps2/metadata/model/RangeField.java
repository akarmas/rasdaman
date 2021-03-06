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
package petascope.wcps2.metadata.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author <a href="merticariu@rasdaman.com">Vlad Merticariu</a>
 */
public class RangeField {

    private final String type;

    private final String name;

    private final String description;

    private final List<Double> nodata;

    private final String uom;

    private final String definition;

    private final List<Interval<BigDecimal>> allowedValues;
    
    public RangeField(String type, String name, String description, List<Double> nodata, String uom, String definition,
                      List<Interval<BigDecimal>> allowedValues) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.nodata = nodata;
        this.uom = uom;
        this.definition = definition;
        this.allowedValues = allowedValues;
    }
    
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Double> getNodata() {
        return nodata;
    }

    public String getUom() {
        return uom;
    }

    public String getDefinition() {
        return definition;
    }

    public List<Interval<BigDecimal>> getAllowedValues() {
        return allowedValues;
    }

    public String getType() {
        return type;
    }
    
    public static final String TYPE = "double";
    public static final String UOM = "10^0";
}
