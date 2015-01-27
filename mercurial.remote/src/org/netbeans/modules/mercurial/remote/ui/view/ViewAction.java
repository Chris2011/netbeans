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
package org.netbeans.modules.mercurial.remote.ui.view;

import java.util.logging.Level;
import org.netbeans.modules.mercurial.remote.HgException;
import org.netbeans.modules.mercurial.remote.HgModuleConfig;
import org.netbeans.modules.mercurial.remote.Mercurial;
import org.netbeans.modules.mercurial.remote.OutputLogger;
import org.netbeans.modules.mercurial.remote.config.HgConfigFiles;
import org.netbeans.modules.mercurial.remote.ui.actions.ContextAction;
import org.netbeans.modules.mercurial.remote.util.HgCommand;
import org.netbeans.modules.mercurial.remote.util.HgUtils;
import org.netbeans.modules.remotefs.versioning.api.VCSFileProxySupport;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.netbeans.modules.versioning.core.spi.VCSContext;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * View action for mercurial: 
 * hg view - launch hg view to view the dependency tree for the repository
 * 
 * @author John Rice
 */
public class ViewAction extends ContextAction {
    
    @Override
    protected boolean enable(Node[] nodes) {
        return HgUtils.isFromHgRepository(HgUtils.getCurrentContext(nodes));
    }

    @Override
    protected String getBaseName(Node[] nodes) {
        return "CTL_MenuItem_View"; //NOI18N
    }

    @Override
    protected void performContextAction(Node[] nodes) {
        VCSContext context = HgUtils.getCurrentContext(nodes);
        final VCSFileProxy roots[] = HgUtils.getActionRoots(context);
        if (roots == null || roots.length == 0) {
            return;
        }
        final VCSFileProxy root = Mercurial.getInstance().getRepositoryRoot(roots[0]);

        RequestProcessor rp = Mercurial.getInstance().getParallelRequestProcessor();
        rp.post(new Runnable() {
            @Override
            public void run() {
                performView(root);
            }
        });
    }

    static void performView(VCSFileProxy root) {
        OutputLogger logger = OutputLogger.getLogger(root);
        try {
            logger.outputInRed(NbBundle.getMessage(ViewAction.class, "MSG_VIEW_TITLE")); // NOI18N
            logger.outputInRed(NbBundle.getMessage(ViewAction.class, "MSG_VIEW_TITLE_SEP")); // NOI18N

            String hgkCommand = HgCommand.HGK_COMMAND;
            boolean bHgkFound = false;
            if(HgUtils.isInUserPath(hgkCommand)){
                    bHgkFound = true;                
            } else if(VCSFileProxySupport.isSolaris(root)){
                VCSFileProxy f = VCSFileProxySupport.getResource(root, HgCommand.HG_HGK_PATH_SOLARIS10+"/"+hgkCommand);
                if(f.exists() && f.isFile()) {
                    bHgkFound = true;
                }
            }
            boolean bHgkPropExists = HgConfigFiles.getSysInstance(root).containsProperty(
                            HgConfigFiles.HG_EXTENSIONS, HgConfigFiles.HG_EXTENSIONS_HGK);
            
            if(!bHgkFound){
                logger.outputInRed(
                            NbBundle.getMessage(ViewAction.class, "MSG_VIEW_HGK_NOT_FOUND_INFO")); // NOI18N
                logger.output(""); // NOI18N
                logger.outputInRed(NbBundle.getMessage(ViewAction.class, "MSG_VIEW_HGK_NOT_FOUND"));    // NOI18N
                logger.outputInRed(NbBundle.getMessage(ViewAction.class, "MSG_VIEW_HGK_NOT_FOUND_TITLE"));  // NOI18N
            }
            if(!bHgkPropExists){
                boolean bConfirmSetHgkProp = false;
                bConfirmSetHgkProp = HgUtils.confirmDialog(
                        ViewAction.class, "MSG_VIEW_SETHGK_PROP_CONFIRM_TITLE", // NOI18N
                        "MSG_VIEW_SETHGK_PROP_CONFIRM_QUERY"); // NOI18N                
                if (bConfirmSetHgkProp) {
                    logger.outputInRed(
                            NbBundle.getMessage(ViewAction.class, "MSG_VIEW_SETHGK_PROP_DO_INFO")); // NOI18N
                    HgConfigFiles hcf = HgConfigFiles.getSysInstance(root);
                    if (hcf.getException() == null) {
                        hcf.setProperty(HgConfigFiles.HG_EXTENSIONS_HGK, ""); // NOI18N
                    } else {
                        Mercurial.LOG.log(Level.WARNING, ViewAction.class.getName() + ": Cannot set hgk property"); // NOI18N
                        Mercurial.LOG.log(Level.INFO, null, hcf.getException());
                        HgModuleConfig.notifyParsingError();
                    }
                } else {
                    logger.outputInRed(
                            NbBundle.getMessage(ViewAction.class, "MSG_VIEW_NOTSETHGK_PROP_INFO")); // NOI18N
                    logger.output(""); // NOI18N
                    logger.closeLog();
                    return;
                }
            }
            
            logger.outputInRed(NbBundle.getMessage(ViewAction.class, 
                    "MSG_VIEW_LAUNCH_INFO", root.getPath())); // NOI18N
            logger.output(""); // NOI18N
            HgCommand.doView(root, logger);
        } catch (HgException.HgCommandCanceledException ex) {
            // canceled by user, do nothing
        } catch (HgException ex) {
            HgUtils.notifyException(ex);
        } finally {
            logger.closeLog();
        }
    }
}