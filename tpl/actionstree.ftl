<#macro drawAction a>
    <li class="closed">
        <span class="file">
            <a href="actions/${a.getLink()}.html" action_id="${a.getId()}" class="actionlink">
                ${a.getScope().toString()}
            </a>
        </span>
        <#list a.getPreconditionsClasses() as precondition>
            <input type="hidden" class="index" precondition="${precondition}" />
        </#list>
        <#if a.getExecutionClass()?exists>
            <input type="hidden" class="index" execution="${a.getExecutionClass()}" />
        </#if>
        <#list a.getScope().getParams().entrySet() as entry >
            <#list entry.value as val>
                <input type="hidden" class="index" scope_${entry.key}="${val}" />
            </#list>
        </#list>


        <#if a.getChildren().size() &gt; 0>
              <ul>
                <#list a.getChildren() as child>
                    <@drawAction a=child />
                </#list>
              </ul>

        </#if>
    </li>
</#macro>

<ul id="tree" class="filetree" style="width: 300px;height: 720px; overflow: auto">
    <#list actions.entrySet() as action >
        <li class="closed">
            <span class="folder">${action.key}</span>
            <ul>
                <#list action.value as a >

                        <#if !a.getParentConfig()?exists || a.getParentId() != a.getId()>
                            <@drawAction a=a />
                        </#if>

                </#list>
            </ul>

            <#if notdefined.get(action.key)?exists>
                <#list notdefined.get(action.key) as notdef >
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