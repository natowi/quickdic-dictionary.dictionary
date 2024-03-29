/**
*******************************************************************************
* Copyright (C) 2006-2013, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/

package com.ibm.icu.charset;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.charset.spi.CharsetProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.icu.impl.InvalidFormatException;


/**
 * A concrete subclass of CharsetProvider for loading and providing charset converters
 * in ICU.
 * @stable ICU 3.6
 */
public final class CharsetProviderICU extends CharsetProvider{
    private String optionsString;
    
    /**
     * Default constructor 
     * @stable ICU 3.6
     */
    public CharsetProviderICU() {
        optionsString = null;
    }

    /**
     * Constructs a charset for the given charset name. 
     * Implements the abstract method of super class.
     * @param charsetName charset name
     * @return charset objet for the given charset name, null if unsupported
     * @stable ICU 3.6
     */
    public final Charset charsetForName(String charsetName){
        try{
            // extract the options from the charset name
            charsetName = processOptions(charsetName);
            // get the canonical name
            String icuCanonicalName = getICUCanonicalName(charsetName);      
    
                // create the converter object and return it
            if(icuCanonicalName==null || icuCanonicalName.length()==0){
                // Try the original name, may be something added and not in the alias table. 
                // Will get an unsupported encoding exception if it doesn't work.
                return getCharset(charsetName);
            }
            return getCharset(icuCanonicalName);
        }catch(UnsupportedCharsetException ex){
        }catch(IOException ex){
        }
        return null;
    }
    
    /**
     * Constructs a charset for the given ICU conversion table from the specified class path.
     * Example use: <code>cnv = CharsetProviderICU.charsetForName("myConverter", "com/myCompany/myDataPackage");</code>.
     * In this example myConverter.cnv would exist in the com/myCompany/myDataPackage Java package.
     * Conversion tables can be made with ICU4C's makeconv tool.
     * This function allows you to allows you to load user defined conversion
     * tables that are outside of ICU's core data.
     * @param charsetName The name of the charset conversion table.
     * @param classPath The class path that contain the conversion table.
     * @return charset object for the given charset name, null if unsupported
     * @stable ICU 3.8
     */
    public final Charset charsetForName(String charsetName, String classPath) {
        return charsetForName(charsetName, classPath, null);
    }
    
    /**
     * Constructs a charset for the given ICU conversion table from the specified class path.
     * This function is similar to {@link #charsetForName(String, String)}.
     * @param charsetName The name of the charset conversion table.
     * @param classPath The class path that contain the conversion table.
     * @param loader the class object from which to load the charset conversion table
     * @return charset object for the given charset name, null if unsupported
     * @stable ICU 3.8
     */
    public Charset charsetForName(String charsetName, String classPath, ClassLoader loader) {
        CharsetMBCS cs = null;
        try {
             cs = new CharsetMBCS(charsetName, charsetName, new String[0], classPath, loader);
        } catch (InvalidFormatException e) {
            // return null;
        }
        return cs;
    }
    
