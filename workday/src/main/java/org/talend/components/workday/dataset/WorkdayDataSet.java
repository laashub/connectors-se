/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.components.workday.dataset;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.talend.components.workday.datastore.WorkdayDataStore;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Suggestable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@DataSet("WorkdayDataset")
@GridLayout({ @GridLayout.Row("datastore"), @GridLayout.Row("service"), @GridLayout.Row("parameters") })
@Documentation("")
public class WorkdayDataSet implements QueryHelper, Serializable {

    private static final long serialVersionUID = -9037128911796623682L;

    @Option
    @Documentation("The connection to workday datastore")
    private WorkdayDataStore datastore;

    @Option
    @Suggestable(value = "workdayServices", parameters = { "datastore" })
    @Documentation("A valid read only query is the source type is Query")
    private String service;

    @Data
    @RequiredArgsConstructor
    @GridLayout({ @GridLayout.Row({ "type", "name", "value" }) })
    public static class Parameter implements Serializable {

        private static final long serialVersionUID = 4222585870348980275L;

        public enum Type {
            Query,
            Path
        }

        @Option
        @Documentation("kind (path or query)")
        private final Type type;

        @Option
        @Documentation("name")
        private final String name;

        @Option
        @Documentation("value")
        private String value = "";

        public String substitute(String brut) {
            String pattern = '{' + this.name.trim() + '}';
            if (!brut.contains(pattern)) {
                return brut;
            }
            return brut.replace(pattern, value);
        }

        public void substitute(StringBuilder brut) {
            String pattern = '{' + this.name.trim() + '}';
            int start = brut.indexOf(pattern);
            if (start >= 0) {
                brut.replace(start, start + pattern.length(), value);
            }
        }
    }

    @Option
    @Documentation("service parameters")
    private final List<Parameter> parameters = new ArrayList<>();

    @Override
    public String getServiceToCall() {
        final StringBuilder toCall = new StringBuilder(service);
        this.parameters.forEach((Parameter p) -> p.substitute(toCall));
        return toCall.toString();
    }

    public WorkdayDataSet() {
        Parameter p1 = new Parameter(Parameter.Type.Query, "param1");
        this.parameters.add(p1);
    }

    @Override
    public Map<String, String> extractQueryParam() {
        return parameters.stream()
                .filter((Parameter x) -> x.type == Parameter.Type.Query && x.value != null && !x.value.isEmpty())
                .collect(Collectors.toMap(Parameter::getName, Parameter::getValue));
    }
}
