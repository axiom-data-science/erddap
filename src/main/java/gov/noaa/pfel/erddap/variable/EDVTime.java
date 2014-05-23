/* 
 * EDVTime Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.util.GregorianCalendar;

/** 
 * This class holds information about *the* main time variable,
 * which is like EDVTimeStamp, but has destinationName="time".
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDVTime extends EDVTimeStamp { 

     /** The constructor. */
    public EDVTime(String tSourceName, 
        Attributes tSourceAttributes, Attributes tAddAttributes,
        String tSourceDataType) 
        throws Throwable {

        super(tSourceName, EDV.TIME_NAME, tSourceAttributes, tAddAttributes,
            tSourceDataType); 
    }
        
}
