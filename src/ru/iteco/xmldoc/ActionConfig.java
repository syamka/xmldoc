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

import java.util.*;

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 03.04.13</p>
 */
public class ActionConfig extends ConfigEx {

    public ActionConfig(Element element, String path){
        init(element, path);
    }

    public Set<String> getPreconditionsClasses(){
        Set<String> result = new TreeSet<String>();
        for(Map<String, String> map: getPreconditions()){
            result.add(map.get("class"));
        }
        return result;
    }

    public List<Map<String, String>> getPreconditions(){
        if(element.getChild("preconditions") != null){
            return getPreconditionsRecursively(element.getChild("preconditions"));
        };
        return new LinkedList<Map<String, String>>();
    }

    protected List<Map<String, String>> getPreconditionsRecursively(Element prElem){
        List<Map<String, String>> result = new LinkedList<Map<String, String>>();
        List<Element> precons = prElem.getChildren("precondition");
        if(precons != null){
            for(Element pr: precons){
                result.add(getElementMap(pr));
                result.addAll(getPreconditionsRecursively(pr));
            }
        }
        return result;
    }

    public String getPreconditionsSource(){
        Element precondsEl = element.getChild("preconditions");
        if(precondsEl != null)
            return new XMLOutputter().outputString(precondsEl).trim();
        return "";
    }

    public Map<String, String> getExecution(){
        return getChildElementMap("execution");
    }

    public String getExecutionClass(){
        if(getExecution() != null)
            return getExecution().get("class");
        return null;
    }


    public List<Map<String, String>> getAdditionalChildren(){
        List<Map<String, String>> result = new LinkedList<Map<String, String>>();

        List<Element> allChildren = element.getChildren();
        for(Element el:allChildren){
            if(!Arrays.asList(new String[]{"preconditions", "execution", "params"}).contains(el.getName())){
                result.add(getElementMap(el));
            }
        };
        return result;
    }



    protected static String NAME = "action";

}