    /**
     * Gets the canonical name of the converter as defined by Java
     * @param enc converter name
     * @return canonical name of the converter
     * @internal
     * @deprecated This API is ICU internal only.
     */
     public static final String getICUCanonicalName(String enc)
                                throws UnsupportedCharsetException{
        String canonicalName = null;
        String ret = null;
        try{
            if(enc!=null){
                 if((canonicalName = UConverterAlias.getCanonicalName(enc, "MIME"))!=null){
                    ret = canonicalName;
                } else if((canonicalName = UConverterAlias.getCanonicalName(enc, "IANA"))!=null){
                    ret = canonicalName;
                } else if((canonicalName = UConverterAlias.getAlias(enc, 0))!=null){
                    /* we have some aliases in the form x-blah .. match those */
                    ret = canonicalName;
                }/*else if((canonicalName = UConverterAlias.getCanonicalName(enc, ""))!=null){
                    ret = canonicalName;
                }*/else if(enc.indexOf("x-")==0 || enc.indexOf("X-")==0){
                    /* TODO: Match with getJavaCanonicalName method */
                    /*
                    char temp[ UCNV_MAX_CONVERTER_NAME_LENGTH] = {0};
                    strcpy(temp, encName+2);
                    */
                    // Remove the 'x-' and get the ICU canonical name
                    if ((canonicalName = UConverterAlias.getAlias(enc.substring(2), 0))!=null) {
                        ret = canonicalName;
                    } else {
                        ret = "";
                    }
                    
                }else{
                    /* unsupported encoding */
                   ret = "";
                }
            }
            return ret;
        }catch(IOException ex){
            throw new UnsupportedCharsetException(enc);
        } 
    }
    private Charset getCharset(String icuCanonicalName) throws IOException{
       String[] aliases = getAliases(icuCanonicalName);    
       String canonicalName = getJavaCanonicalName(icuCanonicalName);
       
       /* Concat the option string to the icuCanonicalName so that the options can be handled properly
        * by the actual charset.
        */
       if (optionsString != null) {
           icuCanonicalName = icuCanonicalName.concat(optionsString);
           optionsString = null;
       }
       
       return (CharsetICU.getCharset(icuCanonicalName,canonicalName, aliases));
    }
    /**
     * Gets the canonical name of the converter as defined by Java
     * @param charsetName converter name
     * @return canonical name of the converter
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static String getJavaCanonicalName(String charsetName){
        /*
        If a charset listed in the IANA Charset Registry is supported by an implementation 
        of the Java platform then its canonical name must be the name listed in the registry. 
        Many charsets are given more than one name in the registry, in which case the registry 
        identifies one of the names as MIME-preferred. If a charset has more than one registry 
        name then its canonical name must be the MIME-preferred name and the other names in 
        the registry must be valid aliases. If a supported charset is not listed in the IANA 
        registry then its canonical name must begin with one of the strings "X-" or "x-".
        */
        if(charsetName==null ){
            return null;
        }  
        try{
            String cName = null;
            /* find out the alias with MIME tag */
            if((cName=UConverterAlias.getStandardName(charsetName, "MIME"))!=null){
            /* find out the alias with IANA tag */
            }else if((cName=UConverterAlias.getStandardName(charsetName, "IANA"))!=null){
            }else {
                /*  
                    check to see if an alias already exists with x- prefix, if yes then 
                    make that the canonical name
                */
                int aliasNum = UConverterAlias.countAliases(charsetName);
                String name;
                for(int i=0;i<aliasNum;i++){
                    name = UConverterAlias.getAlias(charsetName, i);
                    if(name!=null && name.indexOf("x-")==0){
                        cName = name;
                        break;
                    }
                }
                /* last resort just append x- to any of the alias and 
                make it the canonical name */
                if((cName==null || cName.length()==0)){
                    name = UConverterAlias.getStandardName(charsetName, "UTR22");
                    if(name==null && charsetName.indexOf(",")!=-1){
                        name = UConverterAlias.getAlias(charsetName, 1);
                    }
                    /* if there is no UTR22 canonical name .. then just return itself*/
                    if(name==null){
                        name = charsetName;
                    }
                    cName = "x-"+ name;
                }
            }
            /* After getting the java canonical name from ICU alias table, get the
             * java canonical name from the current JDK. This is neccessary because
             * different versions of the JVM (Sun and IBM) may have a different
             * canonical name then the one given by ICU. So the java canonical name
             * will depend on the current JVM.  Since java cannot use the ICU canonical 
             * we have to try to use a java compatible name.
             */
            if (cName != null) {
                try {
                    if (Charset.isSupported(cName)) {
                        String testName = Charset.forName(cName).name();
                        /* Ensure that the java canonical name works in ICU */
                        if (!testName.equals(cName)) {
                            if (getICUCanonicalName(testName).length() > 0) {
                                cName = testName;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Any exception in the try block above
                    // must result Java's canonical name to be
                    // null. This block is necessary to reset
                    // gettingJavaCanonicalName to true always.
                    // See #9966.
                    // Note: The use of static gettingJavaCanonicalName
                    // looks really dangerous and obviously thread unsafe.
                    // We should revisit this code later. See #9973
                    cName = null;
                }
            }
            return cName;
        }catch (IOException ex){
            
        }
        return null;
     }

    /** 
     * Gets the aliases associated with the converter name
     * @param encName converter name
     * @return converter names as elements in an object array
     * @internal
     * @deprecated This API is ICU internal only.
     */
    private static final String[] getAliases(String encName)throws IOException{
        String[] ret = null;
        int aliasNum = 0;
        int i=0;
        int j=0;
        String aliasArray[/*50*/] = new String[50];
    
        if(encName != null){
            aliasNum = UConverterAlias.countAliases(encName);
            for(i=0,j=0;i<aliasNum;i++){
                String name = UConverterAlias.getAlias(encName,i);
                if(name.indexOf('+')==-1 && name.indexOf(',')==-1){
                    aliasArray[j++]= name;
                }
            }
            ret = new String[j];
            for(;--j>=0;) {
                ret[j] = aliasArray[j];
            }
                        
        }
        return (ret);
    
    }

    private void putCharsets(Map<Charset, String> map){
        int num = UConverterAlias.countAvailable();
        for(int i=0;i<num;i++) {
            String name = UConverterAlias.getAvailableName(i);
            try {
                Charset cs = getCharset(name);
                map.put(cs, getJavaCanonicalName(name));
            }catch(UnsupportedCharsetException ex){
            }catch (IOException e) {
            }
            // add only charsets that can be created!
        }
    }

    /**
     * Returns an iterator for the available charsets.
     * Implements the abstract method of super class.
     * @return Iterator the charset name iterator
     * @stable ICU 3.6
     */
    public final Iterator<Charset> charsets(){
        HashMap<Charset, String> map = new HashMap<Charset, String>();
        putCharsets(map);
        return map.keySet().iterator();
    }
    
    /**
     * Gets the canonical names of available converters 
     * @return array of available converter names
     * @internal
     * @deprecated This API is ICU internal only.
     */
     public static final String[] getAvailableNames(){
        CharsetProviderICU provider = new CharsetProviderICU();
        HashMap<Charset, String> map = new HashMap<Charset, String>();
        provider.putCharsets(map);
        return map.values().toArray(new String[0]);
    }
     
    /**
     * Return all names available
     * @return String[] an array of all available names
     * @internal
     * @deprecated This API is ICU internal only.
     */
     public static final String[] getAllNames(){
        int num = UConverterAlias.countAvailable();
        String[] names = new String[num];
        for(int i=0;i<num;i++) {
            names[i] = UConverterAlias.getAvailableName(i);
        }
        return names;
    }
    
    private String processOptions(String charsetName) {
        if (charsetName.indexOf(UConverterConstants.OPTION_SWAP_LFNL_STRING) > -1) {
            /* Remove and save the swap lfnl option string portion of the charset name. */
            optionsString = UConverterConstants.OPTION_SWAP_LFNL_STRING;
            
            charsetName = charsetName.substring(0, charsetName.indexOf(UConverterConstants.OPTION_SWAP_LFNL_STRING));
        }
        
        return charsetName;
    }
}
