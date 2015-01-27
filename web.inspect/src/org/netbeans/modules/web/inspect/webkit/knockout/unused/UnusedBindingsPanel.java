/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */

package org.netbeans.modules.web.inspect.webkit.knockout.unused;

import java.awt.EventQueue;
import java.awt.dnd.DnDConstants;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.netbeans.modules.web.inspect.files.Files;
import org.netbeans.modules.web.inspect.webkit.WebKitPageModel;
import org.netbeans.modules.web.webkit.debugging.api.debugger.RemoteObject;
import org.netbeans.modules.web.webkit.debugging.api.page.Page;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.util.RequestProcessor;

/**
 * Panel showing information about unused bindings.
 *
 * @author Jan Stola
 */
public class UnusedBindingsPanel extends javax.swing.JPanel implements ExplorerManager.Provider  {
    /** Request processor used by this class. */
    private static final RequestProcessor RP = new RequestProcessor(UnusedBindingsPanel.class);
    /** Page model for this panel. */
    private WebKitPageModel pageModel;
    /** Explorer manager provided by this panel. */
    private final ExplorerManager manager = new ExplorerManager();
    /** Tree with unused bindings. */
    private BeanTreeView treeView;

    /**
     * Creates a new {@code UnusedBindingsPanel}.
     */
    public UnusedBindingsPanel() {
        initComponents();
        add(findPanel);
        initTreeView();
        dataPanel.add(treeView);
    }

    /**
     * Initializes the three with unused bindings.
     */
    private void initTreeView() {
        treeView = new BeanTreeView();
        treeView.setAllowedDragActions(DnDConstants.ACTION_NONE);
        treeView.setAllowedDropActions(DnDConstants.ACTION_NONE);
        treeView.setRootVisible(false);
    }

    /**
     * Sets the page model for this panel.
     * 
     * @param pageModel page model for this panel
     */
    public void setPageModel(WebKitPageModel pageModel) {
        this.pageModel = pageModel;
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }

    /**
     * Prepares the page for the collection of unused binding information.
     * It reloads the page with a special JavaScript preprocessor.
     */
    void preparePage() {
        RP.post(new Runnable() {
            @Override
            public void run() {
                Page page = pageModel.getWebKit().getPage();
                String scriptToInject = Files.getScript("knockout"); // NOI18N
                scriptToInject = scriptToInject.replace("\"", "\\\""); // NOI18N
                scriptToInject = scriptToInject.replace("\n", "\\n"); // NOI18N
                String preprocessor =
                        "(function (script) {\n" + // NOI18N
                        "  var scriptToInject = \"" + scriptToInject + "\";\n" +  // NOI18N
                        "  var newScript;\n" + // NOI18N
                        "  if (script.indexOf('getBindingAccessors') != -1 && script.indexOf('bindingProvider') != -1) {\n" + // NOI18N
                        "    newScript = script + scriptToInject;\n" + // NOI18N
                        "  } else {\n" + // NOI18N
                        "    newScript = script;\n" + // NOI18N
                        "  }\n" + // NOI18N
                        "  return newScript;\n" + // NOI18N
                        "})";
                page.reload(false, null, preprocessor);
            }
        });
    }

