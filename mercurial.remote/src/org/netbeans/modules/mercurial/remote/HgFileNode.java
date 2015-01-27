/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.mercurial.remote;

import org.openide.filesystems.FileObject;

import java.util.List;
import java.util.ArrayList;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.netbeans.modules.versioning.util.common.VCSCommitOptions;
import org.netbeans.modules.versioning.util.common.VCSFileNode;
import org.openide.util.NbBundle;

/**
 * Represents real or virtual (non-local) file.
 *
 * @author Padraig O'Briain
 */
public final class HgFileNode /*extends VCSFileNode<FileInformation>*/ {

    private final VCSFileProxy root;
    private final VCSFileProxy file;
    private final VCSFileProxy normalizedFile;
    private FileObject fileObject;
    private String shortPath;
    private VCSCommitOptions commitOption;

    public HgFileNode(VCSFileProxy root, VCSFileProxy file) {
        this.root = root;
        this.file = file;
        normalizedFile = file.normalizeFile();
    }

    public String getName() {
        return file.getName();
    }

    public FileInformation getInformation() {
        return Mercurial.getInstance().getFileStatusCache().getStatus(file); 
    }

    public VCSFileProxy getFile() {
        return file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof HgFileNode && file.equals(((HgFileNode) o).file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    public FileObject getFileObject() {
        if (fileObject == null) {
            fileObject = normalizedFile.toFileObject();
        }
        return fileObject;
    }

    public Object[] getLookupObjects() {
        List<Object> list = new ArrayList<Object>(2);
        list.add(file);
        FileObject fo = getFileObject();
        if (fo != null) {
            list.add(fo);
        }
        return list.toArray(new Object[list.size()]);
    }

    public VCSCommitOptions getDefaultCommitOption (boolean withExclusions) {
        return VCSCommitOptions.COMMIT;
    }
    
    public String getStatusText () {
        return getInformation().getStatusText();
    }
    
    public VCSCommitOptions getCommitOptions() {
        if(commitOption == null) {
            commitOption = getDefaultCommitOption(true);
        }
        return commitOption;
    }
    
    void setCommitOptions(VCSCommitOptions option) {
        commitOption = option;
    }
        
    public VCSFileProxy getRoot () {
        return root;
    }

    public String getRelativePath() {        
        if(shortPath == null) {
            String path = file.getPath();
            String rootPath = root.getPath();
            if (path.startsWith(rootPath)) {
                if (path.length() == rootPath.length()) {
                    shortPath = "."; //NOI18N
                } else {
                    shortPath = path.substring(rootPath.length() + 1);
                }
            } else {
                shortPath = NbBundle.getMessage(VCSFileNode.class, "LBL_Location_NotInRepository"); //NOI18N
            }
        }
        return shortPath;
    }    
}