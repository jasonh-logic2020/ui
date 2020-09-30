(ns ui.views.locations
  (:require
   [clojure.string :as str]
   [clojure.walk :as w]
   ;; [cljs.pprint :refer pprint]
   [reagent.core :as reagent]
   [goog.object :as gobj]
   [re-frame.core :as rf]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [arco.core :as arco]
   [fork.re-frame :as fork]
   [fork.reagent :as fork-reagent]
   [vlad.core :as vlad]
   [tick.alpha.api :as t]
   ["react" :as react]
   ["@material-ui/icons" :as icons]
   ["@material-ui/core" :refer [Badge Container Grid Paper FormControl
                                Table TableCell TableRow MenuItem
                                TableHead TableBody Popover InputLabel
                                Link Tooltip Icon IconButton Select
                                Button TextField withStyles Avatar Typography
                                Dialog DialogTitle DialogActions Paper
                                DialogContent DialogContentText] :as mui]))


;;; Subs

(rf/reg-sub
 :modal-data
 (fn-traced [db _]
            (:modal-data db)))

(rf/reg-sub
 :locations
 (fn-traced [db _]
            (:locations db)))

(rf/reg-sub
 :edit-open?
 (fn-traced [db _]
            (:edit-open? db)))

;;; Events


(rf/reg-event-db
 ::open-edit-dialog
 (fn-traced [db _]
            (prn "open edit-dialog")
            (prn "open edit-dialog" (:modal-data db))
            (assoc db :edit-open? true)))

(rf/reg-event-db
 ::close-edit-dialog
 (fn-traced [db _]
            (assoc db :edit-open? false)))

;; --

(rf/reg-event-fx
 ::edit-location
 (fn-traced [{db :db} the-data]
            (prn "edit-location" the-data)
            {:db (assoc db :modal-data (last the-data))
             :dispatch [::open-edit-dialog]}))

(rf/reg-event-fx
 :cancel-edit
 (fn-traced [{db :db} [ig {:keys [values path]}]]
            {:db (fork/set-submitting db :form false)
             :dispatch [::close-edit-dialog]}))

(rf/reg-event-fx
 ::edit-submit-handler
 (fn-traced [{db :db} [ig {:keys [values path] :as props}]]
            (prn "edit-submit-handler props" props)
            (prn "edit-submit-handler" (:values props))
            {:db (fork/set-submitting db :form true)
             :dispatch [::edit-local props]}))

(rf/reg-event-fx
 ::edit-local
 (fn-traced [{:keys [db]} [_ {:keys [values] :as props}]]
            (prn "edit-local props" props)
            (prn "edit-local" values)
            {:db (assoc db :locations
                        (reduce  (fn [x] (if (= (get values "id")
                                               (:id x))
                                          {:name (get values "name")
                                           :url (get values "url")
                                           :policy (get values "policy")
                                           :status [:success]
                                           :ts (t/now)
                                           :id (get values "id")}
                                          x))
                                 (:locations db)))
             :dispatch [::edit-resolved-form props]}))

(rf/reg-event-fx
 ::edit-resolved-form
 (fn-traced [{db :db} [_ {:keys [values path]}]]
            (prn "resolved-form" values)
            {:db (fork/set-submitting db path false)
             :dispatch [::close-edit-dialog]}))

;; --

;; Components

(defn drawer-icon []
  [:> icons/LocationOn])

(def field-names
  {["name"] "Sharepoint Name"
   ["url"]  "Sharepoint URL"
   ["policy"] "Scan Policy"})

