/**
 * Copyright (C) 2011, 2012, 2013 Commission Junction Inc.
 *
 * This file is part of rest-specs.
 *
 * rest-specs is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * rest-specs is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rest-specs; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.cj.restspecs.mockrunner;

import cj.restspecs.core.RestSpec;
import cj.restspecs.core.io.StringLoader;
import org.codehaus.jackson.JsonParseException;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RestSpecServletValidatorTest {
    @Test
    public void validateSimpleTextResponseBodies() throws Exception {
        String simpleTextSpecJson = "{ \"url\": \"/echo\", \"request\": { \"method\": \"GET\" }, \"response\": { \"statusCode\": 200, \"header\": { \"Content-Type\": \"text/plain\" }, \"representation\": \"marco!\" } }";

        RestSpec restSpec = new RestSpec("simpleTextSpec", new StringLoader(simpleTextSpecJson));
        HttpServlet testSubject = new FakeHttpServlet("text/plain", "marco!");

        new RestSpecServletValidator().validate(restSpec, testSubject).assertNoViolations();
    }

    @Test
    public void validateNormalizesJSONResponseBodies() throws Exception {
        String simpleJsonSpecJson = "{ \"url\": \"/echo\", \"request\": { \"method\": \"GET\" }, \"response\": { \"statusCode\": 200, \"header\": { \"Content-Type\": \"application/json\" }, \"representation\": \"{ \\\"age\\\": 18 }\" } }";

        RestSpec restSpec = new RestSpec("simpleJsonSpec", new StringLoader(simpleJsonSpecJson));
        HttpServlet testSubject = new FakeHttpServlet("application/json", "{\n   \"age\": 18\n}");

        new RestSpecServletValidator().validate(restSpec, testSubject).assertNoViolations();
    }

    @Test
    public void validateNormalizesJSONArrayResponseBodies() throws Exception {
        String simpleJsonArraySpecJson = "{ \"url\": \"/echo\", \"request\": { \"method\": \"GET\" }, \"response\": { \"statusCode\": 200, \"header\": { \"Content-Type\": \"application/json\" }, \"representation\": \"[ { \\\"age\\\": 18 }, { \\\"age\\\": 19 } ]\" } }";

        RestSpec restSpec = new RestSpec("simpleJsonArraySpec", new StringLoader(simpleJsonArraySpecJson));
        HttpServlet testSubject = new FakeHttpServlet("application/json", "[ {\n   \"age\": 18\n},\n{\n   \"age\": 19\n} ]");

        new RestSpecServletValidator().validate(restSpec, testSubject).assertNoViolations();
    }

    @Test
    public void invalidJsonResponseBodyWillThrowAnExceptionCausingTheTestToError() {
        String simpleJsonSpecJson = "{ \"url\": \"/echo\", \"request\": { \"method\": \"GET\" }, \"response\": { \"statusCode\": 200, \"header\": { \"Content-Type\": \"application/json\" }, \"representation\": \"{}\" } }";

        RestSpec restSpec = new RestSpec("simpleJsonSpec", new StringLoader(simpleJsonSpecJson));
        HttpServlet testSubject = new FakeHttpServlet("application/json", "{\n   \"a");

        try {
            new RestSpecServletValidator().validate(restSpec, testSubject).assertNoViolations();
            fail("Should have thrown an exception");
        } catch (Exception error) {
            assertThat(error, instanceOf(RuntimeException.class));
            assertThat(error.getCause(), instanceOf(JsonParseException.class));
        }
    }

    @Test
    public void validateWillThrowExceptionIfThereIsNoRequestInRestSpec() throws Exception {
        String boogieSpecJson = "{ \"url\": \"/boogie\" }";
        RestSpec spec = new RestSpec("boogieSpecJson", new StringLoader(boogieSpecJson));
        HttpServlet testSubject = new FakeHttpServlet("text/plain", "oogie oogie oogie!");

        try {
            new RestSpecServletValidator().validate(spec, testSubject);
        } catch (RuntimeException cause) {
            assertThat(cause.getMessage(), equalTo("Spec is missing a 'request'"));
        }
    }

    @Test
    public void validateWillThrowExceptionIfThereIsNoRequestMethodInRestSpec() throws Exception {
        String scoobySpecJson = "{ \"url\": \"/scooby\", \"request\": { } }";
        RestSpec spec = new RestSpec("scoobySpecJson", new StringLoader(scoobySpecJson));
        HttpServlet testSubject = new FakeHttpServlet("text/plain", "woof!");

        try {
            new RestSpecServletValidator().validate(spec, testSubject);
        } catch (RuntimeException cause) {
            assertThat(cause.getMessage(), equalTo("Spec is missing a 'request.method'"));
        }
    }
}

class FakeHttpServlet extends HttpServlet {
    private final String contentType;
    private final String body;

    public FakeHttpServlet(String contentType, String body) {
        this.contentType = contentType;
        this.body = body;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType(contentType);

        ServletOutputStream output;
        output = resp.getOutputStream();
        output.print(body);
    }
}
