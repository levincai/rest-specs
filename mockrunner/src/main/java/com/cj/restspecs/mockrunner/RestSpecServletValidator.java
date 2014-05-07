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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.apache.commons.io.FileUtils;

import cj.restspecs.core.RestSpec;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import org.codehaus.jackson.map.ObjectMapper;

import static cj.restspecs.core.RestSpec.*;

public class RestSpecServletValidator {

    public static class ValidationResult {
        public final List<Violation> violations;

        public ValidationResult(List<Violation> violations) {
            this.violations = Collections.unmodifiableList(violations);
        }

        public void assertNoViolations(){
            if(violations.size()>0){
                StringBuffer text = new StringBuffer();
                for(Violation violation : violations){
                    text.append(violation.description + "\n");
                }
                throw new RuntimeException(text.toString());
            }
        }
    }

    public static class Violation {
        public final String description;

        public Violation(String description) {
            super();
            this.description = description;
        }
    }
    
    private String stripLeadingQuestionMark(String query){
        if(query!=null && query.startsWith("?")){
            return query.substring(1);
        }else{
            return query;
        }
    }

    public ValidationResult validate(RestSpec rs, HttpServlet testSubject) throws Exception {

        //given
        MockHttpServletRequest req = buildRequestFromRestSpec(rs);
        MockHttpServletResponse res = new MockHttpServletResponseWithNiceExceptionsForUnsupportedFunctionality();

        //when
        testSubject.service(req, res);

        //then
        return validateResponseAgainstRestSpec(rs, res);
    }

    private MockHttpServletRequest buildRequestFromRestSpec(RestSpec restSpec) {
        MockHttpServletRequest request;
        request = new MockHttpServletRequest();

        for(String name: restSpec.request().header().fieldNames()){
            for(String value : restSpec.request().header().fieldsNamed(name)){
                request.setHeader(name, value);
            }
        }

        request.setRequestURI(restSpec.pathMinusQueryStringAndFragment());
        request.setPathInfo(restSpec.pathMinusQueryStringAndFragment());
        request.setQueryString(stripLeadingQuestionMark(restSpec.queryString()));

        RestSpec.QueryParameters queryParameters = restSpec.queryParameters();
        for (String name : queryParameters.names()) {
            request.setupAddParameter(name, queryParameters.values(name).toArray(new String[]{}));
        }

        request.setMethod(restSpec.request().method());

        return request;
    }

    private ValidationResult validateResponseAgainstRestSpec(RestSpec restSpec, MockHttpServletResponse response) {
        List<Violation> violations;
        violations = new ArrayList<Violation>();

        if (restSpec.response().statusCode() != response.getStatusCode()) {
            violations.add(new Violation("Status code should have been " + restSpec.response().statusCode() + " but was " + response.getStatusCode()));
        }

        if (restSpec.response().representation() != null) {
            violations.addAll(validateContentType(restSpec, response));
            violations.addAll(validateResponseBody(restSpec, response));
        }

        return new ValidationResult(violations);
    }

    private List<Violation> validateContentType(RestSpec restSpec, MockHttpServletResponse response) {
        List<Violation> violations;

        final String expectedContentType = restSpec.response().representation().contentType();
        final String actualContentType = response.getHeader("Content-Type");
        if (!expectedContentType.equals(actualContentType)) {
            violations = Collections.singletonList(new Violation("Content type of the response should have been  " + expectedContentType + " but was " + actualContentType));
        } else {
            violations = Collections.emptyList();
        }

        return violations;
    }

    private List<Violation> validateResponseBody(RestSpec restSpec, MockHttpServletResponse response) {
        List<Violation> violations;

        String expectedRepresentation;
        String actualRepresentation;

        if (isJsonContent(restSpec)) {
            expectedRepresentation = normalize(restSpec.response().representation().asText());
            actualRepresentation = normalize(response.getOutputStreamContent());
        } else {
            expectedRepresentation = restSpec.response().representation().asText();
            actualRepresentation = response.getOutputStreamContent();
        }

        boolean representationsAreEquivalent = expectedRepresentation.equals(actualRepresentation);
        if (!representationsAreEquivalent) {
            violations = Collections.singletonList(new Violation("The response representation should have been " + expectedRepresentation + " but was " + actualRepresentation));
        } else {
            violations = Collections.emptyList();
        }

        return violations;
    }

    private boolean isJsonContent(RestSpec restSpec) {
        String contentType = restSpec.response().representation().contentType();
        boolean isJsonContent = contentType.contains("/json");
        return isJsonContent;
    }

    private String normalize(String inputJson) {
        try {
            String outputJson;

            ObjectMapper mapper = new ObjectMapper();
            Object mappedInput = mapper.readValue(inputJson, Object.class);

            outputJson = mapper.writeValueAsString(mappedInput);
            return outputJson;
        } catch (Exception error) {
            throw new RuntimeException("Failed to normalize JSON: " + inputJson, error);
        }
    }
}

class MockHttpServletResponseWithNiceExceptionsForUnsupportedFunctionality extends MockHttpServletResponse {
    @Override
    public PrintWriter getWriter() throws IOException {
        throw new UnsupportedOperationException("getWriter not implemented by MockRunner--sorry!");
    }
}
