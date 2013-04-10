/**
 * Инициализации
 */
$(document).ready(function(){
    //Табы:
    $("#tabs").tabs({
        closable: true,
        cache:true,
        //Это финт ушами, чтобы работала локальная копия HTML подгрузки. Надо пересмотреть вообще-то
        beforeLoad: function( event, ui ) {
            if(ui.panel.text() == "")
                ui.panel.load(ui.ajaxSettings.url);
        },
        heightStyle: "fill"
        });

    //Обработка клика по ссылке на экшн
    $(document).on("click", ".actionlink", function(){
        openTab($(this).attr("action_id"),$(this).attr("href"));
        return false;
    });

    //Обработка клика по ссылке на экшн
    $(document).on("click", ".componentlink", function(){
        openTab($(this).attr("component_id"),$(this).attr("href"));
        return false;
    });


    //Инициализация дерева
    $("#tree").treeview({
        //Это фейковый элемент управления, чтобы можно было юзать ф-л "свернуть/развернуть" ВСЕ
        control: "#tree-control"
    });

    /**
     * Дальше инициализируем все возвожные автокомплиты
     */

    $('#filter_actions').keyup(function(){
        var value = $(this).val();
        $('#tree > li').each(function(){
            //Есть вхождение
            if($(this).find('>span.folder').text().indexOf(value) != -1){
                $(this).removeClass("hide_by_name");
            }
            else $(this).addClass("hide_by_name");
        });
    })

    $('#filter_components').keyup(function(){
        var value = $(this).val();
        $('#tree > li').each(function(){
            //Есть вхождение
            if($(this).find('>span.folder').text().indexOf(value) != -1){
                $(this).removeClass("hide_by_name");
            }
            else $(this).addClass("hide_by_name");
        });
    })


    //По прекондишнам
    $('#filter_preconditions').autocomplete({
        source: dataLists.preconditions,
        minLength: 0,
        select: function(event, ui){
            search("precondition", ui.item.value);
            $(this).parent().find(".close").show();
        }
    });

    //По экзекьюшнам
    $('#filter_executions').autocomplete({
        source: dataLists.executions,
        minLength: 0,
        select: function(event, ui){
            search("execution", ui.item.value);
            $(this).parent().find(".close").show();
        }
    });

    //По всем скопам
    for(var key in dataLists.scopes){

        $('#filter_scope_'+key).autocomplete({
           source:dataLists.scopes[key],
           minLength: 0,
           select: function(event, ui){
               search_scope($(this).attr('id').replace('filter_scope_',''), ui.item.value);
               $(this).parent().find(".close").show();
           }
       });
    }


    $('#filters .ui-autocomplete-input').keyup(function(event){
        if((event.keyCode == 40) && ($(this).val() == "")){
            $(this).autocomplete("search", "");
        }
    });


    //Очистить все по ESC
    $(document).keyup(function(e) {
      if (e.keyCode == 27) {
          restoreTree();
          clearFilters();
      }
    });

    //Инициализация:
    //скрываем фильтры
    toggleFilters();
    //восстанавливаем дерево
    restoreTree();
    //пересчитываем колисетво показанных экшнов
    recalc_visible();

    //Обработка чекбокса "отображать не скопированные"
    $('#show_no_scope').change(function(){
        doFilters();
    })

})

/**
 * Работа с фильтрами
 */

//Применить ВСЕ фильтры
function doFilters(){
    restoreTree();

    // По скопу
    $('#filters input[id^=filter_scope_]').each(function(){
        if($(this).val() != "")
            search_scope($(this).attr("id").replace("filter_scope_",""), $(this).val());
    });

    //по прекондишнам
    if($('#filter_preconditions').val() != "")
        search("precondition", $('#filter_preconditions').val());
    //Экзекьюшны
    if($('#filter_executions').val() != "")
        search("execution", $('#filter_executions').val());

}

//Поиск по фильтру прекондишн или экзекьюшн
function search(key, value){

    var result = $('#tree > li:not(.search_hide) > ul > li:not(.search_hide) .index['+key+'="'+value+'"]').parent();
    $('#tree > li > ul > li').not(result).addClass("search_hide");
    result.find("span a").addClass("searchresult");
    result.parent().parent().find(".expandable-hitarea").trigger("click");
    hideWasted();
}



