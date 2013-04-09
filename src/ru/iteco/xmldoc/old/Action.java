/*
* (C) Copyright 1997 i-Teco, CJSK. All Rights reserved.
* i-Teco PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
* Эксклюзивные права 1997 i-Teco, ЗАО.
* Данные исходные коды не могут использоваться и быть изменены
* без официального разрешения компании i-Teco.          
*/
package ru.iteco.xmldoc.old;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.*;

/**
 * <h3>Класс для сбора информации по экшну</h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 20.03.13</p>
 */
public class Action{

    protected Element element;

    public List<Action> getModifiers() {
        return modifiers;
    }

    protected String id;

    protected String unique_id;

    protected String path;

    protected Map<String, List<String>> scope = null;

    protected String execution;

    protected List<String> preconditions = null;

    protected String extend = null;

    public void setExtended(Action extended) {
        this.extended = extended;
    }

    public Action getExtended() {
        return extended;
    }

    protected Action extended = null;

    protected List<Action> children = new LinkedList<Action>();

    public void addChild(Action child){
        children.add(child);
    }

    public List<Action> getChildren(){
        return children;
    }


    protected List<Action> modifiers = new LinkedList<Action>();

    protected String modify = null;

    //Признак того, что это не конфиг экшна
    protected boolean badActionConfig = false;

    public boolean isBadActionConfig() {
        return badActionConfig;
    }


    public boolean isNotdefined() {
        return notdefined;
    }

    //Признако того, что это - не описание экшна, а всего лишь notdefined.
    protected boolean notdefined = false;


    Action(Element element, String filepath) throws Exception {
        //Элемент XML
        this.element = element;
        //Путь
        path = filepath.replace("\\","/");
        //ID
        id = element.getAttributeValue("id");
        //Если ID==NULL, значит этот экшн модифицирующий
        if(id == null){
            if(element.getAttributeValue("modifies") != null){
                modify = element.getAttributeValue("modifies");
                String[] tmp = parseExtend(modify);
                id = tmp[0];
            }
            //Если это не модифицирующий экшн, то он нам не интересен
            else badActionConfig = true;

            //Больше нас ничего не интересует
            return ;
        }

        //Проверяем, а вдруг это просто notdefined тэг, а не описание экшна
        String notdefinedCheck = element.getAttributeValue("notdefined");
        if((notdefinedCheck != null) &&(notdefinedCheck.equals("true"))){
            //Тогда просто проставляем признак
            notdefined = true;
            //и уходим
            return;
        }

        //Extends, пожалуй, запишем сразу
        extend = element.getAttributeValue("extends");

        //И сгенерируем уникальный ключ
        unique_id = generateUniqueId();
    }

    /**
     * Генерируем уникальный ID
     *
     * @return
     */
    public String generateUniqueId(){
        StringBuilder result = new StringBuilder();
        result.append(generateBaseUniqueId(id,path));

        //Теперь нужно понять, сколько у нас одноименных экшнов в файле
        XPathExpression actionExpr = XPathFactory.instance().compile("scope/action[@id='"+id+"']");
        List<Element> actions =  actionExpr.evaluate(element.getDocument().getRootElement());

        //Если их несколько, то к уникальному ID дописываем скоп
        if(actions.size() > 1){
           result.append("[");
           result.append(getStringScope());
           result.append("]");

        }
        return result.toString();
    }

    public static String generateBaseUniqueId(String id, String path){
        return new StringBuilder().append(id).append(":").append(path).toString();
    }

    private String link;
    public String getLink(){
        if(link == null){
            //Генерируем по UNIQUE_ID
            link = new StringBuilder().append(id).append("_").append(getUnique_id().hashCode()).toString();
        }
        return link;
    }

    public Map<String, List<String>> getScope() {
        if(scope == null){
            scope = new LinkedHashMap<String, List<String>>();

            Element scopeEl = element.getParentElement();
            if(scopeEl.getName() != "scope")
                warn("Where is scope ? Parent el :"+ scopeEl.getName());
            else{
                List<Attribute> scopes = scopeEl.getAttributes();
                for(Attribute sc: scopes){
                    scope.put(sc.getName(), new LinkedList<String>());
                    String tmp_val = sc.getValue();
                    String[] tmp_vals = tmp_val.split(",");
                    for(String v: tmp_vals){
                        scope.get(sc.getName()).add(v.trim());
                    }
                }
            }
        }
        return scope;
    }

    public String getStringScope(){
        List<String> lst = new LinkedList<String>();
        getScope();
        for(String key: scope.keySet()){
            for(String val: scope.get(key)){
                lst.add(new StringBuilder().append(key).append("=").append(val).toString());
            }
        };
        return (lst.size() > 0)?StringUtils.join(lst, SCOPE_DELIMETER): "global";
    }

    public String getXMLContent(){
        return new XMLOutputter().outputString(element).trim();
    }

