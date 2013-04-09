/*
* (C) Copyright 1997 i-Teco, CJSK. All Rights reserved.
* i-Teco PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
* Эксклюзивные права 1997 i-Teco, ЗАО.
* Данные исходные коды не могут использоваться и быть изменены
* без официального разрешения компании i-Teco.          
*/
package ru.iteco.xmldoc.old;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.*;
import java.util.*;


/**
 * <h3>Основной класс для гененриции документации по конфигам</h3>
 * <p></p>
 * <p>Author: predtechenskaya (predtechenskaya@i-teco.ru)</p>
 * <p>Date: 19.03.13</p>
 */
public class ConfigParser {

    //Список экшной, сгрупированных по ID (т.к. есть одноименные экшны)
    protected Map<String, List<Action>> actions = new TreeMap<String, List<Action>>();
    //Модификаторы - не самостоятельные экшны, которые модифицируют родителей
    protected Map<String, List<Action>> modifiers = new HashMap<String, List<Action>>();
    //Отнаследованные экшны (ключ - кого наследуют)
    protected Map<String, Action> extendeds = new HashMap<String, Action>();
    //Множество вссех прекондишнов
    protected Set<String> preconditions = new TreeSet<String>();
    //Множество всех классов execution
    protected Set<String> executions = new TreeSet<String>();

    //Множество скопов, сгрупированных по типу (например: location - {inbox_task, folder_incoming,...})
    protected Map<String, Set<String>> scopes = new HashMap<String, Set<String>>();
    //Множество notdefined-экшнов: ID экшна - Скоп
    protected Map<String, Map<String, List<String>>> notdefined = new HashMap<String, Map<String, List<String>>>();




    /**
     * Обработка одного экшна
     *
     * @param action xml экшна
     * @param path путь к файлу с экшном
     * @throws Exception
     */
    protected void processAction(Element action, String path) throws Exception {
        //Создаем объект экшна
        Action currAct = new Action(action, getActionPath(path));
        //Если это - модификатор, обрабатываем отдельно
        if(currAct.getModify() != null){
            //то есть, складываем к модификаторам
            if(!modifiers.containsKey(currAct.getModify()))
                modifiers.put(currAct.getModify(), new ArrayList<Action>());
            modifiers.get(currAct.getModify()).add(currAct);
        }
        //Если ето notdefined
        else if(currAct.isNotdefined()){
            //Если у нас уже есть что-то в notdefined по этому экшну
            if(notdefined.get(currAct.getId()) != null){
                Map<String, List<String>> newScope = currAct.getScope();

                //Идем по ключам уже созданных скопов
                for(String key: notdefined.get(currAct.getId()).keySet()){
                    //Если нашли совпадение с добавляемым
                    if(newScope.containsKey(key)){
                        //мерджим скопы
                        notdefined.get(currAct.getId()).get(key).addAll(newScope.get(key));
                        //удаляем из добавляемого етот ключ
                        newScope.remove(key);
                    }
                }
                //Теперь в новом скопе только новые ключи, добавляем их как есть
                notdefined.get(currAct.getId()).putAll(newScope);
            }
            //Иначе просто кладем скоп в notdefined
            else notdefined.put(currAct.getId(), currAct.getScope());
        }
        //Если это нормальный экшн, то..
        else if(!currAct.isBadActionConfig()){
            //складываем ко всем остальным
            if(!actions.containsKey(currAct.getId()))
                actions.put(currAct.getId(), new ArrayList<Action>());
            actions.get(currAct.getId()).add(currAct);

            //если он отнаследовал, то складываем к экстендам
            if(currAct.getExtend() != null)
                extendeds.put(currAct.getExtend(), currAct);
        }

        //Сохраняем все прекондишны
        preconditions.addAll(currAct.getPreconditions());
        //И execution, если есть
        if(currAct.getExecution() != null)
            executions.add(currAct.getExecution());

        //И еще скопы
        Map<String, List<String>> scs = currAct.getScope();
        for(String sc: scs.keySet()){
            if(!scopes.containsKey(sc))
                scopes.put(sc, new TreeSet<String>());
            for(String sc_val: scs.get(sc)){
                scopes.get(sc).add(sc_val);
            }

        }
    }

    /**
     * Установить все модификаторы для экшнов
     * Вызывается после полного прохода, когда уже все экшны сохранены
     */
    protected void setActionModifiers(){
        for(String id: modifiers.keySet()){
            if(modifiers.get(id) != null){
                for(Action m: modifiers.get(id)){
                    Action modified = getByExtend(m.getModify());
                    if(modified != null)
                        modified.addModifier(m);
                }
            }
        }
    }

