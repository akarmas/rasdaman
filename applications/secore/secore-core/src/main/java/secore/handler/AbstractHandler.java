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
 * Copyright 2003 - 2012 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package secore.handler;

import secore.req.ResolveResponse;
import secore.req.ResolveRequest;
import secore.db.DbManager;
import secore.util.SecoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import secore.req.RequestParam;
import secore.util.Constants;
import static secore.util.Constants.*;
import secore.util.ExceptionCode;
import secore.util.StringUtil;

/**
 * An abstract implementation of {@link Handler}, which provides some
 * convenience methods to concrete implementations.
 *
 * @author Dimitar Misev
 */
public abstract class AbstractHandler implements Handler {

  private static Logger log = LoggerFactory.getLogger(AbstractHandler.class);

  public boolean canHandle(ResolveRequest request) throws SecoreException {
    return getOperation().equals(request.getOperation());
  }

  /**
   * Returns the parent of element <code>el</code> which text content equals
   * <code>id</code>.
   *
   * @param url URL identifiers that should be resolved
   * @param versionNumber the version of collection to query
   * @param depth set the depth to which to dereference xlinks
   * @param parameters parameters to substitute for target elements
   * @return the definition (parent element of el)
   * @throws SecoreException usually if the text content of el hasn't been
   * matched with id
   */
  protected ResolveResponse resolveId(String url, String versionNumber, String depth, List<Parameter> parameters) throws SecoreException {
    String res = resolve(IDENTIFIER_LABEL, url, versionNumber, depth, parameters);
    ResolveResponse ret = new ResolveResponse(res);

    if (!ret.isValidDefinition()) {
      // no definition found
      if (url.contains(Constants.VERSION_NUMBER)) {
        url = url.replace(Constants.VERSION_NUMBER, versionNumber);
      }
      log.error("Failed resolving " + url);
      throw new SecoreException(ExceptionCode.NoSuchDefinition, "Failed resolving " + url);
    }

    return ret;
  }

  /**
   * Returns the parent of element <code>el</code> which text content equals
   * <code>id</code>.
   *
   * @param el element name
   * @param id text content to match
   * @param versionNumber the version number of collection to query (e.g: 8.5)
   * @param depth set the depth to which to dereference xlinks
   * @return the definition (parent element of el)
   * @throws SecoreException usually if the text content of el hasn't been
   * matched with id
   */
  protected String resolve(String el, String id, String versionNumber, String depth) throws SecoreException {
    return resolve(el, id, depth, versionNumber,  new ArrayList<Parameter>());
  }

