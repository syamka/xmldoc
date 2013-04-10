/*
* (C) Copyright 1997 i-Teco, CJSK. All Rights reserved.
* i-Teco PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
* Эксклюзивные права 1997 i-Teco, ЗАО.
* Данные исходные коды не могут использоваться и быть изменены
* без официального разрешения компании i-Teco.          
*/
package ru.iteco.xmldoc;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.*;
import java.util.*;

/**
 * <h3></h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 03.04.13</p>
 */
public class Controller {

    protected Map<String, List<ActionConfig>> actions = new TreeMap<String, List<ActionConfig>>();

    protected Set<Modifier> modifiers = new LinkedHashSet<Modifier>();

    protected Set<ActionConfig> extendeds = new LinkedHashSet<ActionConfig>();

    protected Map<String, Set<Notdefined>> notdefineds = new LinkedHashMap<String, Set<Notdefined>>();

    //Множество вссех прекондишнов
    protected Set<String> preconditions = new TreeSet<String>();
    //Множество всех классов execution
    protected Set<String> executions = new TreeSet<String>();

    protected Scope scopes = new Scope();

    protected void process(Element el, String path){
        Config confEl = ConfigEx.getElement(el, path);
        if(confEl instanceof Modifier){
            modifiers.add((Modifier)confEl);
            return;
        }
        if(confEl instanceof Notdefined){
            String id = ((Notdefined)confEl).getId();
            if(!notdefineds.containsKey(id))
                notdefineds.put(id, new LinkedHashSet<Notdefined>());
            notdefineds.get(id).add((Notdefined) confEl);
            return;
        }
        if(confEl instanceof ActionConfig){
            ActionConfig action = (ActionConfig) confEl;
            if(!actions.containsKey(action.getId()))
                actions.put(action.getId(), new LinkedList<ActionConfig>());

            actions.get(action.getId()).add(action);

            if(action.getParent() != null)
                extendeds.add(action);
            if(action.getExecutionClass() != null)
                executions.add(action.getExecutionClass());

            preconditions.addAll(action.getPreconditionsClasses());

            scopes.add(action.getScope());

            return;
        }
    }

    protected void endProcess(){
        for(Modifier modifier: modifiers){
            ActionConfig action = getByPath(modifier.getModify());
            if(action != null)
                action.addModifier(modifier);
        };

        for(ActionConfig child: extendeds){
            ActionConfig parent = getByPath(child.getParent());
            if(parent != null){
                parent.addChild(child);
            }
        }
    }

    protected ActionConfig getByPath(String path){
        String id = Config.parseIdFromPath(path);
        List<ActionConfig> lst = actions.get(id);
        if(lst == null)
            return null;

        for(ActionConfig action: lst){
            if(action.getUniqueId().equals(path))
                return action;
        };
        return null;
    }


    /**
     * По полному пути выберем путь, который используется в extends.
     * То есть, это относительный путь при деплое
     *
     * @param path полный путь
     * @return относительный путь при деплое
     */
    protected String getActionPath(String path){
        return path.substring(path.lastIndexOf("webapp") + 6);
    }


    /**
     * Обработать файл, то есть, пройтись по нему и запустить обработку всех экшнов
     *
     *
     * @param xmlfile файл
     * @throws Exception
     */
    protected void processFile(File xmlfile) throws Exception {

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlfile);
        Element root = doc.getRootElement();