    /**
     * This method is invoked to notify the panel about usage of Knockout
     * in the inspected page.
     * 
     * @param knockoutUsed determines whether Knockout is used or not.
     */
    public void setKnockoutUsed(boolean knockoutUsed) {
        if (knockoutUsed) {
            RP.post(new Runnable() {
                @Override
                public void run() {
                    RemoteObject remoteObject = pageModel.getWebKit().getRuntime().evaluate("window.NetBeans && NetBeans.unusedBindingsAvailable()"); // NOI18N
                    final boolean found = "true".equals(remoteObject.getValueAsString()); // NOI18N
                    if (found) {
                        updateData();
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                showComponent(dataPanel);
                            }
                        });
                    }
                }
            });
        } else {
            showComponent(findPanel);
        }
    }

    /**
     * Shows the specified component in this panel.
     * 
     * @param component component to show in the panel.
     */
    void showComponent(JComponent component) {
        removeAll();
        add(component);
        revalidate();
        repaint();
    }

    /**
     * Updates the unused binding information shown in the panel.
     */
    void updateData() {
        RemoteObject remoteObject = pageModel.getWebKit().getRuntime().evaluate("NetBeans.unusedBindings()"); // NOI18N
        String json = remoteObject.getValueAsString();
        manager.setRootContext(new UnusedRootNode(parse(json)));
    }

    /**
     * Parses the information about unused bindings.
     * 
     * @param json information about unused bindings (in JSON string).
     * @return information about unused bindings {@code (name -> (id -> binding))} map.
     */
    private Map<String,Map<Integer,UnusedBinding>> parse(String json) {
        Map<String,Map<Integer,UnusedBinding>> map = new HashMap<String,Map<Integer,UnusedBinding>>();
        try {
            JSONArray array = (JSONArray)new JSONParser().parse(json);
            for (Object o : array) {
                JSONObject jsonBinding = (JSONObject)o;
                String name = (String)jsonBinding.get("name"); // NOI18N
                int id = ((Number)jsonBinding.get("id")).intValue();
                UnusedBinding binding = new UnusedBinding(id, name,
                    (String)jsonBinding.get("nodeTagName"), // NOI18N
                    (String)jsonBinding.get("nodeId"), // NOI18N
                    (String)jsonBinding.get("nodeClasses"), // NOI18N
                    pageModel
                ); 
                Map<Integer,UnusedBinding> innerMap = map.get(name);
                if (innerMap == null) {
                    innerMap = new HashMap<Integer,UnusedBinding>();
                    map.put(name, innerMap);
                }
                innerMap.put(id, binding);
            }
        } catch (ParseException pex) {
            Logger.getLogger(UnusedBindingsPanel.class.getName()).log(Level.INFO, null, pex);
        }
        return map;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        findPanel = new javax.swing.JPanel();
        findButton = new javax.swing.JButton();
        findLabel = new javax.swing.JLabel();
        dataPanel = new javax.swing.JPanel();
        refreshPanel = new javax.swing.JPanel();
        refreshButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(findButton, org.openide.util.NbBundle.getMessage(UnusedBindingsPanel.class, "UnusedBindingsPanel.findButton.text")); // NOI18N
        findButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(findLabel, org.openide.util.NbBundle.getMessage(UnusedBindingsPanel.class, "UnusedBindingsPanel.findLabel.text")); // NOI18N

        javax.swing.GroupLayout findPanelLayout = new javax.swing.GroupLayout(findPanel);
        findPanel.setLayout(findPanelLayout);
        findPanelLayout.setHorizontalGroup(
            findPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(findPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(findPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(findButton)
                    .addComponent(findLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        findPanelLayout.setVerticalGroup(
            findPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(findPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(findButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(findLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        dataPanel.setLayout(new java.awt.BorderLayout());

        org.openide.awt.Mnemonics.setLocalizedText(refreshButton, org.openide.util.NbBundle.getMessage(UnusedBindingsPanel.class, "UnusedBindingsPanel.refreshButton.text")); // NOI18N
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout refreshPanelLayout = new javax.swing.GroupLayout(refreshPanel);
        refreshPanel.setLayout(refreshPanelLayout);
        refreshPanelLayout.setHorizontalGroup(
            refreshPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(refreshPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(refreshButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        refreshPanelLayout.setVerticalGroup(
            refreshPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(refreshPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(refreshButton)
                .addContainerGap())
        );

        dataPanel.add(refreshPanel, java.awt.BorderLayout.PAGE_END);

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    private void findButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findButtonActionPerformed
        preparePage();
    }//GEN-LAST:event_findButtonActionPerformed

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        RP.post(new Runnable() {
            @Override
            public void run() {
                updateData();
            }
        });
    }//GEN-LAST:event_refreshButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel dataPanel;
    private javax.swing.JButton findButton;
    private javax.swing.JLabel findLabel;
    private javax.swing.JPanel findPanel;
    private javax.swing.JButton refreshButton;
    private javax.swing.JPanel refreshPanel;
    // End of variables declaration//GEN-END:variables
}