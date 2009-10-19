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


package wcs.server.core;

//~--- non-JDK imports --------------------------------------------------------

import net.opengis.ows.v_1_0_0.BoundingBoxType;
import net.opengis.wcs.ows.v_1_1_0.DomainMetadataType;
import net.opengis.wcs.ows.v_1_1_0.InterpolationMethodType;
import net.opengis.wcs.ows.v_1_1_0.InterpolationMethods;
import net.opengis.wcs.ows.v_1_1_0.UnNamedDomainType;
import net.opengis.wcs.v_1_1_0.CoverageDescriptionType;
import net.opengis.wcs.v_1_1_0.CoverageDescriptions;
import net.opengis.wcs.v_1_1_0.CoverageDomainType;
import net.opengis.wcs.v_1_1_0.DescribeCoverage;
import net.opengis.wcs.v_1_1_0.FieldType;
import net.opengis.wcs.v_1_1_0.RangeType;
import net.opengis.wcs.v_1_1_0.SpatialDomainType;

import wcps.server.core.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import net.opengis.ows.v_1_0_0.KeywordsType;
import net.opengis.wcs.ows.v_1_1_0.AnyValue;
import net.opengis.wcs.v_1_1_0.TimeSequenceType;

/**
 * This class takes a WCS DescribeCoverage XML request and executes request,
 * building the corresponding XML respose.
 *
 * @author Andrei Aiordachioaie
 */
public class executeDescribeCoverage
{
	private static boolean printLog = true;
	private boolean finished;
	private DescribeCoverage input;
	private DbMetadataSource meta;
	private CoverageDescriptions output;

	/**
	 * Default constructor
	 * @param cap DescribeCoverage object, a WCS (or WCPS) request
	 * @param metadataDbPath Path to the "dbparams.properties" file
	 */
	public executeDescribeCoverage(DescribeCoverage cap, DbMetadataSource source)
			throws WCSException
	{
		input = cap;
		output = new CoverageDescriptions();
		finished = false;
		meta = source;
	}

	/**
	 * Log a message to System.out
	 * @param str Text of the message
	 */
	static void log(String str)
	{
		if ( printLog )
			System.out.println("<WCS DescribeCoverage> " + str);
	}

	/**
	 * Main method of this class. Retrieves the response to the DescribeCoverage
	 * request given to the constructor. If needed, it also calls <b>process()</b>
	 * @return a CoverageDescriptions object.
	 * @throws wcs_web_service.WCSException
	 */
	public CoverageDescriptions get() throws WCSException
	{
		try
		{
			if ( finished == false )
				process();
		}
		catch (WCSException e)
		{
			System.err.println("ERROR ! Could not convert the WCS xml into WCPS xml !!!");
			e.printStackTrace();

			throw e;
		}
		if ( finished == false )
			throw new WCSException("NoApplicableCode",
								   "Could not execute the GetCapabilities request! "
								   + "Please see the other errors...");

		return output;
	}

	/**
	 * Computes the response to the DescribeCoverage request given to the constructor.
	 */
	public void process() throws WCSException
	{
		String name;

		for (int i = 0; i < input.getIdentifier().size(); i++)
		{
			name = input.getIdentifier().get(i);
			output.getCoverageDescription().add(getCoverageDescription(name));
		}
		finished = true;
	}

