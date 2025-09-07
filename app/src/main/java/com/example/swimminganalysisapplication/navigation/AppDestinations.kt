package com.example.swimminganalysisapplication.navigation

object AppDestinations {
    const val LOGIN_SCREEN_ROUTE = "login_screen" // New
    const val CREATE_ACCOUNT_SCREEN_ROUTE = "create_account_screen" // New
    const val HOME_SCREEN_ROUTE = "home_screen"
    const val VIDEO_SCREEN_ROUTE = "video_screen"
    const val SWIMMERS_SCREEN_ROUTE = "swimmers_screen"
    const val ADD_SWIMMER_ROUTE = "add_swimmer_screen"
    const val START_POSITION_SETTING_ROUTE = "start_position_setting_screen"
    const val CREATE_PRACTICE_MENU_ROUTE_BASE = "create_practice_menu" // 修正: menuIdなしのベースルート名
    const val CREATE_PRACTICE_MENU_ROUTE = "$CREATE_PRACTICE_MENU_ROUTE_BASE?menuId={menuId}" // 修正: 引数ありルート
    const val VIDEO_TRIM_SETUP_ROUTE = "video_trim_setup_screen"
    const val PRACTICE_LIST_SCREEN_ROUTE = "practice_list_screen" // New route for practice list
    const val MENU_COMMENTS_ROUTE = "menu_comments" // パラメータなしのベースルート
    const val MENU_COMMENTS_WITH_ARG_ROUTE = "menu_comments/{menuId}" // menuIdを引数として取る
    const val DISCOVER_SCREEN_ROUTE = "discover_menus"
    const val FAVORITES_SCREEN_ROUTE = "favorites_screen"
}