        XPathExpression actionExpr = XPathFactory.instance().compile("scope/action");
        List<Element> actions = actionExpr.evaluate(root);
        for(Element a: actions){
            System.out.println(a.getAttributeValue("id"));
            process(a, xmlfile.getPath());
            counter++;
        }
    }

    //Счетчик, сколько обработали
    private int counter = 0;

    /**
     * Рекурсивная функция обхода файловой структуры. Запускает processFile для всех xml
     *
     * @param dirs массив файлов/директорий поиска
     * @throws Exception
     */
    public void processRecursive(File[] dirs) throws Exception {

        for(File dir: dirs){
            if(dir.isDirectory()){
                processRecursive(dir.listFiles());
            }
            else if(dir.isFile()){
                String path = dir.getPath();
                if(isXmlFile(path)){
                    processFile(dir);
                }
            }
        }
    }

    /**
     * Проверяем, является ли файл XML исходя из расширения
     *
     * @param path путь к файлу
     * @return true если файл XML иначе false
     */
    protected boolean isXmlFile(String path){

        int i = path.lastIndexOf('.');
        if(i > 0)
            return path.substring(i+1).equals("xml");
        return false;
    }

    /**
     * Мапа параметров для базового шаблона
     *
     * @return мапа параметров
     * @throws java.io.IOException
     */
    protected Map<Object, Object> getBaseTemplateMap() {
        HashMap<Object, Object> data = new HashMap<Object, Object>();
        data.put("actions", actions);
        data.put("preconditions", preconditions);
        data.put("executions", executions);
        data.put("scopes", scopes);
        data.put("notdefined", notdefineds);

        return data;
    }

    /**
     * Запуск сбора информации по экшнам
     *
     * @throws Exception
     */
    public void run() throws Exception {

        File[] source_dirs = new File[PATHS.length];
        for(int i=0; i<PATHS.length; i++)
            source_dirs[i] = new File(PATHS[i]);

        processRecursive(source_dirs);
        endProcess();

        TemplateHelper tplHelper = new TemplateHelper();
        tplHelper.processBase(getBaseTemplateMap());
        for(String k: actions.keySet() ){
            for(ActionConfig action: actions.get(k)){
                tplHelper.processAction(action);
            }
        }
    }


    public static void main(String...args) throws Exception {
        new Controller().run();
    }


    //Пути, где ищутся экшны
    //TODO сделать опцией
    private static final String[] PATHS = {"./webapp/weboffice", "./iteco_baseweb/webapp/baseweb"};



}


class TemplateHelper{


    public void processBase(Map<Object, Object> data) throws IOException, TemplateException {
        Writer out = new FileWriter(OUT_FOLDER + RESULT_FILENAME);
        data.put("version", readVersion());
        getBaseTemplate().process(data, out);
    }

    public void processAction(ActionConfig action) throws IOException, TemplateException {
        Writer out_a = new FileWriter(OUT_FOLDER + ACTIONS_FOLDER_NAME+"/"+action.getLink()+".html");
        getActionTemplate().process(action.getTemplateDataModel(), out_a);
    }


   //Объект конфигурации freemarker
    protected Configuration templateConfig;

    /**
     * Геттер конфигурации freemarker
     *
     * @return объект конфигурации
     * @throws java.io.IOException
     */
    protected Configuration getTemplateConfiguration() throws IOException{
        if(templateConfig == null){
            templateConfig = new Configuration();
            templateConfig.setOutputEncoding("UTF-8");
            templateConfig.setDirectoryForTemplateLoading(new File(TPL_FOLDER));
            templateConfig.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
        }
        return templateConfig;
    }

    //объект шаблона freemarker для экшна
    protected Template htmlActionTpl;

    /**
     * Геттер freemarker шаблона экшна
     *
     * @return объект шаблона
     * @throws java.io.IOException
     */
    protected Template getActionTemplate() throws IOException {
        if(htmlActionTpl == null)
            htmlActionTpl = getTemplateConfiguration().getTemplate(TPL_ACTION);
        return htmlActionTpl;
    }

    //объект шаблона freemarker для основного HTML
    protected Template htmlBaseTpl;

    /**
     * Геттер freemarker шаблона основного HTML
     *
     * @return объект шаблона
     * @throws java.io.IOException
     */
    protected Template getBaseTemplate() throws IOException {
        if(htmlBaseTpl == null)
            htmlBaseTpl = getTemplateConfiguration().getTemplate(TPL_BASE);
        return htmlBaseTpl;
    }


    /**
     * Получить мапу версионирования
     *
     * @return мапа версионирования для freemarker
     * @throws java.io.IOException
     */
    protected Map<Object, Object> readVersion() throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(VERSION_FILE));

        return (Map<Object, Object>) props;
    }

    //Путь до файла .properties с версионированием
    private static final String VERSION_FILE = "./webapp/webtop-config-documentation/version.properties";


    //Папка с шаблонами
    private static final String TPL_FOLDER = "./webapp/webtop-config-documentation/tpl";
    //Имя шаблона для основного HTML
    private static final String TPL_BASE = "base.ftl";
    //Имя шаблона для экшна
    private static final String TPL_ACTION = "action.ftl";


    //Папка с выходным HTML
    private static final String OUT_FOLDER = "./webapp/webtop-config-documentation/out/";
    //Имя папки (в OUT_FOLDER), куда пишутся HTML самих экшнов
    private static final String ACTIONS_FOLDER_NAME = "actions";
    //Имя базового HTML-файла
    private static final String RESULT_FILENAME = "result.html";


}
