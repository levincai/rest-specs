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
package com.cj.restspecs.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import cj.restspecs.core.RestSpec;
import cj.restspecs.core.RestSpecValidator;
import cj.restspecs.core.RestSpecValidator.Path;
import cj.restspecs.core.io.FilesystemLoader;
import cj.restspecs.core.io.Loader;
import cj.restspecs.core.model.Representation;

/**
 * @goal fingerprint
 * @phase compile
 */
public class FingerprintMojo extends AbstractRestSpecMojo {
    private static final String encoding = "UTF8";
    
    /** 
     * @parameter expression="${project.build.outputDirectory}/${project.groupId}.${project.artifactId}.rest-spec-fingerprint" 
     * @required 
     */ 
    protected File destinationFile; 
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            
            for(final File dir: findSourceDirectories()){
                final Loader loader = new FilesystemLoader(dir);

                for(Path next : RestSpecValidator.scan(dir).specDotJsFiles){
                    File f = new File(dir, next.toString());
                    getLog().info("Adding to fingerprint: " + next);
                    String headerLine = "\nFILE:" + next.toString() + "\n";
                    digest.update(headerLine.getBytes(encoding));
                    digest.update(FileUtils.readFileToByteArray(f));
                    
                    RestSpec spec = new RestSpec(next.toString(), loader);
                    
                    updateDigest(spec.request().representation(), digest);
                    updateDigest(spec.response().representation(), digest);
                }
            }
            
            final String expected = Hex.encodeHexString(digest.digest());
            
            FileUtils.write(destinationFile, expected);
        } catch (Exception e) {
            throw new MojoFailureException("Error generating fingerprint", e);
        }
    }
    
    private void updateDigest(Representation r, MessageDigest digest){
        if(r!=null){
            updateDigest(r.data(), digest);
        }
    }

    private void updateDigest(InputStream data, MessageDigest digest) {
        try {
            final byte[] buffer = new byte[1024 * 100];
            for(int n = data.read(buffer);n!=-1;n = data.read(buffer)){
                digest.update(buffer, 0, n);
            }
            data.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
}