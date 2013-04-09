/*
* (C) Copyright 1997 i-Teco, CJSK. All Rights reserved.
* i-Teco PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
* Эксклюзивные права 1997 i-Teco, ЗАО.
* Данные исходные коды не могут использоваться и быть изменены
* без официального разрешения компании i-Teco.          
*/
package ru.iteco.xmldoc;

import java.util.HashMap;
import java.util.Map;

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 09.04.13</p>
 */
public class ComponentsGenerator extends AbstractGenerator<Component> {

    @Override
    protected Map<Object, Object> getBaseTemplateMap() {
        HashMap<Object, Object> data = new HashMap<Object, Object>();
        data.put("components", items);
        data.put("scopes", scopes);
        data.put("notdefined", notdefineds);

        return data;
    }

    @Override
    protected String getElementName() {
        return "component";
    }

    @Override
    public void run() throws Exception {
        setTemplateHelper("component.ftl", "base_c.ftl", "components", "components.html");
        super.run();
    }

    public static void main(String...args) throws Exception {
        new ComponentsGenerator().run();
    }
}
