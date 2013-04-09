/*
* (C) Copyright 1997 i-Teco, CJSK. All Rights reserved.
* i-Teco PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
* Эксклюзивные права 1997 i-Teco, ЗАО.
* Данные исходные коды не могут использоваться и быть изменены
* без официального разрешения компании i-Teco.          
*/
package ru.iteco.xmldoc;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Element;

import java.util.*;

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 03.04.13</p>
 */
public class Scope {

    public Map<String, Set<String>> getParams() {
        return params;
    }

    public Scope(){};

    public Scope(Map<String, Set<String>> params){
        this.params = params;
    }

    public Scope(Element element){
        Element scopeEl = element.getParentElement();
        if(scopeEl.getName() != "scope")
            System.out.println("Where is scope ? Parent el :"+ scopeEl.getName());
        else{
            List<Attribute> scopes = scopeEl.getAttributes();
            for(Attribute sc: scopes){
                String tmp_val = sc.getValue();
                String[] tmp_vals = tmp_val.split(",");
                for(String v: tmp_vals){
                    add(sc.getName(), v.trim());
                }
            }
        }
    }

    public Scope merge(Scope scope){
        Scope result = new Scope(params);
        result.add(scope);
        return result;
    }

    public void add(Scope scope){
        Map<String, Set<String>> newParams = scope.getParams();

        for(String key: newParams.keySet()){
            add(key, newParams.get(key));
        }
    }

    public void add(String name, String value){
        if(!params.containsKey(name))
            params.put(name, new TreeSet<String>());
        params.get(name).add(value);
    }

    public void add(String name, Set<String> values){
        if(!params.containsKey(name))
            params.put(name, new TreeSet<String>());
        params.get(name).addAll(values);
    }

    @Override
    public String toString() {
        List<String> lst = new LinkedList<String>();
        for(String key: params.keySet()){
            for(String val: params.get(key)){
                lst.add(new StringBuilder().append(key).append("=").append(val).toString());
            }
        };
        return (lst.size() > 0)? StringUtils.join(lst, SCOPE_DELIMETER): EMPTY_SCOPE_TITLE;
    }

    protected Map<String, Set<String>> params = new LinkedHashMap<String, Set<String>>();

    public static String SCOPE_DELIMETER = ";";

    public static String EMPTY_SCOPE_TITLE = "global";

}