	/**
	 * Retrieve details for one coverage.
	 * @param name Name of the coverage
	 * @return CoverageDescriptionType object, that can just be plugged in the respose object
	 */
	private CoverageDescriptionType getCoverageDescription(String name) throws WCSException
	{
		log("Building coverage description for coverage '" + name + "' ...");
		CoverageDescriptionType desc = new CoverageDescriptionType();

		// Error checking: is the coverage available?
		if ( meta.existsCoverageName(name) == false )
			throw new WCSException("InvalidParameterValue", "Identifier",
								   "Coverage " + name + " is not served by this server !");

		// Read all coverage metadata
		Metadata cov = null;

		try
		{
			cov = meta.read(name);
		}
		catch (Exception e)
		{
			e.printStackTrace();

			throw new WCSException("NoApplicableCode",
								   "Metadata for coverage " + name + " is not valid.");
		}

		desc.setIdentifier(name);
		desc.setTitle(cov.getTitle());
		desc.setAbstract(cov.getAbstract());

        KeywordsType keyword = new KeywordsType();
        Iterator<String> keys = SDU.str2string(cov.getKeywords()).iterator();
        while (keys.hasNext())
        {
            String k = keys.next();
            keyword.getKeyword().add(k);
        }
        desc.getKeywords().add(keyword);

		// Coverage Domain
		CoverageDomainType domain = null;
		Double lo1 = 0.0, lo2 = 0.0, hi1 = 0.0, hi2 = 0.0;

        /* Default Bounding Box (uses IMAGE_CRS): use image size */
        BoundingBoxType bbox = new BoundingBoxType();
        CellDomainElement X = cov.getXCellDomain();
        CellDomainElement Y = cov.getYCellDomain();
        if (X != null && Y != null)
        {
            lo1 = X.getLo().doubleValue();
            hi1 = X.getHi().doubleValue();
            lo2 = Y.getLo().doubleValue();
            hi2 = Y.getHi().doubleValue();
            
            bbox.setCrs(DomainElement.IMAGE_CRS);

            bbox.getLowerCorner().add(lo1);
            bbox.getLowerCorner().add(lo2);
            bbox.getUpperCorner().add(hi1);
            bbox.getUpperCorner().add(hi2);
        }
        else
            throw new WCSException("NoApplicableCode", "Internal error: Could " +
                    "not find X and Y cell domain extends.");

        /* Try to use WGS84 bounding box, if available */
        Wgs84Crs crs = cov.getCrs();
        BoundingBoxType bbox84 = new BoundingBoxType();
        bbox84.setCrs(DomainElement.WGS84_CRS);
        if (crs != null)
        {
            lo1 = crs.getLow1().doubleValue();
            hi1 = crs.getHigh1().doubleValue();
            lo2 = crs.getLow2().doubleValue();
            hi2 = crs.getHigh2().doubleValue();

            bbox84.getLowerCorner().add(lo1);
            bbox84.getLowerCorner().add(lo2);
            bbox84.getUpperCorner().add(hi1);
            bbox84.getUpperCorner().add(hi2);

            bbox = bbox84;
        }

		domain = new CoverageDomainType();
		SpatialDomainType spatial = new SpatialDomainType();
		spatial.getBoundingBox().add(new JAXBElement<BoundingBoxType>(
                new QName("http://www.opengis.net/ows", "BoundingBox", XMLConstants.DEFAULT_NS_PREFIX),
				BoundingBoxType.class, bbox));
		domain.setSpatialDomain(spatial);


        /* Find a time-axis if exists */
        CellDomainElement T = cov.getTCellDomain();
        if (T != null)
        {
            log("Found time-axis for coverage: [" + T.getLo() + ", " + T.getHi() + "]");
            TimeSequenceType temporal = new TimeSequenceType();
            temporal.getTimePositionOrTimePeriod().add(T.getLo().intValue());
            temporal.getTimePositionOrTimePeriod().add(T.getHi().intValue());
            domain.setTemporalDomain(temporal);
        }

		desc.setDomain(domain);

		// The coverage Range
		RangeType wcsRange = new RangeType();
		Iterator<RangeElement> rangeIt = cov.getRangeIterator();

		while (rangeIt.hasNext())
		{
			RangeElement range = rangeIt.next();
			FieldType field = new FieldType();

			field.setIdentifier(range.getName());
			UnNamedDomainType domtype = new UnNamedDomainType();
                AnyValue anyVal = new AnyValue();
            domtype.setAnyValue(anyVal);
			DomainMetadataType dommeta = new DomainMetadataType();

			dommeta.setValue(range.getType());
			domtype.setDataType(dommeta);
			field.setDefinition(domtype);

			InterpolationMethods interp = new InterpolationMethods();

			InterpolationMethodType meth = new InterpolationMethodType();

			meth.setValue(cov.getInterpolationDefault());
			meth.setNullResistance(cov.getNullResistanceDefault());
			interp.setDefaultMethod(meth);

			Iterator<InterpolationMethod> interpIt = cov.getInterpolationMethodIterator();

			while (interpIt.hasNext())
			{
				InterpolationMethod wcpsInterp = interpIt.next();

				meth = new InterpolationMethodType();
				meth.setValue(wcpsInterp.getInterpolationType());
				meth.setNullResistance(wcpsInterp.getNullResistance());
				if ( (wcpsInterp.getInterpolationType()
						.equals(interp.getDefaultMethod().getValue()) == false) || (wcpsInterp
						.getNullResistance()
						.equals(interp.getDefaultMethod().getNullResistance()) == false) )
					interp.getOtherMethod().add(meth);
			}

			field.setInterpolationMethods(interp);
			wcsRange.getField().add(field);
		}
		desc.setRange(wcsRange);

		// Supported formats for GetCoverage: known rasdaman encoders
		String[] mimetypes = meta.getMimetypesList();

        for (int i = 0; i < mimetypes.length; i++)
        {
			String format = mimetypes[i];
			desc.getSupportedFormat().add(format);
		}

		// Available CRSs for current coverage
        desc.getSupportedCRS().add(DomainElement.IMAGE_CRS);
        if (cov.getCrs() != null)
    		desc.getSupportedCRS().add(DomainElement.WGS84_CRS);

		log("Done building the Coverage Description for coverage '" + name + "'.");

		return desc;
	}
}
