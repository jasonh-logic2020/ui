(ns ui.components.user
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [parse-names.core :refer [parse-name]]
   ["@material-ui/core/styles" :refer [withStyles]]
   ["@material-ui/core/Typography" :default Typography]
   ["@material-ui/core/Button" :default Button]
   ["@material-ui/core/Avatar" :default Avatar]
   ["@material-ui/core/Box" :default Box]
   ["@material-ui/core/List" :default List]
   ["@material-ui/core/ListItemText" :default ListItemText]
   ["@material-ui/core/Link" :default Link]))


(rf/reg-sub
 :user
 (fn [db]
   (:user db)))

(def widget-width 200)

(defn user-styles [^js/Mui.Theme theme]
  (clj->js
   {:root {:display "flex"}
    :userbutton {:padding-top "0px"
                 :padding-bottom "0px"}
    :userdisplaylist {:padding-top "0px"
                      :padding-bottom "0px"}
    :usertextitem {:margin-top "0px"
                   :margin-bottom "0px"}
    :display {:text-transform :capitalize
              :line-height 1.0}
    :avatar {:margin (.spacing theme 1)
             :backgroundColor (.. theme -palette -secondary -main)}
    :role {:text-transform :uppercase
           :line-height 1.0}
    :container {:paddingTop (.spacing theme 4)
                :paddingBottom (.spacing theme 4)}}))

(def with-user-styles (withStyles user-styles))

(defn mui-layout []
  (fn [{:keys [^js classes] :as props}]
    (let [{:keys [id name admin image color]
           :or {color "red"}} @(rf/subscribe [:user])
          parsed (parse-name name)
          initials (str (first (:first-name parsed))
                        (first (:last-name parsed)))]
      [:> Button {:class (.-userbutton classes)
                  :variant "outlined"
                  :color "inherit"
                  :aria-label name}
       [:> Avatar (apply merge {:class (.-avatar classes)
                                :alt name}
                         (when image {:src image})) initials]
       [:> List {:container true
                 :class (.-userdisplaylist classes)}
        [:> ListItemText {:primaryTypographyProps {:variant "h6"
                                                   :color   "textSecondary"
                                                   :align   "left"
                                                   :class (.-display classes)}
                          :primary name
                          :secondaryTypographyProps {:variant "caption"
                                                     :color   "textSecondary"
                                                     :align   "left"
                                                     :class (.-role classes)}
                          :secondary (when admin "Admin")
                          :class (.-usertextitem classes)}]]])))

(defn widget []
  [:> (with-user-styles
        (reagent/reactify-component
         (mui-layout)))])