  /**
   * Returns the parent of element <code>el</code> which text content equals
   * <code>id</code>.
   *
   * @param el element name
   * @param id text content to match
   * @param versionNumber the version of collection to query (e.g: 8.5)
   * @param depth set the depth to which to dereference xlinks
   * @param parameters parameters to substitute for target elements
   * @return the definition (parent element of el)
   * @throws SecoreException usually if the text content of el hasn't been
   * matched with id
   */
  protected String resolve(String el, String id, String versionNumber, String depth, List<Parameter> parameters) throws SecoreException {
    // NOTE: this will query in userdb first then later with the gml db.
    // e.g: gml_85 as database collection name
    String collectionName = "";
    if (versionNumber.equals("") || versionNumber.equals(DbManager.FIX_GML_VERSION_ALIAS)) {
      // if user request to version 0, then it is supposed in gml dictionary.
      collectionName = DbManager.createGMLCollectionName(DbManager.FIX_GML_VERSION_ALIAS);
    } else {
      
      // NOTE: if a definition from userdb (e.g: crs/AUTO/1.3/42001) then it don't have gml_13 collection name.
      // it will use the fixed version for gmldb (e.g: gml_85) to resolve.
      boolean collectionExistByVersionNumber = DbManager.collectionExistByVersionNumber(versionNumber);
      if (collectionExistByVersionNumber) {
        collectionName = DbManager.createGMLCollectionName(versionNumber);
      } else {
        collectionName = DbManager.FIX_GML_COLLECTION_NAME;
      }      
    }
    
    String work = null;

    if (!parameters.isEmpty()) {
      String targets = EMPTY;
      int i = 0;
      for (Parameter parameter : parameters) {
        if (!targets.equals(EMPTY)) {
          targets += COMMA + NEW_LINE;
        }
        targets += "     if (exists($tmp" + parameter.getTarget()
            + ")) then replace value of node $tmp" + parameter.getTarget()
            + " with '" + parameter.getValue() + "' else ()";
        if (i++ == 0) {
          targets += NEW_LINE;
        }
      }
      work
          = "	for $res in local:flatten(collection('" + COLLECTION_NAME + "'), $id, 0)\n"
          + " return\n"
          + "   copy $tmp := $res\n"
          + "   modify (\n"
          + targets
          + "   )\n"
          + "   return $tmp\n";
    } else {
      work
          = "	let $res := local:flatten(collection('" + COLLECTION_NAME + "'), $id, 0)\n"
          + "	return $res\n";
    }

    // remove host from id
    id = StringUtil.uriToPath(id);

    // construct query
    /* NOTE: This has 3 functions in XQuery:
     + getid($d is collection("userdb" or "gml"), $id is URI)
     It will try to find definition from xlink:href in "userdb" first.
     If it is not inside "userdb" then will try to resolve from "gml".
     $ret can returns a sequence() so need to use *[last()]* to get only 1 element

     + flatten($d is collection("userdb" or "gml"), $id is URI, $depth is a number) // this is recursive function
     Using xlink:href so one definition can import multiple other definition in side it.
     So need to traverse the XML Element Tree to the xlink:href to get other included definition.
     Depth-First Search.

     + work($id is URI) will return the full resolved definition.
     */

    // Rules to make resolved definition is valid to GML Schema
    // + xlink:href="" empty then must not be resolved
    // + Unresolvable URL should be put inside <remarks>empty_xlink:href="$URL"</remarks>
    // + Some element should not be resolved:
    List<String> unresolveElements = Arrays.asList("changeID", "descriptionReference", "projectionConversion", "sourceGeographicCRS",
        "geometryFile", "unitsSystem", "signReversal", "propertyRference", "changeRequest",
        "deprecatedObject", "replacedBy", "supersededObject", "supersededBy", "propertyReference",
        "sourceCoordDiffUom", "targetCoordDiffUom", "baseGeodeticCRS");

    // This string will store all the unresolveElements in where clause (node name does not contains any string in List)
    String whereNotContains = "where not (contains($elName, \"" + unresolveElements.get(0) + "\")) \n";
    for (int i = 1; i < unresolveElements.size(); i++) {
      whereNotContains += " and not (contains($elName, \"" + unresolveElements.get(i) + "\")) \n";
    }

    // And NOT resolve xlink:href="" (some definition has this element (/def/crs/EPSG/0/66006405 )
    whereNotContains += " and not ($hrefValue = \"\") \n";

    // XQUERY hint: if error if is not complete, just use  $c instead of return $c
    // if error cannot promote sequence() to element() then not execute function local:work as it only returns 1 element() (ONLY FOR DEBUGGING)
    // Execute local:flatten(collection('__COLLECTION__'), '/def/crs/EPSG/0/66006405', 0) 
    
    String query
        = "declare namespace gml = \"" + NAMESPACE_GML + "\";\n"
        + "declare namespace xlink = \"" + NAMESPACE_XLINK + "\";\n"
        + "declare function local:getid($d as document-node(), $id as xs:string) as element() {\n"
        + "let $retUserDB := $d//gml:" + el + "[fn:ends-with(text(), $id)]/..\n"
        + "let $retValue := \"\"\n"
        + "return if (empty($retUserDB)) then\n"
        + "             let $retGML := collection('" + collectionName + "')//gml:" + el + "[fn:ends-with(text(), $id)]/..\n"
        + "             return if (empty($retGML)) then\n"
        + "                       let $retValue := <empty/>\n"
        + "                       return $retValue"
        + "                    else"
        + "                        let $retValue := $retGML[last()]\n"
        + "                        return $retValue"
        + "       else\n"
        + "             let $retValue := $retUserDB[last()]\n"
        + "             return $retValue"
        + "};\n"
        + ""
        + "declare function local:flatten($d as document-node(), $id as xs:string, $depth as xs:integer) as element()* {\n"
        + "  copy $el := local:getid($d, $id)\n"
        + "  modify\n"
        + "  (\n"
        + "  for $c in $el//*[@xlink:href]\n"
        + "  let $elName := $c/name()\n"
        + "  let $hrefValue := $c/@xlink:href\n"
        + "  (: $nodeName will be created as a parent node of resolved definition which is referenced by xlink:href :)"
        + "  let $nodeName := node-name($c)"
        + whereNotContains
        + "  return if ($depth < " + depth + ") then\n"
        + "  	replace node $c with element {$nodeName}   {\n"
        + "             (  local:flatten($d, $c/@xlink:href, $depth + 1) ) \n"
        + "	  }  else replace node $c with $c\n"
        + "  )\n"
        + "  return $el\n"
        + "};\n"
        + ""
        + "declare function local:work($id as xs:string) as element() {\n"
        + work
        + "};\n"
        + "local:work('" + id + "')";

    return DbManager.getInstance().getDb().queryUser(query, versionNumber);
  }

