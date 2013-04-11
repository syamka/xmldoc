/*
* (C) Copyright 1997 i-Teco, CJSK. All Rights reserved.
* i-Teco PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
* Эксклюзивные права 1997 i-Teco, ЗАО.
* Данные исходные коды не могут использоваться и быть изменены
* без официального разрешения компании i-Teco.          
*/
package ru.iteco.xmldoc;

import org.jdom2.Element;
import java.util.*;

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 09.04.13</p>
 */
public class ActionsGenerator extends AbstractGenerator<Action> {

    //Множество вссех прекондишнов
    protected Set<String> preconditions = new TreeSet<String>();
    //Множество всех классов execution
    protected Set<String> executions = new TreeSet<String>();

    @Override
    protected void process(Element el, String path) {
        super.process(el, path);

        Config confEl = ConfigEx.getElement(el, path);
        if(confEl instanceof Action){
            Action action = (Action) confEl;

            if(action.getExecutionClass() != null)
                executions.add(action.getExecutionClass());

            preconditions.addAll(action.getPreconditionsClasses());
        }
    }

    @Override
    protected Map<Object, Object> getBaseTemplateMap() {
        HashMap<Object, Object> data = new HashMap<Object, Object>();
        data.put("actions", items);
        data.put("preconditions", preconditions);
        data.put("executions", executions);
        data.put("scopes", scopes);
        data.put("notdefined", notdefineds);

        return data;
    }

    @Override
    protected String getElementName() {
        return "action";
    }


    public static void main(String...args) throws Exception {
        new ActionsGenerator().run();
    }
}
