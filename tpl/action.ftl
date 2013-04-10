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
        $('#action_${action.getId()} .xml-source textarea').each(function(){
            if($(this).prev().prop("tagName") != "a"){
                resizeTextarea($(this));
                $('<a href="#" onclick="toggleXML(this); return false;"><span class="ui-icon ui-icon-plus" style="float:left;"></span>источник</a>').insertBefore($(this).parent());

            }
        })
    })
</script>
<div id="action_${action.getId()}">
    <h3>${action.getId()}</h3>

    <p style="font-style: italic; font-family: Tahoma">${action.getDescription()}</p>

    <div class="xml-source" style="display:none">
        <textarea style="width:100%;border:none;" disabled="true">
            ${action.getXMLContent()}
        </textarea>
    </div>

    <p>
        <span>scope:</span>
        <#list action.getScope().getParams().entrySet() as entry >
            <#list entry.value as val>
                ${entry.key}=<a href="#" onclick="clearFilters(); $('#filter_scope_${entry.key}').val('${val}'); doFilters(); return false;">${val}</a>
            </#list>
        </#list>
        <br />
        <span>filepath:</span>${action.getPath()}<br />

        <#if action.getParent()?exists>
            <span>extends: </span> ${action.getParent()}
        </#if>

    </p>

    <#if action.getParams().size() &gt; 0>
        <h4>Параметры</h4>
        <ul>
            <#list action.getParams() as param>
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

    <#if action.getPreconditions().size() &gt; 0 >
        <h4>Preconditions</h4>
        <div class="xml-source" style="display:none">
            <textarea style="width:100%;border:none;" disabled="true">
                ${action.getPreconditionsSource()}
            </textarea>
        </div>
        <ul>
            <#list action.getPreconditions() as precondition>
                <li>
                    <a href="#" onclick="clearFilters();  $('#filter_preconditions').val(this.text); doFilters(); return false;">${precondition._class}</a>
                    <br /> ${precondition.description}
                </li>
            </#list>
        </ul>
    </#if>

    <#if action.getExecution()?exists>
        <h4>Execution</h4>
        <div class="xml-source" style="display:none">
            <textarea style="width:100%;border:none;" disabled="true">
                ${action.getExecution().source}
            </textarea>
        </div>
        <ul>
            <li>
                <a href="#" onclick="clearFilters();  $('#filter_executions').val(this.text); doFilters(); return false;">${action.getExecution()._class}</a>
                <br /> ${action.getExecution().description}
            </li>
        </ul>
    </#if>

    <#if action.getAdditionalChildren().size() &gt; 0 >
        <h4>Дополнительные параметры</h4>
        <ul>
            <#list action.getAdditionalChildren() as param>
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


    <#if action.getModifiers().size() &gt; 0>
    <h4>Модификаторы</h4>
        <ul>
            <#list action.getModifiers() as modifier>
                <li>
                    <span class="param">${modifier.getId()}</span>
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

</div>

