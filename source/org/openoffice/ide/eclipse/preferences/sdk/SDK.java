/*************************************************************************
 *
 * $RCSfile: SDK.java,v $
 *
 * $Revision: 1.2 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2005/07/26 06:23:59 $
 *
 * The Contents of this file are made available subject to the terms of
 * either of the following licenses
 *
 *     - GNU Lesser General Public License Version 2.1
 *     - Sun Industry Standards Source License Version 1.1
 *
 * Sun Microsystems Inc., October, 2000
 *
 *
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * Copyright 2000 by Sun Microsystems, Inc.
 * 901 San Antonio Road, Palo Alto, CA 94303, USA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 *
 *
 * Sun Industry Standards Source License Version 1.1
 * =================================================
 * The contents of this file are subject to the Sun Industry Standards
 * Source License Version 1.1 (the "License"); You may not use this file
 * except in compliance with the License. You may obtain a copy of the
 * License at http://www.openoffice.org/license.html.
 *
 * Software provided under this License is provided on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES THAT THE SOFTWARE IS FREE OF DEFECTS,
 * MERCHANTABLE, FIT FOR A PARTICULAR PURPOSE, OR NON-INFRINGING.
 * See the License for the specific provisions governing your rights and
 * obligations concerning the Software.
 *
 * The Initial Developer of the Original Code is: Sun Microsystems, Inc..
 *
 * Copyright: 2002 by Sun Microsystems, Inc.
 *
 * All Rights Reserved.
 *
 * Contributor(s): Cedric Bosdonnat
 *
 *
 ************************************************************************/
package org.openoffice.ide.eclipse.preferences.sdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.Path;
import org.openoffice.ide.eclipse.OOEclipsePlugin;
import org.openoffice.ide.eclipse.i18n.I18nConstants;

/**
 * SDK Class used as model for a line of the table. This class doesn't take the new URE
 * possibilities into account. It will certainly need some additionnal members to support
 * UNO SDK other than the OpenOffice.org one. 
 * 
 * @author cbosdonnat
 *
 */
public class SDK {
	
	/**
	 * private constant that holds the sdk name key in the dk.mk file
	 */
	private static final String  K_SDK_NAME = "SDKNAME";
	
	/**
	 * private constant that holds the sdk name key in the dk.mk file.
	 * Used for OpenOffice.org SDK 1.1 compatibility
	 */
	private static final String K_DK_NAME = "DKNAME";
	
	/**
	 * private constant that holds the sdk build id key in the dk.mk file
	 */
	private static final String K_SDK_BUILDID = "BUILDID";
	
	/**
	 * private constant that hold the name of the sdk config file (normaly dk.mk)
	 * This is set to easily change if there are future sdk organization changes
	 */
	private static final String F_DK_CONFIG = "dk.mk";

	
	
	/* SDK Members */
	
	private String name;
	private String buildId;
	private String sdkHome;
	private String oooHome;
	
	/**
	 * Standard and only constructor for the SDK object. The name and buildId will be fetched
	 * from the $(SDK_HOME)/settings/dk.mk properties file.
	 * 
	 * @param sdkHome absolute path of the SDK root
	 * @param oooHome absolute path to the $OOO_HOME directory
	 */
	public SDK (String sdkHome, String oooHome) throws InvalidSDKException {
		
		// Sets the path to the SDK
		setSDKHome(sdkHome);
		setOOoHome(oooHome);
	}

