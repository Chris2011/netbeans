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

import java.util.Map;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 * Root node of the unused bindings tree.
 *
 * @author Jan Stola
 */
public class UnusedRootNode extends AbstractNode {

    /**
     * Creates a new {@code UnusedRootNode}.
     * 
     * @param unusedBindings information about unused bindings
     * ({@code name -> (id -> binding)} map).
     */
    @NbBundle.Messages({
        "UnusedRootNode.displayName=Unused Bindings"
    })
    public UnusedRootNode(Map<String,Map<Integer,UnusedBinding>> unusedBindings) {
        super(new UnusedRootChildren(unusedBindings));
        setDisplayName(Bundle.UnusedRootNode_displayName());
    }

    void update(Map<String,Map<Integer,UnusedBinding>> unusedBindings) {
        ((UnusedRootChildren)getChildren()).update(unusedBindings);
    }

    /**
     * Children of {@code UnusedRootNode}.
     */
    static class UnusedRootChildren extends Children.Keys<String> {
        /** Unused binding information ({@code name -> (id -> binding)} map). */
        private java.util.Map<String,java.util.Map<Integer,UnusedBinding>> unusedBindings;

        /**
         * Creates a new {@code UnusedRootChildren}.
         * 
         * @param unusedBindings unused binding information
         * ({@code name -> (id -> binding)} map).
         */
        UnusedRootChildren(java.util.Map<String,java.util.Map<Integer,UnusedBinding>> unusedBindings) {
            this.unusedBindings = unusedBindings;
            setKeys(unusedBindings.keySet());
        }

        synchronized void update(java.util.Map<String,java.util.Map<Integer,UnusedBinding>> unusedBindings) {
            this.unusedBindings = unusedBindings;
        }

        @Override
        protected synchronized Node[] createNodes(String key) {
            java.util.Map<Integer,UnusedBinding> bindings = unusedBindings.get(key);
            if (bindings == null) {
                return new Node[0];
            } else {
                return new Node[] { new UnusedGroupNode(bindings) };
            }
        }
        
    }
    
}