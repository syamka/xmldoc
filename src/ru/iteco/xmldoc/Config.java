/*
* (C) Copyright 1997 i-Teco, CJSK. All Rights reserved.
* i-Teco PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
* Эксклюзивные права 1997 i-Teco, ЗАО.
* Данные исходные коды не могут использоваться и быть изменены
* без официального разрешения компании i-Teco.          
*/
package ru.iteco.xmldoc;

import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 03.04.13</p>
 */
public abstract class Config {

    protected Element element;

    protected Scope scope;

    public Scope getScope() {
        return scope;
    }

    public String getPath() {
        return path;
    }

    protected String path;

    public void init(Element element, String path){
        this.element = element;
        this.path = path.replace("\\","/").substring(path.lastIndexOf("webapp") + 6);
        initScope();
    };

    public String getXMLContent(){
        return new XMLOutputter().outputString(element).trim();
    }

    public static String parseIdFromPath(String path){
        String[] couples = path.split(":");
        if(couples.length == 2)
            return couples[0];
        return "";
    }

    public static String generateBaseUniqueId(String id, String path){
        return new StringBuilder().append(id).append(":").append(path).toString();
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
    protected static String getPreviousComment(Element element){
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

    public String getElementSource(String name){
        Element child = element.getChild(name);
        if(child != null)
            return new XMLOutputter().outputString(child).trim();
        return "";
    }

    protected void initScope(){
        scope = new Scope(element);
    }



}
