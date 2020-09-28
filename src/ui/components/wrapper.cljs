(ns ui.components.wrapper
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.core :as reitit]
   ["@material-ui/core" :refer [CssBaseline withStyles] :as mui]))


;;; Styles


(defn wrapper-styles [^js/Mui.Theme theme]
  )

(def with-wrapper-styles (withStyles wrapper-styles))


(defn wrapper [{:keys [router current-route]}]
  (fn [{:keys [^js classes] :as props}]
    [:div {:class (.-root classes)}
     [:> CssBaseline]

     [:main {:class (.-content classes)}
      (when current-route
        [(-> current-route :data :view) {:classes classes}])
      ]]))

(defn page [{:keys [router current-route]}]
  [:> (with-wrapper-styles
        (reagent/reactify-component
         (wrapper {:router router :current-route current-route})))])
