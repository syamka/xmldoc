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

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 03.04.13</p>
 */
public class Modifier extends Config {

    protected String elName;

    protected String parentId;

    protected String modify;

    public Modifier(Element element, String path){
        init(element, path);

        modify = element.getAttributeValue("modifies");
        parentId = parseIdFromPath(modify);
        elName = element.getName();
    }

    public String getModify() {
        return modify;
    }

    public String getElName() {
        return elName;
    }

    public String getParentId() {
        return parentId;
    }


}