    /**
     * Установить все extends для экшнов
     * Вызывается после полного прохода, когда уже все экшны сохранены
     */
    protected void setActionExtendeds(){
        for(String id: extendeds.keySet()){
            Action extended = getByExtend(id);
            if(extended != null){
                extendeds.get(id).setExtended(extended);
                extended.addChild(extendeds.get(id));
            }

        }
    }

    /**
     * По строке extend, то есть, {имя экшна}:{путь} ищем экшн среди сохраненных
     *
     * @param extend строка {имя экшна}:{путь}
     * @return объект экшна или null, если он не был найден
     */
    protected Action getByExtend(String extend){
        String[] parse = Action.parseExtend(extend);
        if(parse == null)
            return null;

        String id = parse[0];
        List<Action> vars = actions.get(id);
        if(vars == null)
            return null;

        for(Action a: vars){
            if(a.getUnique_id().equals(extend))
                return a;
        }
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
            processAction(a, xmlfile.getPath());
            counter++;
        }

        //System.out.print(this.actions);
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
     * Запуск сбора информации по экшнам
     *
     * @throws Exception
     */
    public void run() throws Exception {
        long time_0 = System.currentTimeMillis();

        File[] source_dirs = new File[PATHS.length];
        for(int i=0; i<PATHS.length; i++)
            source_dirs[i] = new File(PATHS[i]);

        processRecursive(source_dirs);
        setActionModifiers();
        setActionExtendeds();


        System.out.println(counter);
        System.out.println((System.currentTimeMillis() - time_0)/1000);
    }

    //Объект конфигурации freemarker
    protected Configuration templateConfig;

    /**
     * Геттер конфигурации freemarker
     *
     * @return объект конфигурации
     * @throws IOException
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
     * @throws IOException
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
     * @throws IOException
     */
    protected Template getBaseTemplate() throws IOException {
        if(htmlBaseTpl == null)
            htmlBaseTpl = getTemplateConfiguration().getTemplate(TPL_BASE);
        return htmlBaseTpl;
    }

    /**
     * олучить мапу версионирования
     *
     * @return мапа версионирования для freemarker
     * @throws IOException
     */
    protected Map<Object, Object> readVersion() throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(VERSION_FILE));

        return (Map<Object, Object>) props;
    }

    /**
     * Мапа параметров для базового шаблона
     *
     * @return мапа параметров
     * @throws IOException
     */
    protected Map<Object, Object> getBaseTemplateMap() throws IOException {
        HashMap<Object, Object> data = new HashMap<Object, Object>();
        data.put("actions", actions);
        data.put("preconditions", preconditions);
        data.put("executions", executions);
        data.put("scopes", scopes);
        data.put("version", readVersion());
        data.put("notdefined", notdefined);

        return data;
    }



    //Папка с шаблонами
    private static final String TPL_FOLDER = "./webapp/webtop-config-documentation/tpl";
    //Имя шаблона для основного HTML
    private static final String TPL_BASE = "base.ftl";
    //Имя шаблона для экшна
    private static final String TPL_ACTION = "action.ftl";
    //Пути, где ищутся экшны
    //TODO сделать опцией
    private static final String[] PATHS = {"./webapp/weboffice", "./iteco_baseweb/webapp/baseweb"};

    //Путь до файла .properties с версионированием
    private static final String VERSION_FILE = "./webapp/webtop-config-documentation/version.properties";

    //Папка с выходным HTML
    private static final String OUT_FOLDER = "./webapp/webtop-config-documentation/out/";
    //Имя папки (в OUT_FOLDER), куда пишутся HTML самих экшнов
    private static final String ACTIONS_FOLDER_NAME = "actions";
    //Имя базового HTML-файла
    private static final String RESULT_FILENAME = "result.html";


    /**
     * Engage !
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ConfigParser instance = new ConfigParser();
        instance.run();

        Writer out = new FileWriter(OUT_FOLDER + RESULT_FILENAME);
        Map<Object, Object> data = instance.getBaseTemplateMap();
        instance.getBaseTemplate().process(data, out);

        for(String k: instance.actions.keySet() ){
            for(Action action: instance.actions.get(k)){
                if(!action.isBadActionConfig() && action.getModify() == null){
                    Writer out_a = new FileWriter(OUT_FOLDER + ACTIONS_FOLDER_NAME+"/"+action.getLink()+".html");
                    instance.getActionTemplate().process(action.getTemplateDataModel(), out_a);
                }

            }
        }

    }


}