//Поиск по скопу key
function search_scope(key, value){
    var parent = get_parent_scope(key, value);
    parent.push(value);

    //Поиск по всем ЕЩЕ ВИДИМЫМ конфигурациям
    $('#tree > li:not(.search_hide) > ul > li:not(.search_hide) .index[scope_'+key+']').each(function(){
        //пропускаем какие-то битые индексы
        if(($(this).attr("scope_"+key) == undefined) || ($(this).attr("scope_"+key) == ""))
            return;

        //Нашли совпадение
        if(parent.indexOf($(this).attr("scope_"+key)) != -1){
            //Присваиваем временный фиктивный класс
            $(this).parent().addClass("search_show");
        }
    });

    //Поиск по notdefined
    $('#tree > li:not(.search_hide) .index[notdefined_scope_'+key+']').each(function(){

        //пропускаем какие-то битые индексы
        if(($(this).attr("notdefined_scope_"+key) == undefined) || ($(this).attr("notdefined_scope_"+key) == ""))
            return;

        //Нашли совпадение
        if(parent.indexOf($(this).attr("notdefined_scope_"+key)) != -1){
            //Присваиваем временный фиктивный класс
            $(this).parent().addClass("search_hide");
        }
    });


    //Если нужно отображать нескопированные
    if($('#show_no_scope').is(":checked")){
        //ищем конфигурации без индексов по этому скопу
        $('#tree > li:not(.search_hide) > ul > li:not(.search_hide)').each(function(){
           if($(this).find('.index[scope_'+key+']').length == 0)
               //Им тоже присваиваем фиктивный класс
               $(this).addClass("search_show");
       })
    }



    //Скрываем ВСЕ, что не надо показывать
    $('#tree > li > ul li:not(.search_show)').addClass('search_hide');
    //Убираем фиктивный класс
    $('#tree > li > ul li.search_show').removeClass('search_show');

    //Пересчитываем, что получилось
    hideWasted();
}

//Скрыть отфильтрованное
function hideWasted(){
    //Скрываем пустые узлы
    $('#tree > li').each(function(){
        if($(this).find("> ul > li:not(.search_hide)").length == 0)
            $(this).addClass('search_hide');
    });

    recalc_visible();
}

//ПЕРЕДЕЛАТЬ получить массив parent-скопов
function get_parent_scope(key, value){
    var result = [];
    if(key == "type"){
        if (["ddt_incoming", "ddt_outcoming", "ddt_protocol", "ddt_payment_request", "ddt_internal_doc", "ddt_ord", "ddt_contract"].indexOf(value) !== -1)
            result.push("ddt_registered");
            result.push("ddt_working_card");
    }
    return result;

}

//Восстановить дерево
function restoreTree(){
    //Скрытые показываем
    $('#tree > li').removeClass("search_hide");
    $('#tree > li > ul > li').removeClass("search_hide");

    $('#tree > li').removeClass("hide_by_name");

    //Выделенные подчищаем
    $('#tree > li > ul > li span a').removeClass("searchresult");

    //Сворачиваемся
    $("#tree-collapse").trigger("click");

    //Пересчитываем выделенные
    recalc_visible();
}

//Очистить фильтры
function clearFilters(){
    $("#filters input:text").val("");
    $("#filters a.close").hide();

}

//Скрыть/показать фильтры
function toggleFilters(){
    var icon = $("#toggle_filters a span:eq(0)");
    var text = $("#toggle_filters a span:eq(1)");

    //скрываем
    if($("#filters").is(":visible")){
        icon.removeClass("ui-icon-arrowthick-1-n");
        icon.addClass("ui-icon-arrowthick-1-s");
        text.text("развернуть");
        $("#filters").hide();
    }
    //показываем
    else{
        icon.removeClass("ui-icon-arrowthick-1-s");
        icon.addClass("ui-icon-arrowthick-1-n");
        text.text("свернуть");
        $("#filters").show();
    }
}

//Пересчитать количество показанных экшнов
function recalc_visible(){
    $('#visible_count').text($('#tree > li:not(.search_hide) > ul > li:not(.search_hide)').length);
}

/**
 * Работа с табами
 *
 */

//Открыть таб
function openTab(title, url){
    if($("#tabs > ul > li a[href='"+url+"']").length == 0)
        addTab(title, url);
    $("#tabs").tabs("option", "active", $("#tabs > ul > li a[href='"+url+"']").parent().index($("#tabs > ul > li")));
}

//Создать таб
function addTab(title, url){

    $("#tabs > ul").append("<li><a href='"+url+"'>"+title+"</a><span style='cursor:pointer; float:left' onclick='removeTab(\""+url+"\")' class='ui-icon ui-icon-circle-close'></span></li>");
    $("#tabs").tabs("refresh");

}

//Удалить таб
function removeTab(url){
    $("#tabs > div:eq("+$("#tabs > ul > li").index($("#tabs > ul > li > a[href='"+url+"']").parent())+")").remove();
    $("#tabs > ul > li > a[href='"+url+"']").parent().remove();

    $("#tabs").tabs("refresh");
}



//Нормализовать высоту текстареа
function resizeTextarea(elem){
    elem.attr("rows",elem.text().split("\n").length - 1);
}