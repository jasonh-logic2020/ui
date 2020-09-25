(ns ui.components.copyright
  (:require
   ["@material-ui/core" :refer [Typography Link Grid]] ))

;;; General components that can be used in multiple views

(defn copyright []
  [:> Typography {:variant "caption"
                  :color   "textSecondary"
                  :display "block"
                  :align   "center"}
   [:> Link {:color "inherit"
             :href  "https://material-ui.com"}
    "Sample App "]
   "©"
   (.getFullYear (js/Date.))
   " "
   [:> Link {:color "inherit"
             :href  "https://sample.org"}
    "Sample Corp."]
   ])