(def validation
  (vlad/join (vlad/attr ["name"]
                        (vlad/chain
                         (vlad/present)
                         (vlad/length-in 6 128)))
             (vlad/attr ["url"]
                        (vlad/chain
                         (vlad/present)
                         (vlad/length-over 7)))
             (vlad/attr ["policy"]
                        (vlad/chain
                         (vlad/present)
                         (vlad/one-of #{"Low" "High"})))))

(defn edit-dialog-form [{:keys [^js classes]}
                        {:keys [values
                                props
                                state
                                form-id
                                errors
                                dirty
                                set-values
                                handle-change
                                handle-blur
                                handle-submit
                                reset
                                submitting?
                                touched
                                on-submit-response]}]
  (let [{:keys [is-open?]} props]
    [:form
     {:id        form-id
      :on-submit handle-submit}
     [:> mui/Dialog {:fullWidth       true
                     :open            @is-open?
                     :on-close        #(rf/dispatch
                                        [::close-edit-dialog])
                     :aria-labelledby :edit-location-title}
      [:> mui/DialogTitle {:id :edit-location-title} "Edit Location"]
      [:> mui/DialogContent
       [:> mui/DialogContentText "Edit Location properties "]
       [:div [:pre (with-out-str
                     (cljs.pprint/pprint (touched))
                     (cljs.pprint/pprint dirty)
                     (cljs.pprint/pprint values)
                     (cljs.pprint/pprint (or errors
                                             submitting?)))]]
       [:> TextField {:auto-focus true
                      :required   true
                      :full-width true
                      :margin     "dense"
                      :name       "name"
                      :label      "Name"
                      :type       "text"
                      :helperText (when (touched "name")
                                    (first (get errors (list "name"))))
                      :error      (and (touched "name")
                                       (first (get errors (list "name"))))
                      :value      (values "name")
                      :on-blur    handle-blur
                      :on-change  handle-change}]
       [:> TextField {:required   true
                      :full-width true
                      :helperText (when (touched "url")
                                    (first (get errors (list "url"))))
                      :error      (and (touched "url")
                                       (first (get errors (list "url"))))
                      :value      (values "url")
                      :on-blur    handle-blur
                      :on-change  handle-change
                      :margin     "dense"
                      :name       "url"
                      :label      "URL"
                      :type       "url"}]
       [:> FormControl {:class-name  (.-formControl classes)
                        :margin     "dense"
                        :full-width true
                        :label      "Policy"}
        [:> InputLabel {:id "policy-selecter-label"} "Policy"]
        [:> Select {:label-id  "policy-selecter-label"
                    :id        "policy"
                    :name      "policy"
                    :value     (values "policy")
                    :on-blur   handle-blur
                    :on-change #(swap! state
                                       assoc-in [:values "policy"]
                                       (-> % .-target .-value))
                    }
         [:> MenuItem {:name "none"
                       :value nil} [:em "None"]]
         [:> MenuItem {:name "low"
                       :value "Low"} "Low"]
         [:> MenuItem {:name "high"
                       :value "High"} "High"]]]]
      [:> DialogActions
       [:> Button {:label    "Cancel"
                   :on-blur   handle-blur
                   :color    "primary"
                   :on-click (fn [e]
                               (reset)
                               (rf/dispatch [:cancel-edit]))}
        "Cancel"]
       [:> Button {:type     "submit"
                   :on-blur   handle-blur
                   :class    (.-submit classes)
                   :color    "primary"
                   ;; :disabled (or (not dirty) errors submitting?)
                   :disabled (or errors submitting?)
                   :on-click handle-submit
                   :variant  "contained"
                   :label    "Save Edits"} "Update"]]]]))

(defn edit-dialog [props]
  (when-let [initial-values @(rf/subscribe [:modal-data])]
    [fork/form {:form-id         "edit"
                :initial-values  (w/stringify-keys initial-values)
                :validation
                #(-> (vlad/validate validation %)
                     (vlad/assign-names field-names)
                     (vlad/translate-errors vlad/english-translation))
                :path               :form
                :prevent-default?   true
                :clean-on-unmount?  true
                :props       {:is-open? (rf/subscribe
                                         [:edit-open?])}
                :on-submit #(rf/dispatch [::edit-submit-handler %])}
     (partial edit-dialog-form props)]))

(defn location-table [{:keys [^js classes] :as props}]
  (let [locs (rf/subscribe [:locations])]
    (fn []
      [:> mui/Table {:size "small"}
       [:> mui/TableHead
        [:> mui/TableRow {:class (.-hoverable classes)}
         [:> mui/TableCell "Name"]
         [:> mui/TableCell "URL"]
         [:> mui/TableCell "Policy"]
         [:> mui/TableCell {:align "center"} "Status"]
         [:> mui/TableCell "Last"]
         [:> mui/TableCell " "]]]
       [:> mui/TableBody
        (for [{:keys [id name url policy status ts] :as loc} @locs]
          (let [this-id (str "row-" id)]
            ^{:key this-id}
            [:> mui/TableRow
             {:hover true
              :id    this-id
              :class this-id
              :key   this-id }
             [:> mui/TableCell [:> Link name]]
             [:> mui/TableCell [:> Link url]]
             [:> mui/TableCell policy]
             (if (= :success (first status))
               [:> mui/TableCell {:align "center"
                                  :style {:color "Green"}}
                [:> Icon [:> icons/Check] ]]
               [:> mui/TableCell {:align "center"
                                  :style {:color "Red"}}
                [:> Icon
                 [:> Badge {:badgeContent (second status)
                            :color        "error"}
                  [:> icons/Close]]]])
             [:> mui/TableCell (arco/time-since [ts])]
             [:> mui/TableCell
              [:> mui/Grid {:container true}
               [:> Tooltip {:title (str "Edit " name)}
                [:> IconButton {:aria-label "edit"
                                :color      "primary"
                                :class      (.-avatar classes)
                                :on-click   #(rf/dispatch
                                              [::edit-location loc])}
                 [:> icons/Edit]]]] ]]))]] )))


(defn main [{:keys [^js classes]}]
  [:<>
   [edit-dialog {:classes classes}]

   [:> Container {:max-width "lg" :class (.-container classes)}
    [:> Grid {:container true :spacing 3}
     [:> Grid {:item true :xs 12}
      [:> Paper {:max-width "lg"
                 :spacing 2
                 :class (.-paper classes)}
       [location-table {:classes classes}]
       ]]
     [:> Grid {:container true :spacing 3}]]]])
