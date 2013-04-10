<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <title></title>
    <meta charset="utf-8" />
    <link rel="stylesheet" href="js/jquery-ui-1.10.2.custom/css/redmond/jquery-ui-1.10.2.custom.min.css"/>
    <link rel="stylesheet" href="js/treeview/jquery.treeview.css" />
    <link rel="stylesheet" href="common.css" />


    <script src="js/jquery-ui-1.10.2.custom/js/jquery-1.9.1.js"></script>
    <script src="js/jquery-ui-1.10.2.custom/js/jquery-ui-1.10.2.custom.min.js"></script>
    <script src="js/treeview/jquery.treeview.js" type="text/javascript"></script>

    <script type="text/javascript">

        dataLists = {
            components: [
                        <#list components.entrySet() as component>
                            "${component.key}",
                        </#list>
                    ],

            scopes: {
                <#list scopes.getParams().entrySet() as scope>
                    ${scope.key}: [
                        <#list scope.value as val>
                            "${val}",
                        </#list>
                    ],
                </#list>
            }
        }
    </script>
    <script src="js/common.js" type="text/javascript"></script>


</head>
<body>
<table id="main">
	<tr>
		<td>
            <div id="filters" style="height: 220px;">
                <table>
                    <tr><td colspan="2"><b>scope</b></td></tr>
                    <#list scopes.getParams().entrySet() as scope>
                        <tr>
                            <td style="text-align:right;">${scope.key}:</td>
                            <td><input id="filter_scope_${scope.key}" type="text" style="float:left"/> <a href="#" class="close" style="display: none;" onclick="$('#filter_scope_${scope.key}').val(''); $(this).hide(); doFilters(); return false;"><span class="ui-icon ui-icon-close" ></span></a></td>
                        </tr>
                    </#list>
                    <tr>
                        <td colspan="2">
                            отображать не скопированные <input type="checkbox" id="show_no_scope" checked="checked" />
                        </td>

                    </tr>
                </table>
            </div>

        <div id="toggle_filters">
            <a href="#" onclick="toggleFilters(); return false;"><span class="ui-icon ui-icon-arrowthick-1-n" style="float:left"></span> <span>свернуть</span></a>
        </div>

            <span style="font-size: 12px;">Показано уникальных конфигураций: </span><span id="visible_count" style="font-size: 12px;"></span>
            <br />
            <input id="filter_components" type="text" style="width:95%"/>
            <#include "componentstree.ftl">
		<td>
		<td style="width:100%">
            <div id="tabs" style="height: 820px">
                <ul>
                    <li><a href="#version">Версия</a></li>
                </ul>
                <div id="version">
                    <#include "version.ftl">
                </div>

            </div>
		</td>
	</tr>
</table>
<div style="display:none" id="tree-control">
    <a href="#" id="tree-collapse"></a>
    <a href="#" id="tree-expand"></a>
    <a href="#" id="tree-toggle"></a>

</div>


</body>
</html>

