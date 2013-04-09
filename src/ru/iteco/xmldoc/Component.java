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
import org.jdom2.output.XMLOutputter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 09.04.13</p>
 */
public class Component extends ConfigEx {

    public Component(Element element, String path){
        init(element, path);
    }

    public String getNlsBundle(){
        Element nlsEl = element.getChild("nlsbundle");
        if(nlsEl != null)
            return nlsEl.getValue();
        return "";
    }

    public String getPagesSource(){
        Element pagesEl = element.getChild("pages");
        if(pagesEl != null)
            return new XMLOutputter().outputString(pagesEl).trim();
        return "";
    }

    public String getPageStart(){
        Element pagesEl = element.getChild("pages");
        if(pagesEl != null){
            Element startEl = pagesEl.getChild("start");
            if(startEl == null){
                Element filterEl = pagesEl.getChild("filter");
                if(filterEl != null)
                    startEl = filterEl.getChild("start");
            }
            if(startEl != null)
                return startEl.getValue();
        }
        return "";
    }

    public String getComponentClass(){
        Element clsEl = element.getChild("class");
        if(clsEl != null)
            return clsEl.getValue();
        return "";
    }

    public List<Map<String, String>> getAdditionalChildren(){
        List<Map<String, String>> result = new LinkedList<Map<String, String>>();

        List<Element> allChildren = element.getChildren();
        for(Element el:allChildren){
            if(!Arrays.asList(new String[]{"nlsbundle", "pages", "class", "params"}).contains(el.getName())){
                result.add(getElementMap(el));
            }
        };
        return result;
    }

    protected static String NAME = "component";
}
