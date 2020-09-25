(ns ui.main
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [tick.alpha.api :as t]
   ["react" :as react]
   ["@material-ui/core" :refer [ThemeProvider createMuiTheme]]
   ["@material-ui/core" :refer [ThemeProvider createMuiTheme
                                Badge Container Grid Paper FormControl
                                Table TableCell TableRow MenuItem
                                TableHead TableBody Popover InputLabel
                                Link Tooltip Icon IconButton Select
                                Button TextField withStyles Avatar Typography
                                Dialog DialogTitle DialogActions Paper
                                DialogContent DialogContentText] :as mui]
   [goog.object :as gobj]
   [ui.routes :as routes]
   [ui.components.wrapper :as wrapper]))


;;; Config

(def debug?
  ^boolean goog.DEBUG)

(def drawer-width 240)

(defn styles [^js/Mui.Theme theme]
  (clj->js
   {:root {:display "flex"}
    :toolbar {:paddingRight 24} ; keep right padding when drawer closed
    :toolbarIcon (merge {:display "flex"
                         :alignItems "center"
                         :justifyContent "flex-end"
                         :padding "0 8px"}
                        (js->clj (.. theme -mixins -toolbar)))
    :appBar {:zIndex (+ (.. theme -zIndex -drawer) 1)
             :transition
             (.. theme -transitions
                 (create #js ["width" "margin"]
                         #js
                         {:easing
                          (.. theme -transitions -easing -sharp)
                          :duration
                          (.. theme -transitions -duration -leavingScreen)}))}
    :appBarShift {:marginLeft drawer-width
                  :width (str "calc(100% - " drawer-width "px)")
                  :transition
                  (.. theme -transitions
                      (create #js ["width" "margin"]
                              #js
                              {:easing
                               (.. theme -transitions -easing -sharp)
                               :duration
                               (.. theme -transitions -duration
                                   -enteringScreen)}))}
    :menuButton {:marginRight 36}
    :menuButtonHidden {:display "none"}
    :title {:flexGrow 1}
    :drawerPaper {:position "relative"
                  :whiteSpace "nowrap"
                  :width drawer-width
                  :transition
                  (.. theme -transitions
                      (create "width"
                              #js
                              {:easing (.. theme -transitions -easing -sharp)
                               :duration
                               (.. theme -transitions -duration
                                   -enteringScreen)}))}
    :drawerPaperClose {:overflowX "hidden"
                       :transition
                       (.. theme -transitions
                           (create "width"
                                   #js
                                   {:easing (.. theme -transitions -easing
                                                -sharp)
                                    :duration (.. theme -transitions -duration
                                                  -leavingScreen)}))
                       :width (.spacing theme 7)
                       (.breakpoints.up theme "sm") {:width (.spacing theme 9)}}
    :appBarSpacer (.. theme -mixins -toolbar)
    :content {:flexGrow 1
              :height "100vh"
              :overflow "auto"}
    :container {:paddingTop (.spacing theme 4)
                :paddingBottom (.spacing theme 4)}
    :paper {:padding (.spacing theme 4)
            :display "flex"
            :overflow "auto"
            :flexDirection "column"}
    :fixedHeight {:height 240}}))

(def with-styles (withStyles styles))

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

;;; DB

(def default-db
  {:dark-theme? false
   :modal-data {:name "should never appear"}
   :drawer/open? true
   :add-open? false
   :edit-open? false
   :delete-open? false
   :user {:id "204201e3-d5c4-40c3-a672-22b886e81ed2"
          :name "Admin Role"
          :admin true}
   :locations [{:name "Widget One"
                :url "https://sample.org/1"
                :policy "Low"
                :status [:fail 2]
                :ts "2020-05-16T20:04:19Z"
                :id "c8c9794a-0b68-4cc1-93f4-7e1edf68513f"}
               {:name "Widget Two"
                :url "https://sample.org/2"
                :policy "High"
                :status [:success]
                :ts "2020-06-16T20:03:19Z"
                :id "336838ea-fde6-4083-8b17-07c72511eeec"}
               {:name "Widget Three"
                :url "https://sample.org/3"
                :policy "Low"
                :status [:success]
                :ts "2020-07-16T20:04:19Z"
                :id "ccbf18f2-39a1-448a-a71d-bf1cd7366a21"}
               {:name "Widget Four"
                :url "https://sample.org/4"
                :policy "High"
                :status [:fail 2]
                :ts "2020-08-16T20:04:19Z"
                :id "9a6d029d-ab8f-470e-a4e9-1208e15ebdca"}]})


;;; Events

(rf/reg-event-db
 :initialize-db
 (fn-traced [_ _]
            default-db))

(rf/reg-event-db
 :toggle-dark-theme
 (fn-traced [db _]
   (update db :dark-theme? not)))

;;; Subs

(rf/reg-sub
 :db
 (fn [db]
   db))

(rf/reg-sub
 :dark-theme?
 (fn [db]
   (:dark-theme? db)))

(rf/reg-sub
 :errors
 (fn [db]
   (:errors db)))

;;; Styles

(defn custom-theme [dark-theme?]
  (createMuiTheme (clj->js {:palette {:type (if dark-theme? "dark" "light")}
                            :status {:danger "red"}})))

;;; Views

(defn main-shell [{:keys [router classes]}]
  (let [current-route @(rf/subscribe [:current-route])
        dark-theme? @(rf/subscribe [:dark-theme?])]
    [:> ThemeProvider {:theme (custom-theme dark-theme?)}
     [wrapper/page {:router router :current-route current-route}]]))

;;; Core

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (routes/init-routes!)
  (rdom/render [main-shell {:router routes/router}]
               (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
