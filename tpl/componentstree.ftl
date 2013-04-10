<#macro drawComponent a>
    <li class="closed">
        <span class="file">
            <a href="components/${a.getLink()}.html" component_id="${a.getId()}" class="componentlink">
                ${a.getScope().toString()}
            </a>
        </span>
        <#list a.getScope().getParams().entrySet() as entry >
            <#list entry.value as val>
                <input type="hidden" class="index" scope_${entry.key}="${val}" />
            </#list>
        </#list>


        <#if a.getChildren().size() &gt; 0>
              <ul>
                <#list a.getChildren() as child>
                    <#if a.getId() == child.getId()>
                        <@drawComponent a=child />
                    </#if>
                </#list>
              </ul>

        </#if>
    </li>
</#macro>

<ul id="tree" class="filetree" style="width: 300px;height: 720px; overflow: auto">
    <#list components.entrySet() as component >
        <li class="closed">
            <span class="folder">${component.key}</span>
            <ul>
                <#list component.value as a >
                        <#if !a.getParentConfig()?exists || a.getParentId() != a.getId()>
                            <@drawComponent a=a />
                        </#if>
                </#list>
            </ul>

            <#if notdefined.get(component.key)?exists>
                <#list notdefined.get(component.key) as notdef >
                    <#list notdef.getScope().getParams().entrySet() as scope>
                        <#list scope.value as val>
                            <input type="hidden" class="index" notdefined_scope_${scope.key}="${val}" />
                        </#list>
                    </#list>
                </#list>
            </#if>


        </li>
    </#list>
</ul>