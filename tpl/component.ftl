<script>

    function toggleXML(el){
        if($(el).next().is(':visible')){
            $(el).find(".ui-icon").removeClass("ui-icon-minus");
            $(el).find(".ui-icon").addClass("ui-icon-plus");
        }
        else{
            $(el).find(".ui-icon").removeClass("ui-icon-plus");
            $(el).find(".ui-icon").addClass("ui-icon-minus");
        }
        $(el).next().toggle();
    }

    $(document).ready(function(){
        $('#component_${component.getUniqueId()} .xml-source textarea').each(function(){
            if($(this).prev().prop("tagName") != "a"){
                resizeTextarea($(this));
                $('<a href="#" onclick="toggleXML(this); return false;"><span class="ui-icon ui-icon-plus" style="float:left;"></span>источник</a>').insertBefore($(this).parent());

            }
        })
    })
</script>
<div id="component_${component.getUniqueId()}">
    <h3>${component.getId()}</h3>

    <p style="font-style: italic; font-family: Tahoma">${component.getDescription()}</p>

    <div class="xml-source" style="display:none">
        <textarea style="width:100%;border:none;" disabled="true">
            ${component.getXMLContent()}
        </textarea>
    </div>

    <p>
        <span>scope:</span>
        <#list component.getScope().getParams().entrySet() as entry >
            <#list entry.value as val>
                ${entry.key}=<a href="#" onclick="clearFilters(); $('#filter_scope_${entry.key}').val('${val}'); doFilters(); return false;">${val}</a>
            </#list>
        </#list>
        <br />
        <span>filepath:</span>${component.getPath()}<br />

        <#if component.getParent()?exists>
            <span>extends: </span> ${component.getParent()}
        </#if>

    </p>

    <#if component.getParams().size() &gt; 0>
        <h4>Параметры</h4>
        <ul>
            <#list component.getParams() as param>
                <li style="list-style: circle">
                    <span>${param.name}</span>
                    <#if (param.required?exists) && (param.required == "true")>
                        <b>обязательный</b>
                    </#if><br />
                    <i style="font-family:Tahoma">${param.description}</i>
                </li>

            </#list>

        </ul>
    </#if>

    <#if component.getNlsBundle() != "">
        <h4>NlsBundle</h4>
        <span>${component.getNlsBundle()}</span>
    </#if>

    <#if component.getPagesSource() != "">
        <h4>JSP</h4>
        <div class="xml-source" style="display:none">
            <textarea style="width:100%;border:none;" disabled="true">
                ${component.getPagesSource()}
            </textarea>
        </div>
        <span>start: ${component.getPageStart()}</span>
    </#if>

    <#if component.getComponentClass() != "">
        <h4>Class</h4>
        <span>${component.getComponentClass()}</span>
    </#if>

    <#if component.getAdditionalChildren().size() &gt; 0 >
        <h4>Дополнительные параметры</h4>
        <ul>
            <#list component.getAdditionalChildren() as param>
                <li>
                    <span class="param">${param.elementname}</span>
                    ${param.description} <br />
                    <div class="xml-source" style="display:none">
                        <textarea style="width:100%;border:none;" disabled="true">
                            ${param.source}
                        </textarea>
                    </div>
                </li>
            </#list>
        </ul>
    </#if>


    <#if component.getModifiers().size() &gt; 0>
    <h4>Модификаторы</h4>
        <ul>
            <#list component.getModifiers() as modifier>
                <li>
                    <span class="param">${modifier.getParentId()}</span>
                    ${modifier.getDescription()}  <br />
                    <div class="xml-source" style="display:none">
                        <textarea style="width:100%;border:none;" disabled="true">
                            ${modifier.getXMLContent()}
                        </textarea>
                    </div>
                </li>
            </#list>
        </ul>
    </#if>


    <#if component.getChildren().size() &gt; 0>
        <h4>Наследники</h4>
        <ul>
            <#list component.getChildren() as child>
                <li>
                    <a href="components/${child.getLink()}.html" component_id="${child.getId()}" class="componentlink">${child.getId()}</a>
                </li>
            </#list>
        </ul>

    </#if>

</div>