	/**
	 * Set the new SDK Home after having checked for the existence of the idl and settings directory.
	 * Fetches the sdk name and buildid from the dk.mk file
	 * 
	 * @param sdkHome path to the new sdk home
	 * 
	 * @exception InvalidSDKException <p>This exception is thrown when the following errors are encountered
	 *            with the INVALID_SDK_HOME error code: </p>
	 *             <ul>
	 *                <li>the sdk path does not point to a valid directory</li>
	 *                <li>the $(SDK_HOME)/idl directory doesnt exist</li>
	 *                <li>the $(SDK_HOME)/settings directory doesnt exist</li>
	 *                <li>the sdk name and buildid cannot be fetched</li>
	 *                <li>an unexpected exception has been raised</li>
	 *             </ul>
	 */
	public void setSDKHome(String sdkHome) throws InvalidSDKException {
		try {
		
			// Get the file representing the given sdkHome
			Path homePath = new Path(sdkHome);
			File homeFile = homePath.toFile();
			
			// First check the existence of this directory
			if (homeFile.exists() && homeFile.isDirectory()){
				
				/**
				 * <p>If the provided sdk home does not contains <li><code>idl</code></li>
				 * <li><code>settings</code></li> directories, the provided sdk is considered as invalid</p>
				 */
				
				// test for the idl directory
				File idlFile = new File(homeFile, "idl");
				if (! (idlFile.exists() && idlFile.isDirectory()) ) {
					throw new InvalidSDKException(
							OOEclipsePlugin.getTranslationString(I18nConstants.IDL_DIR_MISSING), 
							InvalidSDKException.INVALID_SDK_HOME);
				}
				
				// test for the settings directory
				File settingsFile = new File(homeFile, "settings");
				if (! (settingsFile.exists() && settingsFile.isDirectory()) ) {
					throw new InvalidSDKException(
							OOEclipsePlugin.getTranslationString(I18nConstants.SETTINGS_DIR_MISSING),
							InvalidSDKException.INVALID_SDK_HOME);
				}
				
				
				// If the settings and idl directory both exists, then try to fetch the name and buildId from
				// the settings/dk.mk properties file
				readSettings(settingsFile);
				this.sdkHome = sdkHome;
				
			} else {
				throw new InvalidSDKException(
						OOEclipsePlugin.getTranslationString(I18nConstants.NOT_EXISTING_DIR)+sdkHome,
						InvalidSDKException.INVALID_SDK_HOME);
			}
		} catch (Throwable e) {
			
			if (e instanceof InvalidSDKException) {
				
				// Rethrow the InvalidSDKException
				InvalidSDKException exception = (InvalidSDKException)e;
				throw exception;
			} else {
				
				// Unexpected exception thrown
				throw new InvalidSDKException(
						OOEclipsePlugin.getTranslationString(I18nConstants.UNEXPECTED_EXCEPTION), 
						InvalidSDKException.INVALID_SDK_HOME, e);
			}
		}
	}
	
	/**
	 * 
	 * @param oooHome
	 * @throws InvalidSDKException
	 */
	public void setOOoHome(String oooHome) throws InvalidSDKException {
		
		try {
			// Get the file representing the given OOo home path
			Path homePath = new Path(oooHome);
			File homeFile = homePath.toFile();
			
			
				
			// Check for the program directory
			File programFile = new File(homeFile, "program");
			if (programFile.exists() && programFile.isDirectory()){
				
				// checks for types.rdb
				File typesFile = new File(programFile, "types.rdb");
				if (! (typesFile.exists() && typesFile.isFile()) ){
					throw new InvalidSDKException(
							OOEclipsePlugin.getTranslationString(I18nConstants.NOT_EXISTING_FILE)+ typesFile.getAbsolutePath(), 
							InvalidSDKException.INVALID_OOO_HOME);
				}
				
				// checks for classes directory
				File classesFile = new File (programFile, "classes");
				if (! (classesFile.exists() && classesFile.isDirectory()) ){
					throw new InvalidSDKException(
							OOEclipsePlugin.getTranslationString(I18nConstants.NOT_EXISTING_DIR) + classesFile.getAbsolutePath(), 
							InvalidSDKException.INVALID_OOO_HOME);
				}
				
				this.oooHome = oooHome;
				
			} else {
				throw new InvalidSDKException(
						OOEclipsePlugin.getTranslationString(I18nConstants.NOT_EXISTING_DIR) + programFile.getAbsolutePath(), 
						InvalidSDKException.INVALID_OOO_HOME);
			}
				
			
			
		} catch (Throwable e){
			
			if (e instanceof InvalidSDKException) {
				
				// Rethrow the invalidSDKException
				InvalidSDKException exception = (InvalidSDKException)e;
				throw exception;
			} else {

				// Unexpected exception thrown
				throw new InvalidSDKException(
						OOEclipsePlugin.getTranslationString(I18nConstants.UNEXPECTED_EXCEPTION),
						InvalidSDKException.INVALID_OOO_HOME, e);
			}
		}
	}
	
	/**
	 * Returns the SDK name as set in the settings/mkdk file
	 * 
	 * @return sdk name
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Build a unique id from the sdk name and build id
	 *
	 * @return the sdk unique id
	 */
	public String getId(){
		return getId(name, buildId);
	}
	
	/**
	 * Creates a sdk unique id with a name and build id
	 * 
	 * @param name sdk name
	 * @param buildId sdk build id
	 * 
	 * @return sdk unique identifier
	 */
	public static String getId(String name, String buildId){
		return name+"-"+buildId;
	}
	
	/**
	 * Returns an array containing the name and build id composing the sdk unique id 
	 * 
	 * @param sdkKey sdk unique id to decompose
	 * @return name and build if
	 */
	public static String[] getSdkNameBuildId(String sdkKey) {
		
		return sdkKey.split("-");
	}
	
