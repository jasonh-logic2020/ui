(ns ui.main
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ["react" :as react]
   ["@material-ui/core" :refer [ThemeProvider createMuiTheme]]
   [goog.object :as gobj]
   [ui.views.locations :as locations]
   [ui.routes :as routes]
   [ui.components.wrapper :as wrapper]))


;;; Config

(def debug?
  ^boolean goog.DEBUG)


(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

;;; DB

(def default-db
  {:modal-data {:name "should never appear"}
   :add-open? false
   :edit-open? false
   :delete-open? false
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


;;; Subs

;;; Styles

;;; Views

(defn main-shell [{:keys [router classes]}]
  (let [current-route @(rf/subscribe [:current-route])]
    [:> ThemeProvider {:theme (createMuiTheme
                               (clj->js {:palette {:type "light"}
                                         :status {:danger "red"}}))}
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