    public void addModifier(Action modifier){
        modifiers.add(modifier);
    }
    public static String[] parseExtend(String uniqueId){

        String[] couples = uniqueId.split(":");
        if(couples.length == 2)
            return couples;
        return null;
    }


    public Map<Object, Object> getTemplateDataModel(){
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("action", this);
        return map;
    }

    public String getDescription(){
       return getPreviousComment(element);
    }

    /**
     * Получить комментарий перед элементом element.
     * Если комментария нет, возвращаем null.
     *
     * @param element
     * @return
     */
    protected String getPreviousComment(Element element){
        //Родительский элемент по отношению к текущему
        Element parent = element.getParentElement();
        //Индекс текущего элемента в родителе
        int index = parent.indexOf(element);

        //Идем "назад" от текущего элемента, ищем предыдущий элемент-коммент
        for(int i = index-1; i > 0; i--){
            //Следуэщий контент
            Content prev = parent.getContent(i);
            //Если это узел текста - пропускаем, видимо, это пробел/табуляция
            if(prev.getCType().equals(Content.CType.Text))
                continue;
            //Если это комментарий - мы нашли, что искали, возвращаем его
            else if(prev.getCType().equals(Content.CType.Comment))
                return prev.getValue();
            //Иначе считаем, что комментария не было, а мы наткнулись на следующий элемент. Выходим.
            else return "";
        }
        //Дошли до начала родителя и ничего не нашли.
        return "";
    }

    protected void warn(String msg){
        System.out.println(path+" "+"[id="+id+"] "+msg);
    }

    @Override
    public String toString() {
        return getUnique_id()+" extends: "+extend+" preconditions: "+preconditions+" execution: "+execution+"\n";
    }

    public String getExecution() {
        if(execution == null){
            Element execEl = element.getChild("execution");
            if(execEl != null)
                execution = execEl.getAttributeValue("class");
            else execution = "";
        }

        return execution;
    }

    public String getExecutionDescription(){
        Element execEl = element.getChild("execution");
        if(execEl != null)
            return getPreviousComment(execEl);
        return "";
    }

    public String getExecutionSource(){
        Element execEl = element.getChild("execution");
        if(execEl != null)
            return new XMLOutputter().outputString(execEl).trim();
        return "";
    }

    public String getExtend() {
        return extend;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public List<String> getPreconditions() {
        if(preconditions == null){
            preconditions = new LinkedList<String>();
            Element precondsEl = element.getChild("preconditions");
            if(precondsEl != null)
                getPreconditionsRecursive(precondsEl);
        }

        return preconditions;
    }

    protected void getPreconditionsRecursive(Element el){
        List<Element> precons = el.getChildren("precondition");
        if(precons != null){
            for(Element pr: precons){
                preconditions.add(pr.getAttributeValue("class"));
                getPreconditionsRecursive(pr);
            }
        }
    }

    public Map<String, String> getPreconditionsDesc(){
        if(getPreconditions().size() > 0){
            return getPreconditionsDescRecursive(element.getChild("preconditions"));
        }
        return new LinkedHashMap<String, String>();

    }

    protected Map<String, String> getPreconditionsDescRecursive(Element el){
        Map<String, String> result = new LinkedHashMap<String, String>();
        List<Element> precons = el.getChildren("precondition");
        if(precons != null){
            for(Element pr: precons){
                result.put(pr.getAttributeValue("class"), getPreviousComment(pr));
                result.putAll(getPreconditionsDescRecursive(pr));
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

    public List<Map<String, String>> getAdditionalParams(){
        List<Map<String, String>> result = new LinkedList<Map<String, String>>();

        List<Element> allChildren = element.getChildren();
        for(Element el:allChildren){
            if(!Arrays.asList( new String[]{"preconditions", "execution", "params"}).contains(el.getName())){
                Map<String, String> res = new HashMap<String, String>();
                res.put("name", el.getName());
                res.put("description", getPreviousComment(el));
                res.put("source", new XMLOutputter().outputString(el).trim());

                result.add(res);
            }
        }

        return result;
    }


    public List<Map<String, String>>getParams(){
        List<Map<String, String>> result = new LinkedList<Map<String, String>>();
        if(element.getChild("params") != null){
            for(Element param: element.getChild("params").getChildren("param")){
                Map<String, String> res = new HashMap<String, String>();
                res.put("name", param.getAttributeValue("name"));
                res.put("required", param.getAttributeValue("required"));
                res.put("description", getPreviousComment(param));

                result.add(res);
            }
        }
        return result;
    }


    public String getUnique_id() {
        return unique_id;
    }

    public String getShortPath(){
        return path.substring(0, TITLE_CUT_SIZE)+"...";
    }

    public String getModify() {
        return modify;
    }

    public static final String SCOPE_DELIMETER = ";";

    public static final int TITLE_CUT_SIZE = 30;


}
