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
import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.*;
import java.util.*;

/**
 * <h3>Генератор HTML-документации по XML-конфигурациям</h3>
 * <p>
 *     Абстрактный generic - класс, предполагающий явное определение T в наследниках.
 *     Основной метод, подлежащий запуску - run(), в котором собирается информация и генерируется HTML.
 * </p>
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

    /**
     * Обработать переданный узел el
     *
     * @param el узел XML документа с конфигурацией
     * @param path путь к XML файлу (абсолютный или относительный - происходит обрезка до вида, используемого в webtop)
     */
    protected void process(Element el, String path){
        //Получаем объект конфигурации из фабричного метода
        Config confEl = ConfigEx.getElement(el, path);
        //Это модификатор
        if(confEl instanceof Modifier){
            //Сохраняем для пост-обработки
            modifiers.add((Modifier)confEl);
            return;
        }
        //Это notdefined - элемент
        if(confEl instanceof Notdefined){
            //Сохраняем для пост-обработки
            String id = ((Notdefined)confEl).getId();
            if(!notdefineds.containsKey(id))
                notdefineds.put(id, new LinkedHashSet<Notdefined>());
            notdefineds.get(id).add((Notdefined) confEl);
            return;
        }
        //Это интересующая нас конфигураций
        if(confEl instanceof ConfigEx){

            //Сохраняем
            T item = (T) confEl;
            if(!items.containsKey(item.getId()))
                items.put(item.getId(), new LinkedList<T>());
            items.get(item.getId()).add(item);

            //Если элемент отнаследован, сохраняем путь к родителю
            if(item.getParent() != null)
                extendeds.add(item);
            //Сохраняем scope в общем скопе
            scopes.add(item.getScope());

            return;
        }
    }

    /**
     * Пост-обработка <br />
     * После завершения сбора информации, обрабатываем сохраненные модификаторы и наследование.
     */
    protected void endProcess(){
        //Идем по модификаторам
        for(Modifier modifier: modifiers){
            T item = getByPath(modifier.getModify());
            //Нашли, кого модифицировали
            if(item != null)
                //Добавляем ему модификатор
                item.addModifier(modifier);
        }

        //По наследникам
        for(T child: extendeds){
            T parent = getByPath(child.getParent());
            //Нашли родителя
            if(parent != null){
                //Родителю добавляем ребенка
                parent.addChild(child);
                //а ребенку - устанавливаем родителя
                child.setParentConfig(parent);
            }
        }
    }

    /**
     * Получить объект T из сохраненным по пути (как в webtop)
     *
     * @param path webtop-путь (id:path)
     * @return объект конфигурации или null, если не найдено
     */
    protected T getByPath(String path){
        //ID искомого конфига
        String id = Config.parseIdFromPath(path);
        //Все конфиги с этим ID
        List<T> lst = items.get(id);
        //Ничего нет, выходим
        if(lst == null)
            return null;

        //Перебираем конфиги
        for(T item: lst){
            //Нашли нужный
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

    /**
     * Получить мапу параметров для freemarker.
     * @return мапу параметров
     */
    protected abstract Map<Object, Object> getBaseTemplateMap();

    /**
     * Имя элемента: action|component
     * @return
     */
    protected abstract String getElementName();

    /**
     * Запуск сбора информации по экшнам
     *
     * @throws Exception
     */
    public void run() throws Exception {

        //Читаем конфигурацию с параметрами запуска
        Properties props = new Properties();
        props.load(new InputStreamReader(new FileInputStream(CONFIG_FILE), "UTF-8"));
        conf = (Map<Object, Object>) props;

        //Создаем объект шаблона freemarker, чтобы он сразу выкинул эксепшны, если конфигурация невалидна
        setTplHelper(new TemplateHelper(conf));

        //Область поиска
        String[] paths = StringUtils.split((String) conf.get("SEARCH_FOLDERS"), ",");
        if(paths.length == 0)
            throw new Exception("Область поиска не задана. Проверьте параметры запуска: SEARCH_FOLDERS");

        //Преобразуем пути в директории
        File[] source_dirs = new File[paths.length];
        for(int i=0; i<paths.length; i++)
            source_dirs[i] = new File(paths[i]);

        //Запуск сбора информации
        processRecursive(source_dirs);
        //Пост-обработка
        endProcess();

        //Собираем HTML
        TemplateHelper tplHelper = getTplHelper();
        //Базовый HTML
        tplHelper.processBase(getBaseTemplateMap());
        //HTML описаний
        for(String k: items.keySet() ){
            for(T item: items.get(k)){
                tplHelper.processItem(item);
            }
        }
    }

    //Файл с параметрами запуска
    protected String CONFIG_FILE = "./config.properties";
    //мапа конфигурации
    protected Map<Object, Object> conf = new LinkedHashMap<Object,Object>();


    protected TemplateHelper getTplHelper() {
        return tplHelper;
    }

    protected void setTplHelper(TemplateHelper tplHelper) {
        this.tplHelper = tplHelper;
    }

    protected TemplateHelper tplHelper;

    class TemplateHelper{

        //Папка с шаблонами
        protected String TPL_FOLDER;
        //Папка с выходным HTML
        protected String OUT_FOLDER;
        //Имя шаблона для основного HTML
        protected String TPL_BASE;
        //Имя шаблона для экшна
        protected String TPL_ITEM;
        //Имя папки (в OUT_FOLDER), куда пишутся HTML самих экшнов
        protected String ITEMS_FOLDER_NAME;
        //Имя базового HTML-файла
        protected String RESULT_FILENAME;

        public TemplateHelper(Map<Object, Object> params) throws Exception {
            if(!params.containsKey("TPL_FOLDER"))
                throw new Exception("Не задан путь до папки с шаблонами");
            if(!params.containsKey("OUT_FOLDER"))
                throw new Exception("Не задан путь до папки генерации результата");

            TPL_FOLDER = (String) params.get("TPL_FOLDER");
            OUT_FOLDER = (String) params.get("OUT_FOLDER");

            if(getElementName().equals("action")){
                if(!params.containsKey("ACTIONS_TREE_TEMPLATE"))
                    throw new Exception("Не задан шаблон дерева экшнов");
                if(!params.containsKey("ACTION_TEMPLATE"))
                    throw new Exception("Не задан шаблон описания экшна");
                if(!params.containsKey("OUT_ACTIONS_FILENAME"))
                    throw new Exception("Не задано имя html-файла результата");
                if(!params.containsKey("OUT_ACTIONS_FOLDER"))
                    throw new Exception("Не задано имя папки для описания экшнов");

                TPL_BASE = (String) params.get("ACTIONS_TREE_TEMPLATE");
                TPL_ITEM = (String) params.get("ACTION_TEMPLATE");
                RESULT_FILENAME = (String) params.get("OUT_ACTIONS_FILENAME");
                ITEMS_FOLDER_NAME = (String) params.get("OUT_ACTIONS_FOLDER");
            }
            else if(getElementName().equals("component")){
                if(!params.containsKey("COMPONENTS_TREE_TEMPLATE"))
                    throw new Exception("Не задан шаблон дерева компонентов");
                if(!params.containsKey("COMPONENT_TEMPLATE"))
                    throw new Exception("Не задан шаблон описания компонента");
                if(!params.containsKey("OUT_COMPONENTS_FILENAME"))
                    throw new Exception("Не задано имя html-файла результата");
                if(!params.containsKey("OUT_COMPONENTS_FOLDER"))
                    throw new Exception("Не задано имя папки для описания компонентов");

                TPL_BASE = (String) params.get("COMPONENTS_TREE_TEMPLATE");
                TPL_ITEM = (String) params.get("COMPONENT_TEMPLATE");
                RESULT_FILENAME = (String) params.get("OUT_COMPONENTS_FILENAME");
                ITEMS_FOLDER_NAME = (String) params.get("OUT_COMPONENTS_FOLDER");
            }
            else{
                throw new Exception("Неизвестный тип конфигурации "+getElementName());
            }

            this.params = params;
        }

        protected Map<Object, Object> params;

        public void processBase(Map<Object, Object> data) throws IOException, TemplateException {
            Writer out = new FileWriter(OUT_FOLDER + RESULT_FILENAME);

            //Информация о версии генератора
            data.put("generator", readVersion());
            //Информация о проекте
            data.put("project", params);

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

          //Путь до файла .properties с версионированием
          protected String VERSION_FILE = "./version.properties";

           /**
            * Получить мапу версионирования
            *
            * @return мапа версионирования для freemarker
            * @throws java.io.IOException
            */
           protected Map<Object, Object> readVersion() throws IOException {
               Properties props = new Properties();
               props.load(new InputStreamReader(new FileInputStream(VERSION_FILE), "UTF-8"));
               return (Map<Object, Object>) props;
           }
    }


}
