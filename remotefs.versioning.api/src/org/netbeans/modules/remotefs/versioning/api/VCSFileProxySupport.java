/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.remotefs.versioning.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import org.netbeans.modules.remotefs.versioning.api.ProcessUtils.ExitStatus;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.netbeans.modules.versioning.core.api.VersioningSupport;
import org.netbeans.modules.versioning.core.spi.VCSContext;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 *
 * @author Alexander Simon
 */
public final class VCSFileProxySupport {
    private VCSFileProxySupport(){
    }

    public static final class VCSFileProxyComparator implements Comparator<VCSFileProxy> {

        @Override
        public int compare(VCSFileProxy o1, VCSFileProxy o2) {
            return o1.getPath().compareTo(o2.getPath());
        }
    }
    
    public static void delete(VCSFileProxy file) {
        // TODO: should not it return status or throw an exception on failure?
        RemoteVcsSupport.delete(file);
    }

    public static void deleteOnExit(VCSFileProxy file) {
        //TODO: implemetn it!
    }

    /**
     * Sets file timestamp equals to the timestamp of another (reference) file
     * @param file file to set timestamp
     * @param referenceFile reference file
     * NB: both files must be on the same file system !!!
     */
    public static void setLastModified(VCSFileProxy file, VCSFileProxy referenceFile) {
        RemoteVcsSupport.setLastModified(file, referenceFile);
    }
    
    public static boolean mkdir(VCSFileProxy file) {
        File javaFile = file.toFile();
        if (javaFile != null) {
            return javaFile.mkdir();
        } else {
            // TODO: rewrite it with using sftp
            ExitStatus status = ProcessUtils.executeInDir(file.getParentFile().getPath(), null, false, new ProcessUtils.Canceler(), VersioningSupport.createProcessBuilder(file),
                    "mkdir", file.getPath()); //NOI18N
            if (!status.isOK()) {
                ProcessUtils.LOG.log(Level.INFO, status.toString());
                return false;
            } else {
                // TODO: make sure that file.exists() returns true
                return true;
            }
        }
    }
    
    public static boolean mkdirs(VCSFileProxy file) {
        File javaFile = file.toFile();
        if (javaFile != null) {
            return javaFile.mkdirs();
        } else {
            // TODO: rewrite it with using sftp
            ExitStatus status = ProcessUtils.executeInDir(null, null, false, new ProcessUtils.Canceler(), VersioningSupport.createProcessBuilder(file),
                    "mkdir", "-p", file.getPath()); //NOI18N
            if (!status.isOK()) {
                ProcessUtils.LOG.log(Level.INFO, status.toString());
                return false;
            } else {
                // TODO: make sure that file.exists() returns true
                return true;
            }
        }
    }
    
    public static VCSFileProxy fromURI(URI uri) {
        if ("file".equals(uri.getScheme())) { // NOI18N
            return VCSFileProxy.createFileProxy(new File(uri));
        } else {
            try {
                List<String> segments = new ArrayList<>();
                FileObject fo = findExistingParent(uri, segments);
                VCSFileProxy res = VCSFileProxy.createFileProxy(fo);
                for (int i = segments.size() - 1; i >= 0; i--) {
                    res = VCSFileProxy.createFileProxy(res, segments.get(i));
                }
                return res;
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }
    }

    private static FileObject findExistingParent(URI uri, List<String> segments) throws MalformedURLException, URISyntaxException {
        while (true) {
            URL url =  uri.toURL();
            FileObject fo = URLMapper.findFileObject(url);
            if (fo != null) {
                return fo;
            }
            String path = uri.getPath();
            int i = path.indexOf('/');
            if (i < 0) {
                i = path.indexOf('\\');
            }
            if (i <= 0) {
                throw new MalformedURLException();
            }
            segments.add(path.substring(i+1));
            path = path.substring(0, i);
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, uri.getQuery(), uri.getFragment());
        }
    }
    