  /**
   * Returns the value of the attribute with name "identifier" of the element
   * <code>el</code> which text content equals to <code>id</code>.
   *
   * @param el element name
   * @param id text content to match
   * @param versionNumber gml has multiple version, userdb is called a version also.
   * @return the data value of the attribute
   * @throws SecoreException usually if the text content of el hasn't been
   * matched with id
   */
  public String resolveAttribute(String el, String id, String versionNumber) throws SecoreException {    
    String query = "declare namespace gml = \"" + NAMESPACE_GML + "\";\n"
        + "let $d := collection('" + COLLECTION_NAME + "')\n"
        + "return data($d//gml:" + el + "[text() = '" + id + "']/../@identifier)";
    return DbManager.getInstance().getDb().queryBothDB(query, versionNumber);
  }

  public List<String> getComponentCRSs(ResolveRequest request, int componentNo) throws SecoreException {

    List<RequestParam> params = request.getParams();

    // component CRS URIs
    List<String> components = new ArrayList<String>();

    // get the component CRSs
    for (int i = 0; i < params.size(); i++) {
      String key = params.get(i).key;
      String val = params.get(i).val.toString();
      if (val != null) {
        try {
          int ind = Integer.parseInt(key);
          if (ind == components.size() + 1) {
            // the value is a CRS reference, e.g. 1=crs_ref
            checkCrsRef(val);
            components.add(val);
          } else {
            // error
            log.error("Invalid " + getOperation() + " request, expected number "
                + (components.size() + 1) + " but got " + ind);
            throw new SecoreException(ExceptionCode.InvalidParameterValue,
                "Invalid " + getOperation() + " request, expected number "
                + (components.size() + 1) + " but got " + ind);
          }
        } catch (NumberFormatException ex) {
          // this is a key=value pair that needs to be added to the last component
          int ind = components.size() - 1;
          if (ind < 0) {
            log.error("Invalid " + getOperation() + " request");
            throw new SecoreException(ExceptionCode.InvalidRequest,
                "Invalid " + getOperation() + " request");
          }
          // append to last component
          String component = components.get(ind);
          if (component.contains(QUERY_SEPARATOR)) {
            component += PAIR_SEPARATOR;
          } else {
            component += QUERY_SEPARATOR;
          }
          components.set(ind, component + key + KEY_VALUE_SEPARATOR + val);
        }
      }
    }

    // they both must be specified
    if (components.size() < componentNo) {
      log.error("Expected at least " + componentNo + " CRSs, got " + components.size());
      throw new SecoreException(ExceptionCode.MissingParameterValue,
          "Expected at least " + componentNo + " CRSs, got " + components.size());
    }

    return components;
  }

  private void checkCrsRef(String crsRef) throws SecoreException {
    if (!crsRef.contains(StringUtil.SERVLET_CONTEXT + "/crs")) {
      log.error("Invalid " + getOperation() + " request, expected a CRS reference, but got " + crsRef);
      throw new SecoreException(ExceptionCode.InvalidParameterValue,
          "Invalid " + getOperation() + " request, expected a CRS reference, but got " + crsRef);
    }
  }
}
