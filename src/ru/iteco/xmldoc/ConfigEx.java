/*
* (C) Copyright 1997 i-Teco, CJSK. All Rights reserved.
* i-Teco PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
* Эксклюзивные права 1997 i-Teco, ЗАО.
* Данные исходные коды не могут использоваться и быть изменены
* без официального разрешения компании i-Teco.          
*/
package ru.iteco.xmldoc;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 03.04.13</p>
 */
public abstract class ConfigEx extends Config {

    public static Config getElement(Element el, String path){
        if(el.getAttributeValue("modifies") != null)
            return new Modifier(el, path);
        if(el.getAttributeValue("notdefined") != null)
            return new Notdefined(el, path);
        if(el.getName().equals("action"))
            return new ActionConfig(el,path);
        if(el.getName().equals("component"))
            return new Component(el,path);
        return null;
    }

    public static String getName(){
        return NAME;
    }

    protected static String NAME = "";

    public String getId() {
        return id;
    }

    protected String id;

    @Override
    public void init(Element element, String path) {
        super.init(element, path);
        id = element.getAttributeValue("id");
        parent = element.getAttributeValue("extends");
        setUniqueId();
    }

    protected Set<Modifier> modifiers = new TreeSet<Modifier>();

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public void addModifier(Modifier modifier){
        modifiers.add(modifier);
    }


    protected String parent;

    public String getParent() {
        return parent;
    }

    public String getParentId(){
        return parseIdFromPath(getParent());
    }

    protected void setParent(String parent) {
        this.parent = parent;
    }

    public ConfigEx getParentConfig() {
        return parentConfig;
    }

    public void setParentConfig(ConfigEx parentConfig) {
        this.parentConfig = parentConfig;
    }

    protected ConfigEx parentConfig;



    protected Set<ConfigEx> children = new LinkedHashSet<ConfigEx>();

    public Set<ConfigEx> getChildren() {
        return children;
    }

    public void addChild(ConfigEx child) {
        this.children.add(child);
    }

    public Scope getNotdefined() {
        return notdefined;
    }

    public void addNotdefined(Scope notdefined) {
        this.notdefined.add(notdefined);
    }

    protected Scope notdefined = new Scope();


    protected List<Map<String, String>> params = null;


    public String getUniqueId() {
        return UniqueId;
    }

    protected void setUniqueId() {
        StringBuilder result = new StringBuilder();
        result.append(generateBaseUniqueId(id,path));

        //Теперь нужно понять, сколько у нас одноименных экшнов/компонент в файле
        XPathExpression elExpr = XPathFactory.instance().compile("scope/"+element.getName()+"[@id='"+id+"']");
        List<Element> els =  elExpr.evaluate(element.getDocument().getRootElement());

        //Если их несколько, то к уникальному ID дописываем скоп
        if(els.size() > 1){
           result.append("[");
           result.append(getScope().toString());
           result.append("]");
        }

        UniqueId = result.toString();
    }

    protected String UniqueId;


    protected String link;
    public String getLink(){
        if(link == null){
            //Генерируем по UNIQUE_ID
            link = new StringBuilder().append(id).append("_").append(getUniqueId().hashCode()).toString();
        }
        return link;
    }

    public Map<Object, Object> getTemplateDataModel(){
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put(element.getName(), this);
        return map;
    }

    public Map<String, String> getChildElementMap(String name){
        Element childEl;
        if((childEl = element.getChild(name)) != null){
            return getElementMap(childEl);
        }
        return null;
    }

    public Map<String, String> getElementMap(Element el){
        Map<String, String> result = new LinkedHashMap<String, String>();
        for(Attribute attr: el.getAttributes()){
            if(attr.getName().equals("class"))
                //Чтобы не було конфликта при подстановке в шаблон
                result.put("_class", attr.getValue());
            result.put(attr.getName(), attr.getValue());
        }


        result.put("description", getPreviousComment(el));
        result.put("source", new XMLOutputter().outputString(el).trim());
        result.put("elementname", el.getName());

        return result;
    }


    public List<Map<String, String>> getParams(){
        if(params == null){
            params = new LinkedList<Map<String, String>>();
            if(element.getChild("params") != null){
                for(Element param: element.getChild("params").getChildren("param")){
                    params.add(getElementMap(param));
                }
            }
        };
        return params;
    }

}
