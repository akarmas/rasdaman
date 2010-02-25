/*
 * This file is part of PetaScope.
 *
 * PetaScope is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * PetaScope is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with PetaScope. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information please see <http://www.PetaScope.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 *
 * Copyright 2009 Jacobs University Bremen, Peter Baumann.
 */




package petascope.wcps.grammar;

/**
 * RangeExpr
 *
 * @author Andrei Aiordachioaie
 */
public class RangeExpr implements IParseTreeNode
{
	String xml;

	public RangeExpr()
	{
		this.xml = "";
	}

	public RangeExpr(String field, ScalarExpr expr)
	{
		this();
		add(field, expr);
	}

	public void add(String field, ScalarExpr expr)
	{
		String tmp = "";

		tmp += "<field>" + field + "</field>";
		tmp += expr.toXML();
		xml += "<componen>" + tmp + "</component>";
	}

	public String toXML()
	{
		return xml;
	}
}
