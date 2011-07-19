/*
 * YUI Test Coverage
 * Author: Nicholas C. Zakas <nzakas@yahoo-inc.com>
 * Copyright (c) 2009, Yahoo! Inc. All rights reserved.
 * Code licensed under the BSD License:
 *     http://developer.yahoo.net/yui/license.txt
 */
package com.yahoo.platform.yuitest.coverage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.RecognitionException;

import org.apache.commons.io.FileUtils;


/**
 * Encapsulates instrumenting all files in a inputDir.
 * @author Nicholas C. Zakas
 */
public class DirectoryInstrumenter {

    private static boolean verbose = false;

    public static boolean isVerbose() {
        return verbose;
    }

    public static void setVerbose(boolean verbose) {
        DirectoryInstrumenter.verbose = verbose;
    }

    public static void instrument(String inputDir, String outputDir, HashSet<String> excludes) 
            throws FileNotFoundException, UnsupportedEncodingException,
            IOException, RecognitionException {

        //normalize
        if (!inputDir.endsWith(File.separator)){
            inputDir = inputDir + File.separator;
        }
        if (!outputDir.endsWith(File.separator)){
            outputDir = outputDir + File.separator;
        }

        List<String> filenames = getFilenames(inputDir, excludes);

        for (int i=0; i < filenames.size(); i++){
            String inputFilename = filenames.get(i);
            String outputFilename = outputDir + inputFilename.substring(inputFilename.indexOf(inputDir) + inputDir.length());
            
            //create the directories if necessary
            File dir = new File(outputFilename.substring(0, outputFilename.lastIndexOf(File.separator)));
            if (!dir.exists()){

                if (verbose){
                    System.err.println("[INFO] Creating directory " + dir.getPath());
                }
                
                dir.mkdirs();
            }
            
            FileInstrumenter.setVerbose(verbose);
            FileInstrumenter.instrument(inputFilename, outputFilename);
        }
        
        //copy files and directories excluded from instrumentation, still need them to run tests
        copyExcludes(inputDir, outputDir, excludes);
    }

    /**
     * Copy files and directories excluded for instrumenation to destination directory.
     * @param inputDir String The source directory containing the excluded file or directory
     * @param outputDir String The destination directory
     * @param excludes HashSet A HashSet containing the file and directory paths to exclude from instrumentation.
     * 		These paths are in the form of inputDir+filename/dirname.
     * @throws IOException various IO errors including null or invalid source, destination, etc.
     */
    private static void copyExcludes(String inputDir, String outputDir, HashSet<String> excludes) throws IOException {
    	//copy files and directories excluded for instrumentation, need them to run tests properly
    	String name = null;
    	String destName = null;
    	File f = null;
    	
    	System.out.println("intput dir=" + inputDir);
    	
        for (Iterator<String> it = excludes.iterator(); it.hasNext(); ) {
        	name = it.next();
        	destName = outputDir + name.substring(inputDir.length());
        	f = new File(name);
        	if (f.isFile()) {
        		System.out.println("copying skipped file " + name + " to " + destName);
        		FileUtils.copyFile(f, new File(destName));
        	} else if (f.isDirectory()) {
        		System.out.println("copying skipped directory " + name + " to " + destName);
        		FileUtils.copyDirectory(f, new File(destName));
        	}
        }
    }

    /**
     * Retrieves a recursive list of all JavaScript files in the inputDir.
     * @param inputDir The inputDir to search.
     * @return List of all JavaScript files in the inputDir and subdirectories.
     * @throws IllegalArgumentException When the inputDir cannot be read.
     * @throws FileNotFoundException When the inputDir doesn't exist.
     */
    private static List<String> getFilenames(String directory, HashSet<String> excludes) throws IllegalArgumentException, FileNotFoundException {
    	
    	File dir = new File(directory);

        //validate the inputDir first
        if (!dir.exists()){
        	throw new FileNotFoundException("'" + directory + "' does not exist.");
        }
        if (!dir.isDirectory()){
            throw new FileNotFoundException("'" + directory + "' is not a valid directory.");
        }
        if (!dir.canRead()){
            throw new IllegalArgumentException("'" + directory + "' cannot be read.");
        }

        List<String> filenames = new LinkedList<String>();
        
        //TODO: Gotta be a better way...
        File[] files = dir.listFiles(new FileDirFilter(excludes));
        for (int i=0; i < files.length; i++){
            if (files[i].isFile() && files[i].getName().endsWith(".js")){
                filenames.add(files[i].getPath());
            } else if (files[i].isDirectory()){
                filenames.addAll(getFilenames(files[i].getPath(), excludes));
            }
        }

        return filenames;

    }

}