	/**
	 * Returns the SDK build id without the parenthesized string. For example, if the
	 * full build id is <code>680m92(Build:8896)</code>, the result will be: <code>680m92</code>.
	 * 
	 * If the builid is <code>null</code>, the return will be 
	 * 
	 * @return the shortened build id
	 */
	public String getBuildId(){
		String result = null;
		
		String[] splits = buildId.split("\\(.*\\)");
		if (splits.length > 0){
			result = splits[0];
		}
		
		return result;
	}
	
	/**
	 * Returns the full build id with the parenthesized string. For example, if the
	 * full build id is <code>680m92(Build:8896)</code>, the result will be same string.
	 * 
	 * @return the full build id of the sdk
	 */
	public String getFullBuildId(){
		return buildId;
	}
	
	/**
	 * Returns the SDK home directory. This string could be passed to the
	 * Path constructor to get the folder object. 
	 * 
	 * @return SDK home directory
	 * @see Path
	 */
	public String getSDKHome(){
		return sdkHome;
	}
	
	/**
	 * Returns the path to the associated OpenOffice.org home directory. This string could 
	 * be passed to the Path constructor to get the folder object. 
	 * 
	 * @return path to the SDK associated OpenOffice.org home directory.
	 * @see Path
	 */
	public String getOOoHome(){
		return oooHome;
	}
	
	/**
	 * <p>Returns the path to the associated OpenOffice.org classes directory. This string could 
	 * be passed to the Path constructor to get the folder object.</p> 
	 * 
	 * <p><em>This method should be used for future compatibility with URE applications</em></p>
	 * 
	 * @return path to the associated OpenOffice.org classes directory
	 */
	public String getClassesPath(){
		return oooHome + "/program/classes";
	}
	
	/**
	 * Reads the dk.mk file to get the sdk name and buildid. They are set in the SDK object if 
	 * they are both fetched. Otherwise an invalide sdk exception is thrown.
	 * 
	 * @param settingsFile
	 * @throws InvalidSDKException Exception thrown when one of the following problems happened
	 *          <ul>
	 *             <li>the given settings file isn't a valid directory</li>
	 *             <li>the settings/dk.mk file doesn't exists or is unreadable</li>
	 *             <li>one or both of the sdk name or buildid key is not set</li>
	 *          </ul>
	 */
	private void readSettings(File settingsFile) throws InvalidSDKException {
		
		if (settingsFile.exists() && settingsFile.isDirectory()) {
		
			// Get the dk.mk file
			File dkFile = new File(settingsFile, F_DK_CONFIG);
			
			Properties dkProperties = new Properties();
			try {
				dkProperties.load(new FileInputStream(dkFile));
				
				// Checks if the name and buildid properties are set
				if (dkProperties.containsKey(K_SDK_NAME) && dkProperties.containsKey(K_SDK_BUILDID)){
					
					// Sets the both value
					name = dkProperties.getProperty(K_SDK_NAME);
					buildId = dkProperties.getProperty(K_SDK_BUILDID);
				} else if (dkProperties.containsKey(K_DK_NAME) && dkProperties.containsKey(K_SDK_BUILDID)){
					
					// Sets the both value
					name = dkProperties.getProperty(K_DK_NAME);
					buildId = dkProperties.getProperty(K_SDK_BUILDID);
				} else {
					throw new InvalidSDKException(
							OOEclipsePlugin.getTranslationString(I18nConstants.KEYS_NOT_SET) + K_SDK_NAME + ", " + K_SDK_BUILDID,
							InvalidSDKException.INVALID_SDK_HOME);
				}
				
			} catch (FileNotFoundException e) {
				throw new InvalidSDKException(
						OOEclipsePlugin.getTranslationString(I18nConstants.NOT_EXISTING_FILE)+ "settings/" + F_DK_CONFIG , 
						InvalidSDKException.INVALID_SDK_HOME);
			} catch (IOException e) {
				throw new InvalidSDKException(
						OOEclipsePlugin.getTranslationString(I18nConstants.NOT_READABLE_FILE) + "settings/" + F_DK_CONFIG, 
						InvalidSDKException.INVALID_SDK_HOME);
			}
			
		} else {
			throw new InvalidSDKException(
					OOEclipsePlugin.getTranslationString(I18nConstants.NOT_EXISTING_DIR)+ settingsFile.getAbsolutePath(),
					InvalidSDKException.INVALID_SDK_HOME);
		}
	}
}
