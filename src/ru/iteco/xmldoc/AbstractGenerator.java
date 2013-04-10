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
 * <p>Date: 09.04.13</p>
 */
public abstract class AbstractGenerator<T extends ConfigEx> {

    //Мапа: ID - список конфигураций (экшн/компонент) - сгруппировано по ID
    protected Map<String, List<T>> items = new TreeMap<String, List<T>>();
    //Модификаторы
    protected Set<Modifier> modifiers = new LinkedHashSet<Modifier>();
    //Унаследованные конфигурации
    protected Set<T> extendeds = new LinkedHashSet<T>();
    //Конфигурации notdefined, сгруппированные по ID
    protected Map<String, Set<Notdefined>> notdefineds = new LinkedHashMap<String, Set<Notdefined>>();
    //Множество всех скопов
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
        if(confEl instanceof ConfigEx){
            T item = (T) confEl;
            if(!items.containsKey(item.getId()))
                items.put(item.getId(), new LinkedList<T>());
            items.get(item.getId()).add(item);

            if(item.getParent() != null)
                extendeds.add(item);

            scopes.add(item.getScope());

            return;
        }
    }

    protected void endProcess(){
        for(Modifier modifier: modifiers){
            T item = getByPath(modifier.getModify());
            if(item != null)
                item.addModifier(modifier);
        };

        for(T child: extendeds){
            T parent = getByPath(child.getParent());
            if(parent != null){
                parent.addChild(child);
                child.setParentConfig(parent);
            }
        }
    }

    protected T getByPath(String path){
        String id = Config.parseIdFromPath(path);
        List<T> lst = items.get(id);
        if(lst == null)
            return null;

        for(T item: lst){
            if(item.getUniqueId().equals(path))
                return item;
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

        XPathExpression itemExpr = XPathFactory.instance().compile("scope/" + getElementName());
        List<Element> items = itemExpr.evaluate(root);
        for(Element i: items){
            System.out.println(i.getAttributeValue("id"));
            process(i, xmlfile.getPath());
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

    protected abstract Map<Object, Object> getBaseTemplateMap();

    protected abstract String getElementName();

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

        TemplateHelper tplHelper = getTemplateHelper();
        tplHelper.processBase(getBaseTemplateMap());
        for(String k: items.keySet() ){
            for(T item: items.get(k)){
                tplHelper.processItem(item);
            }
        }
    }

    protected void setTemplateHelper(String tplItem, String tplBase, String itemsFolder, String resultFilename){
        tplHelper = new TemplateHelper(tplItem,tplBase,itemsFolder,resultFilename);
    }

    protected TemplateHelper tplHelper;
    protected TemplateHelper getTemplateHelper(){
        if(tplHelper == null){
            tplHelper = new TemplateHelper();
        }
        return tplHelper;
    }

    protected String[] PATHS = {"./webapp/weboffice", "./iteco_baseweb/webapp/baseweb"};


    class TemplateHelper{

        public TemplateHelper(){};

        public TemplateHelper(String tplItem, String tplBase, String itemsFolder, String resultFilename){
            TPL_ITEM = tplItem;
            TPL_BASE = tplBase;
            ITEMS_FOLDER_NAME = itemsFolder;
            RESULT_FILENAME = resultFilename;
        }

        //Путь до файла .properties с версионированием
        protected String VERSION_FILE = "./webapp/webtop-config-documentation/version.properties";
        //Папка с шаблонами
        protected String TPL_FOLDER = "./webapp/webtop-config-documentation/tpl";
        //Папка с выходным HTML
        protected String OUT_FOLDER = "./webapp/webtop-config-documentation/out/";



        //Имя шаблона для основного HTML
        protected String TPL_BASE = "base.ftl";

        //Имя шаблона для экшна
        protected String TPL_ITEM = "action.ftl";

        //Имя папки (в OUT_FOLDER), куда пишутся HTML самих экшнов
        protected String ITEMS_FOLDER_NAME = "actions";
        //Имя базового HTML-файла
        protected String RESULT_FILENAME = "result.html";

        public void processBase(Map<Object, Object> data) throws IOException, TemplateException {
            Writer out = new FileWriter(OUT_FOLDER + RESULT_FILENAME);
            data.put("version", readVersion());
            getBaseTemplate().process(data, out);
        }

        public void processItem(T item) throws IOException, TemplateException {
            Writer out_a = new FileWriter(OUT_FOLDER + ITEMS_FOLDER_NAME+"/"+item.getLink()+".html");
            getActionTemplate().process(item.getTemplateDataModel(), out_a);
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
                   htmlActionTpl = getTemplateConfiguration().getTemplate(TPL_ITEM);
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
    }


}