    public static URI toURI(VCSFileProxy file) {
        File javaFile = file.toFile();
        if (javaFile != null) {
            return javaFile.toURI();
        }
        try {
            List<String> segments = new ArrayList<>();
            FileObject fo = findExistingParent(file, segments);
            URI res = fo.toURI();
            for (int i = segments.size() - 1; i >= 0; i--) {
                String path;
                if (res.getPath().endsWith("/")) { //NOI18N
                    path = res.getPath()+segments.get(i);
                } else {
                    path = res.getPath()+"/"+segments.get(i); //NOI18N
                }
                res = new URI(res.getScheme(), res.getUserInfo(), res.getHost(), res.getPort(), path, res.getQuery(), res.getFragment());
            }
            return res;
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private static FileObject findExistingParent(VCSFileProxy file, List<String> segments) throws FileNotFoundException {
        while (true) {
            FileObject fo = file.toFileObject();
            if (fo != null) {
                return fo;
            }
            segments.add(file.getName());
            file = file.getParentFile();
            if (file == null) {
                throw new FileNotFoundException();
            }
        }
    }
    
    public static boolean isSymlink(VCSFileProxy file, VCSFileProxy root) {
        return RemoteVcsSupport.isSymlink(file);
    }
    
    public static boolean canRead(VCSFileProxy file) {
        return RemoteVcsSupport.canRead(file);
    }
    
    public static boolean canRead(VCSFileProxy base, String subdir) {
        return RemoteVcsSupport.canRead(base, subdir);
    }
    
    public static boolean createNew(VCSFileProxy file) throws IOException {
        VCSFileProxy parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            mkdirs(parentFile);
        }
         ExitStatus status = ProcessUtils.executeInDir(parentFile.getPath(), null, false, new ProcessUtils.Canceler(), VersioningSupport.createProcessBuilder(file),
                 "touch", file.getName()); //NOI18N
        if (!status.isOK()) {
            ProcessUtils.LOG.log(Level.INFO, status.toString());
            throw new IOException(status.toString());
        }
        return true;
    }
    
    public static OutputStream getOutputStream(VCSFileProxy file) throws IOException {
        return RemoteVcsSupport.getOutputStream(file);
    }

    public static long length(VCSFileProxy file) {
        return RemoteVcsSupport.getSize(file);
    }
    
    public static String getCanonicalPath(VCSFileProxy file) throws IOException {
        return RemoteVcsSupport.getCanonicalPath(file);
    }

    public static VCSFileProxy getCanonicalFile(VCSFileProxy file) throws IOException {
        return RemoteVcsSupport.getCanonicalFile(file);
    }
    
    public static VCSFileProxy generateTemporaryFile(VCSFileProxy file, String name) {
        VCSFileProxy tmp = VCSFileProxy.createFileProxy(file, name);
        while (tmp.exists()) {
            tmp = VCSFileProxy.createFileProxy(file, name + Long.toString(System.currentTimeMillis()));
        }
        return tmp;
    }

    public static VCSFileProxy createTempFile(VCSFileProxy file, String prefix, String suffix, boolean deleteOnExit) throws IOException {
        File javaFile = file.toFile();
        if (javaFile != null) {
            File res = File.createTempFile(prefix, suffix);
            res.deleteOnExit();
            return VCSFileProxy.createFileProxy(res);
        } else {
            // TODO: review it!
            return VCSFileProxy.createFileProxy(file.toFileObject().createData(prefix+Long.toString(System.currentTimeMillis()), suffix));
        }
    }
    
    /**
     * Creates a temporary folder. The folder will have deleteOnExit flag set to <code>deleteOnExit</code>.
     * @return
     */
    public static VCSFileProxy getTempFolder(VCSFileProxy file, boolean deleteOnExit) throws FileStateInvalidException, IOException {
        FileObject tmpDir = VCSFileProxySupport.getFileSystem(file).getTempFolder();
        for (;;) {
            try {
                //TODO: support delete on exit
                FileObject dir = tmpDir.createFolder("vcs-" + Long.toString(System.currentTimeMillis())); // NOI18N
                return VCSFileProxy.createFileProxy(dir).normalizeFile();
            } catch (IOException ex) {
                continue;
            }
        }
    }
    
    public static boolean renameTo(VCSFileProxy from, VCSFileProxy to){
        File javaFile = from.toFile();
        if (javaFile != null) {
            return javaFile.renameTo(to.toFile());
        } else {
            // TODO: rewrite it with using sftp
            ExitStatus status = ProcessUtils.executeInDir(from.getParentFile().getPath(), null, false, new ProcessUtils.Canceler(),
                    VersioningSupport.createProcessBuilder(from),
                    "mv", "-f", from.getName(), to.getPath()); //NOI18N
            if (!status.isOK()) {
                ProcessUtils.LOG.log(Level.INFO, status.toString());
                return false;
            } else {
                return true;
            }
        }
    }
    
    public static void copyDirFiles(VCSFileProxy sourceDir, VCSFileProxy targetDir, boolean preserveTimestamp) {
        VCSFileProxy[] files = sourceDir.listFiles();

        if(files==null || files.length == 0) {
            mkdirs(targetDir);
            if(preserveTimestamp) {
                setLastModified(targetDir, sourceDir);
            }
            return;
        }
        if(preserveTimestamp) {
            setLastModified(targetDir, sourceDir);
        }
        for (int i = 0; i < files.length; i++) {
            try {
                VCSFileProxy target = VCSFileProxy.createFileProxy(targetDir, files[i].getName()).normalizeFile();
                if(files[i].isDirectory()) {
                    copyDirFiles(files[i], target, preserveTimestamp);
                } else {
                    copyFile(files[i], target);
                    if(preserveTimestamp) {
                        setLastModified(target, files[i]);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(VCSFileProxySupport.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
            }
        }
    }

    public static boolean copyFile(VCSFileProxy from, VCSFileProxy to) throws IOException {
        if (from == null || to == null) {
            throw new NullPointerException("from and to files must not be null"); // NOI18N
        }
        InputStream inputStream = null;
        try {
            inputStream = from.getInputStream(false);
            copyStreamToFile(inputStream, to);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException ex) {
                    // ignore
                }
            }
        }
        return true;
    }
    
    /**
     * Returns the first found file whose filename is the same (in a case insensitive way) as given <code>file</code>'s.
     * @param file
     * @return the first found file with the same name, but ignoring case, or <code>null</code> if no such file is found.
     */
    public static String getExistingFilenameInParent(VCSFileProxy file) {
        String filename = null;
        if (file == null) {
            return filename;
        }
        VCSFileProxy parent = file.getParentFile();
        if (parent == null) {
            return filename;
        }
        VCSFileProxy[] children = parent.listFiles();
        if (children == null) {
            return filename;
        }
        for (VCSFileProxy child : children) {
            if (file.getName().equalsIgnoreCase(child.getName())) {
                filename = child.getName();
                break;
            }
        }
        return filename;
    }
    
    /**
     * Copies the specified sourceFile to the specified targetFile.
     * It <b>closes</b> the input stream.
     */
    public static void copyStreamToFile(InputStream inputStream, VCSFileProxy targetFile) throws IOException {
        OutputStream outputStream = null;
        try {            
            outputStream = VCSFileProxySupport.getOutputStream(targetFile);
            try {
                byte[] buffer = new byte[32768];
                for (int readBytes = inputStream.read(buffer);
                     readBytes > 0;
                     readBytes = inputStream.read(buffer)) {
                    outputStream.write(buffer, 0, readBytes);
                }
            } catch (IOException ex) {
                VCSFileProxySupport.delete(targetFile);
                throw ex;
            }
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException ex) {
                    // ignore
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                }
                catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
    
    public static boolean isRemoteFileSystem(VCSFileProxy file) {
        return file.toFile() == null;
    }
    
    public static VCSFileProxy getResource(VCSFileProxy file, String absPath) {
        if (!absPath.startsWith("/")) { //NOI18N
            assert absPath.startsWith("/") : "Path "+absPath+"must be absolute";
        }
        VCSFileProxy parent = file;
        while (true) {
            parent = file.getParentFile();
            if (parent == null) {
                parent = file;
                break;
            }
            file = parent;
        }
        return VCSFileProxy.createFileProxy(parent, absPath.substring(1));
    }

    public static VCSFileProxy getResource(FileSystem fileSystem, String absPath) {
        VCSFileProxy root = VCSFileProxy.createFileProxy(fileSystem.getRoot());
        if (absPath.startsWith("/")) { //NOI18N
            return VCSFileProxy.createFileProxy(root, absPath.substring(1));
        } else {
            // Abs Path should start with "/". Assertion?
            return VCSFileProxy.createFileProxy(root, absPath);
        }
    }
    
    public static VCSFileProxy getHome(VCSFileProxy file){
        return RemoteVcsSupport.getHome(file);
    }

    /**
     * Creates or gets the user-localhost folder to persist svn configurations.
     * @return
     */
    public static VCSFileProxy getRemotePeristenceFolder(FileSystem fileSystem) throws FileStateInvalidException, IOException {
        FileObject tmpDir = fileSystem.getTempFolder();
        String userName = "user";  //NOI18N //RemoteExecutionEnvironment.getUser();
        String folderName = "svn_" + userName; // NOI18N
        FileObject res = tmpDir.getFileObject(folderName);
        if (res == null || !res.isValid()) {
            res = tmpDir.createFolder(folderName);
        }
        // TODO: create subfolder with hash of localhost
        return VCSFileProxy.createFileProxy(res);
    }
    
    public static boolean isMac(VCSFileProxy file) {
        return RemoteVcsSupport.isMac(file);
    }

    public static boolean isSolaris(VCSFileProxy file) {
        return RemoteVcsSupport.isSolaris(file);
    }

    public static boolean isUnix(VCSFileProxy file){
        return RemoteVcsSupport.isUnix(file);
    }
    
    public static String getFileSystemKey(FileSystem file) {
        return RemoteVcsSupport.getFileSystemKey(file);
    }
    
    public static boolean isConnectedFileSystem(FileSystem file) {
        return RemoteVcsSupport.isConnectedFileSystem(file);
    }

    public static void connectFileSystem(FileSystem file) {
        RemoteVcsSupport.connectFileSystem(file);
    }
    
    public static String toString(VCSFileProxy file) {
        return RemoteVcsSupport.toString(file);
    }
    
    public static VCSFileProxy fromString(String file) {
        return RemoteVcsSupport.fromString(file);
    }

    /**
     * 
     * @param proxy defines FS and initial selection
     * @return 
     */
    public static JFileChooser createFileChooser(VCSFileProxy proxy) {
        return RemoteVcsSupport.createFileChooser(proxy);
    }

    public static VCSFileProxy getSelectedFile(JFileChooser chooser) {
        return RemoteVcsSupport.getSelectedFile(chooser);
    }
    
    public static FileSystem getDefaultFileSystem() {
        // TODO: remove dependencies!
        return RemoteVcsSupport.getDefaultFileSystem();
    }

    public static FileSystem[] getFileSystems() {
        // TODO: return list of remote file systems
        return RemoteVcsSupport.getFileSystems();
    }

    public static FileSystem getFileSystem(VCSFileProxy file) {
        return RemoteVcsSupport.getFileSystem(file);
    }
    
    public static FileSystem readFileSystem(DataInputStream is) throws IOException {
        return RemoteVcsSupport.readFileSystem(is);
    }

    public static void writeFileSystem(DataOutputStream os, FileSystem fs) throws IOException {
        RemoteVcsSupport.writeFileSystem(os, fs);
    }

//<editor-fold defaultstate="collapsed" desc="methods from org.netbeans.modules.versioning.util.Utils">
    public static boolean isAncestorOrEqual(VCSFileProxy ancestor, VCSFileProxy file) {
        String ancestorPath = ancestor.getPath();
        String filePath = file.getPath();
        if (VCSFileProxySupport.isMac(ancestor)) {
            // Mac is not case sensitive, cannot use the else statement
            if(filePath.length() < ancestorPath.length()) {
                return false;
            }
        } else {
            if(!filePath.startsWith(ancestorPath)) {
                return false;
            }
        }
        
        // get sure as it still could be something like:
        // ancestor: /home/dil
        // file:     /home/dil1/dil2
        for (; file != null; file = file.getParentFile()) {
            if(ancestor == null) {
                // XXX have to rely on path because of fileproxy being created from
                // io.file even if it was originaly stored from a remote
                if (file.getPath().equals(ancestorPath)) {
                    return true;
                }
            } else {
                if (file.equals(ancestor)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Opens a file in the editor area.
     *
     * @param file a File to open
     */
    public static void openFile(VCSFileProxy file) {
        FileObject fo = file.toFileObject();
        if (fo != null) {
            try {
                DataObject dao = DataObject.find(fo);
                OpenCookie oc = dao.getLookup().lookup(OpenCookie.class);
                if (oc != null) {
                    oc.open();
                }
            } catch (DataObjectNotFoundException e) {
                // nonexistent DO, do nothing
            }
        }
    }
    
    /**
     * Splits files/folders into 2 groups: flat folders and other files
     *
     * @param files array of files to split
     * @return File[][] the first array File[0] contains flat folders (
     * @see #flatten for their direct descendants), File[1] contains all other
     * files
     */
    public static VCSFileProxy[][] splitFlatOthers(VCSFileProxy[] files) {
        Set<VCSFileProxy> flat = new HashSet<>(1);
        for (int i = 0; i < files.length; i++) {
            if (VersioningSupport.isFlat(files[i])) {
                flat.add(files[i]);
            }
        }
        if (flat.isEmpty()) {
            return new VCSFileProxy[][]{new VCSFileProxy[0], files};
        } else {
            Set<VCSFileProxy> allFiles = new HashSet<>(Arrays.asList(files));
            allFiles.removeAll(flat);
            return new VCSFileProxy[][]{
                        flat.toArray(new VCSFileProxy[flat.size()]),
                        allFiles.toArray(new VCSFileProxy[allFiles.size()])
                    };
        }
    }
    
    /**
     * Flattens the given collection of files and removes those that do not respect the flat folder logic,
     * i.e. those that lie deeper under a flat folder.
     * @param roots selected files with flat folders
     * @param files
     * @return 
     */
    public static Set<VCSFileProxy> flattenFiles (VCSFileProxy[] roots, Collection<VCSFileProxy> files) {
        VCSFileProxy[][] split = splitFlatOthers(roots);
        Set<VCSFileProxy> filteredFiles = new HashSet<>(files);
        if (split[0].length > 0) {
            outer:
            for (Iterator<VCSFileProxy> it = filteredFiles.iterator(); it.hasNext(); ) {
                VCSFileProxy f = it.next();
                // file is directly under a flat folder
                for (VCSFileProxy flat : split[0]) {
                    if (f.getParentFile().equals(flat)) {
                        continue outer;
                    }
                }
                // file lies under a recursive folder
                for (VCSFileProxy folder : split[1]) {
                    if (isAncestorOrEqual(folder, f)) {
                        continue outer;
                    }
                }
                it.remove();
            }
        }
        return filteredFiles;
    }
    
    /**
     * Checks if the context was originally created from files, not from nodes
     * and if so then it tries to determine if those original files are part of
     * a single DataObject. Call only if the context was created from files (not
     * from nodes), otherwise always returns false.
     *
     * @param ctx context to be checked
     * @return true if the context was created from files of the same DataObject
     */
    public static boolean isFromMultiFileDataObject(VCSContext ctx) {
        if (ctx != null) {
            Collection<? extends Set> allSets = ctx.getElements().lookupAll(Set.class);
            if (allSets != null) {
                for (Set contextElements : allSets) {
                    // private contract with org.openide.loaders - original files from multifile dataobjects are passed as
                    // org.openide.loaders.DataNode$LazyFilesSet
                    if ("org.openide.loaders.DataNode$LazyFilesSet".equals(contextElements.getClass().getName())) { //NOI18N
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Tests whether all files belong to the same data object.
     *
     * @param files array of Files
     * @return true if all files share common DataObject (even null), false
     * otherwise
     */
    public static boolean shareCommonDataObject(VCSFileProxy[] files) {
        if (files == null || files.length < 2) {
            return true;
        }
        DataObject common = findDataObject(files[0]);
        for (int i = 1; i < files.length; i++) {
            DataObject dao = findDataObject(files[i]);
            if (dao != common && (dao == null || !dao.equals(common))) {
                return false;
            }
        }
        return true;
    }

    private static DataObject findDataObject(VCSFileProxy file) {
        FileObject fo = file.toFileObject();
        if (fo != null) {
            try {
                return DataObject.find(fo);
            } catch (DataObjectNotFoundException e) {
                // ignore
            }
        }
        return null;
    }

//</editor-fold>
    
}