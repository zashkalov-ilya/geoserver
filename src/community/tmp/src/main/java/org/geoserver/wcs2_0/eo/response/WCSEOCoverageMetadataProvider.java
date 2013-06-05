package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wcs2_0.response.WCS20CoverageMetadataProvider;
import org.geoserver.wcs2_0.response.WCSTimeDimensionHelper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Encodes the minimal set of EO metadata for a given coverage:
 * 
 * <pre>
 *                 <wcseo:EOMetadata>
 *                     <eop:EarthObservation gml:id="someEOCoverage1_metadata">
 *                         <om:phenomenonTime>
 *                             <gml:TimePeriod gml:id="someEOCoverage1_tp">
 *                                 <gml:beginPosition>2008-03-13T10:00:06.000</gml:beginPosition>
 *                                 <gml:endPosition>2008-03-13T10:20:26.000</gml:endPosition>
 *                             </gml:TimePeriod>
 *                         </om:phenomenonTime>
 *                         <om:resultTime>
 *                             <gml:TimeInstant gml:id="someEOCoverage1_archivingdate">
 *                                 <gml:timePosition>2001-08-22T11:02:47.999</gml:timePosition>
 *                             </gml:TimeInstant>
 *                         </om:resultTime>
 *                         <om:procedure/>
 *                         <om:observedProperty/>
 *                         <om:featureOfInterest>
 *                             <eop:Footprint gml:id="someEOCoverage1_fp">
 *                                 <eop:multiExtentOf>
 *                                     <gml:MultiSurface gml:id="someEOCoverage1_ms" srsName="EPSG:4326">
 *                                         <gml:surfaceMembers>
 *                                             <gml:Polygon gml:id="someEOCoverage1_fppoly">
 *                                                 <gml:exterior>
 *                                                     <gml:LinearRing>
 *                                                         <gml:posList>43.516667 2.1025 43.381667 2.861667 42.862778 2.65 42.996389 1.896944 43.516667 2.1025</gml:posList>
 *                                                     </gml:LinearRing>
 *                                                 </gml:exterior>
 *                                             </gml:Polygon>
 *                                         </gml:surfaceMembers>
 *                                     </gml:MultiSurface>
 *                                 </eop:multiExtentOf>
 *                                 <eop:centerOf>
 *                                     <gml:Point gml:id="someEOCoverage1_p" srsName="EPSG:4326">
 *                                         <gml:pos>43.190833 2.374167</gml:pos>
 *                                     </gml:Point>
 *                                 </eop:centerOf>
 *                             </eop:Footprint>
 *                         </om:featureOfInterest>
 *                         <om:result/>
 *                         <eop:metaDataProperty>
 *                             <eop:EarthObservationMetaData>
 *                                 <eop:identifier>someEOCoverage1</eop:identifier>
 *                                 <eop:acquisitionType>NOMINAL</eop:acquisitionType>
 *                                 <eop:status>ARCHIVED</eop:status>
 *                             </eop:EarthObservationMetaData>
 *                         </eop:metaDataProperty>
 *                     </eop:EarthObservation>
 *                 </wcseo:EOMetadata>
 * </pre>
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class WCSEOCoverageMetadataProvider implements WCS20CoverageMetadataProvider {
    
    static final Logger LOGGER = Logging.getLogger(WCSEOCoverageMetadataProvider.class);

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[0];
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        namespaces.declarePrefix("eop", "http://www.opengis.net/eop/2.0");
        namespaces.declarePrefix("gml", "http://www.opengis.net/gml/3.2");
        namespaces.declarePrefix("wcseo", "http://www.opengis.net/wcseo/1.0");
        
//        "xmlns:gmlcov", "http://www.opengis.net/gmlcov/1.0", "xmlns:om",
//        "http://www.opengis.net/om/2.0", "xmlns:swe", "http://www.opengis.net/swe/2.0",
//        "xmlns:wcs", "http://www.opengis.net/wcs/2.0",  "xmlns:xlink",

    }

    @Override
    public void encode(Translator tx, Object context) throws IOException {
        if (!(context instanceof CoverageInfo)) {
            return;
        }

        CoverageInfo ci = (CoverageInfo) context;
        DimensionInfo time = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null) {
            LOGGER.log(Level.FINE, "We received a coverage info that has no " +
            		"associated time, cannot add EO metadata to it: "+ ci.prefixedName());
            return;
        }
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        String coverageId = NCNameResourceCodec.encode(ci);
        WCSTimeDimensionHelper timeHelper = new WCSTimeDimensionHelper(time, reader, coverageId);
        tx.start("wcseo:EOMetadata");
        tx.start("eop:EarthObservation", atts("gml:id", coverageId + "_metadata"));
        
        // phenomenon time
        tx.start("om:phenomenonTime");
        tx.start("gml:TimePeriod", atts("gml:id", coverageId + "_tp"));
        element(tx, "gml:beginPosition", timeHelper.getBeginPosition(), null);
        element(tx, "gml:endPosition", timeHelper.getEndPosition(), null);
        tx.end("gml:TimePeriod");
        tx.end("om:phenomenonTime");
        
        // resultTime
        tx.start("om:resultTime");
        tx.start("gml:TimeInstant", atts("gml:id", coverageId + "_rt"));
        element(tx, "gml:timePosition", timeHelper.getEndPosition(), null);
        tx.end("gml:TimeInstant");
        tx.end("om:resultTime");
        
        // some empty elements...
        element(tx, "om:procedure", null, null);
        element(tx, "om:observedProperty", null, null);
        
        tx.end("eop:EarthObservation");
        tx.end("wcseo:EOMetadata");

    }
    
    private void element(Translator tx, String element,
            String content, AttributesImpl attributes) {
        tx.start(element, attributes);
        if(content != null) {
            tx.chars(content);
        }
        tx.end(element);
    }
    
    Attributes atts(String... atts) {
        AttributesImpl attributes = new AttributesImpl();
        for (int i = 0; i < atts.length; i += 2) {
            attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
        }
        return attributes;
    }

}