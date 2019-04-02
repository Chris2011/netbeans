/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.html.editor.hints.other.img;

import org.netbeans.modules.html.editor.hints.css.*;
import java.io.IOException;
import java.util.List;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.modules.html.editor.api.gsf.HtmlParserResult;
import org.netbeans.modules.html.editor.hints.HtmlRule;
import org.netbeans.modules.html.editor.hints.HtmlRuleContext;
import org.netbeans.modules.html.editor.lib.api.elements.ElementType;
import org.netbeans.modules.html.editor.lib.api.elements.ElementUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author marekfukala
 */
@NbBundle.Messages({
    "MSG_MissingAltAttribute=Img tag is missing alt attribute",
    "MSG_MissingAltAttribute_Desc=An img tag needs an alt attribute."
})
public class MissingAltAttributeRule extends HtmlRule {
    private static final String MSG_MISSING_ALT_ATTRIBUTE = NbBundle.getMessage(MissingCssElement.class, "MSG_MissingAltAttribute");
    private static final String MSG_MISSING_ALT_ATTRIBUTE_DESC = NbBundle.getMessage(MissingCssElement.class, "MSG_MissingAltAttribute_Desc");
    
    public MissingAltAttributeRule() {
    }

    @Override
    protected void run(HtmlRuleContext context, List<Hint> result) {
        try {
            HtmlParserResult parserResult = context.getHtmlParserResult();
            CssIdsVisitor visitor = new CssIdsVisitor(this, context, result);
            ElementUtils.visitChildren(parserResult.root(), visitor, ElementType.OPEN_TAG);
        } catch(IOException ioe) {
            Exceptions.printStackTrace(ioe);
        }
    }
    
    @Override
    public boolean appliesTo(RuleContext context) {
        HtmlParserResult result = (HtmlParserResult) context.parserResult;
        FileObject file = result.getSnapshot().getSource().getFileObject();

        if (file == null) {
            return false;
        }

        Project project = FileOwnerQuery.getOwner(file);
        if (project == null) {
            return false;
        }

        return true;
    }

    @Override
    public String getDescription() {
        return MSG_MISSING_ALT_ATTRIBUTE_DESC;
    }

    @Override
    public String getDisplayName() {
        return MSG_MISSING_ALT_ATTRIBUTE;
    }

    @Override
    public boolean showInTasklist() {
        return false;
    }

    @Override
    public HintSeverity getDefaultSeverity() {
        return HintSeverity.CURRENT_LINE_WARNING;
    }   
}