/*
 Copyright (c) 2012 Centre National de la Recherche Scientifique (CNRS).

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package eu.stratuslab.cimi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import org.joda.time.DateTime;

import eu.stratuslab.cimi.ParseException;
import eu.stratuslab.cimi.SimpleNode;

public class CIMIFilterParserTest {
    
    private CIMIFilterParser parser(String s) {
        return new CIMIFilterParser(new StringReader(s));
    }
    
    @Test
    public void testValidFilters() throws ParseException {
        
        String[] validFilters = {"alpha=3",
                                 "3=alpha",
                                 "alpha=3 and beta=4",
                                 "3=alpha and 4=beta",
                                 "(alpha=3)",
                                 "(3=alpha)",
                                 "property['beta']=4",
                                 "4=property['beta']",
                                 "alpha=3 and beta=4",
                                 "alpha=3 or beta=4",
                                 "alpha=3 and beta=4 or gamma=5 and delta=6"};
        
        for (String filter : validFilters) {
            System.out.println(filter);
            SimpleNode n = parser(filter).filter();
            n.dump("> ");
        }
    }

    @Test
    public void testValidIntegers() throws ParseException {

        String[] validIntegers = {"0", "00", "01", "1", "123", "9999"};
        
        for (String s : validIntegers) {
            Integer correct = Integer.valueOf(s);
            Integer i = parser(s).intValue();
            assertEquals(correct, i);
        }
    }

    @Test
    public void testInvalidIntegers() throws ParseException {

        String[] invalidIntegers = {"", "abc"};
        
        for (String s : invalidIntegers) {
            try {
                parser(s).intValue();
                fail("invalid integer did not throw an exception: " + s);
            } catch (ParseException e) {
                // OK
            }
        }
    }

    @Test
    public void testValidBooleans() throws ParseException {

        String[] validBooleans = {"true", "false"};
        
        for (String s : validBooleans) {
            Boolean correct = Boolean.valueOf(s);
            Boolean b = parser(s).boolValue();
            assertEquals(correct, b);
        }
    }

    @Test
    public void testStrings() throws ParseException {

        Map<String, String> cases = new HashMap<String, String>();
        cases.put("'alpha'", "alpha");
        cases.put("\"alpha\"", "alpha");

        cases.put("'al\\\\pha'", "al\\pha");
        cases.put("\"al\\\\pha\"", "al\\pha");
        cases.put("'al\\'pha'", "al'pha");
        cases.put("\"al\\\"pha\"", "al\"pha");

        cases.put("'\\\\pha'", "\\pha");
        cases.put("\"\\\\pha\"", "\\pha");
        cases.put("'\\'pha'", "'pha");
        cases.put("\"\\\"pha\"", "\"pha");
        
        cases.put("'al\\\\'", "al\\");
        cases.put("\"al\\\\\"", "al\\");
        cases.put("'al\\''", "al'");
        cases.put("\"al\\\"\"", "al\"");

        for (Map.Entry<String, String> entry : cases.entrySet()) {
            String correct = entry.getValue();
            String s = parser(entry.getKey()).stringValue();
            assertEquals(correct, s);
        }
    }

    @Test
    public void testValidDates() throws ParseException {

        String[] validDates = {"2012-01-02",
                               "2012-01-02T13:14:25.6Z",
                               "2012-01-02T13:14:25.6-01:15",
                               "2012-01-02T13:14:25.6+02:30"};
        
        for (String s : validDates) {
            DateTime correct = DateTime.parse(s);
            DateTime i = parser(s).dateValue();
            assertEquals(correct, i);
        }
    }

    @Test
    public void testInvalidDates() throws ParseException {

        String[] invalidDates = {"2012",
                                 "2012-01-99T13:14:25.6Z",
                                 "2012-01-99T13:14:25.6Q",
                                 "2012-01:02T25:14:25.6-01:15",
                                 "2012-01-02T13:14:25.6+02-30"};
        
        for (String s : invalidDates) {
            try {
                parser(s).dateValue();
                fail("invalid date did not throw an exception: " + s);
            } catch (ParseException e) {
                // OK
            }
        }
    }


}
