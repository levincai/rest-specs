/**
 * Copyright (C) Commission Junction Inc.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class Util {

    public static String relativePath(File parent, File child){
        final String childPath = child.getAbsolutePath();
        final String parentPath = parent.getAbsolutePath();
        if(!childPath.startsWith(parentPath)){
            throw new RuntimeException(childPath + " is not a child of " + parentPath);
        }
        final String path = childPath.substring(parentPath.length());
        if(path.startsWith("/")){
            return path.substring(1);
        }else{
            return path;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T readObject(File path){
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
            try{
                return (T) in.readObject();
            }finally{
                in.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeObject(Object o, File path){
        try{
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
            try{
                out.writeObject(o);
            }finally{
                out.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void deleteDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File tempDir() {
        try {
            File path = File.createTempFile("tempdirectory", ".dir");
            delete(path);
            mkdirs(path);
            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(File path) {
        if(!path.delete()) throw new RuntimeException("Could not delete " + path.getAbsolutePath());
    }

    public static void mkdirs(File directory, String string) {
        File path = new File(directory, string);
        mkdirs(path);
    }

    public static void mkdirs(File path) {
        if(!path.exists() && !path.mkdirs()){
            throw new RuntimeException("Could not create directory: " + path.getAbsolutePath());
        }
    }

    /**
     * Convert a package name into a relative file system path.
     *
     * @param packageName name of package in dotted notation.
     *
     * @return file system path string.
     *
     * @throws NullPointerException if argument is null
     */
    public  static String packageToPath(String packageName) {
        return packageName.replace(".","/");
    }

    /**
     *
     * @param fromRoot
     * @return
     *
     * @deprecated use #findRestSpecFiles(Path)
     */
    @Deprecated
    public static Stream<File> findRestSpecFiles(File fromRoot) {
        final Iterator<File> fit =  FileUtils.iterateFiles(
                fromRoot, new WildcardFileFilter("*.spec.json"), TrueFileFilter.INSTANCE);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(fit, Spliterator.ORDERED), false);
    }


    public static Stream<Path> findRestSpecPaths(Path fromRoot) {
        final Iterator<File> fit =  FileUtils.iterateFiles(
                fromRoot.toFile(), new WildcardFileFilter("*.spec.json"), TrueFileFilter.INSTANCE);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(fit, Spliterator.ORDERED), false)
                .map(File::toPath);
    }


}
