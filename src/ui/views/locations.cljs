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

(defn styles [^js/Mui.Theme theme]
  (clj->js
   {:popover {:pointerEvents "none"}
    :hoverable {"&.hover" {:backgroundColor "red !important"}}
    :paper {:marginTop (.spacing theme 1)
            :display "flex"
            :flexDirection "column"
            :alignItems "left"}
    :hide {:display "none"}
    :good {:style {:color "Green"}}
    :bad {:color "Red"}
    :avatar {:margin (.spacing theme 1)
             :backgroundColor (.. theme -palette -secondary -main)}
    :form {:marginTop (.spacing theme 1)}
    :button {:margin (.spacing theme 1)}
    :submit {:margin (.spacing theme 8)}}))

(def with-styles (mui/withStyles styles))


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
 :add-open?
 (fn-traced [db _]
            (:add-open? db)))

(rf/reg-sub
 :edit-open?
 (fn-traced [db _]
            (:edit-open? db)))

(rf/reg-sub
 :delete-open?
 (fn-traced [db _]
            (:delete-open? db)))

;;; Events

;; --

(rf/reg-event-db
 ::open-delete-dialog
 (fn-traced [db _]
            (assoc db :delete-open? true)))

(rf/reg-event-db
 ::close-delete-dialog
 (fn-traced [db _]
            (assoc db :delete-open? false)))

;; --

(rf/reg-event-fx
 ::delete-location
 (fn-traced [{db :db} the-data]
            (prn "delete-location" the-data)
            {:db (assoc db :modal-data (last the-data))
             :dispatch [::open-delete-dialog]}))

(rf/reg-event-fx
 ::delete-submit-handler
 (fn-traced [{db :db} [ig {:keys [values path] :as props}]]
            (prn "delete-submit-handler props" props)
            (prn "delete-submit-handler" (:values props))
            {:db (fork/set-submitting db :form true)
             :dispatch [::delete-local props]}))

(rf/reg-event-fx
 ::delete-local
 (fn-traced [{db :db} [_ {:keys [values reset dirty path] :as props}]]
            (prn "delete-local props" props)
            (prn "delete-local" values)
            {:db (assoc db :locations
                        (filter #(not= % {:name (get values "name")
                                          :url (get values "url")
                                          :policy (get values "policy")
                                          :status (get values "status")
                                          :ts (get values "ts")
                                          :id (get values "id")})
                                (:locations db)))
             :dispatch [::delete-resolved-form props]}))

(rf/reg-event-fx
 ::delete-resolved-form
 (fn-traced [{db :db} [_ {:keys [values path ] :as props}]]
            (prn "resolved-form" values)
            {:db (fork/set-submitting db path false)
             :dispatch [::close-delete-dialog]}))

(rf/reg-event-fx
 :cancel-delete
 (fn-traced [{db :db} [ig {:keys [values path]}]]
            {:db (fork/set-submitting db :form false)
             :dispatch [::close-delete-dialog]}))

;; --

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

#_(rf/reg-event-fx
   ::edit-submit-handler
   (fn-traced [{db :db} [_ {:keys [values reset dirty path] :as props}]]
              (prn "edit-submit-handler props" props)
              (prn "edit-submit-handler" (:values props))
              (reset)
              {:db (fork/set-submitting db path true)
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
             :dispatch [::add-resolved-form props]}))

(rf/reg-event-fx
 ::edit-resolved-form
 (fn-traced [{db :db} [_ {:keys [values path]}]]
            (prn "resolved-form" values)
            {:db (fork/set-submitting db path false)
             :dispatch [::close-edit-dialog]}))

;; --

#_(rf/reg-event-fx
   :add-start
   (fn-traced [{db :db} [_ {:keys [values reset dirty path] :as props}]]
              (prn "add-submit-handler props" props)
              (prn "add-submit-handler" (:values props))
              (reset)
              {:db (fork/set-submitting db path true)
               :dispatch [::add-local props]}))

(rf/reg-event-db
 ::open-add-dialog
 (fn-traced [db _]
            (assoc db :add-open? true)))

(rf/reg-event-db
 ::close-add-dialog
 (fn-traced [db _]
            (assoc db :add-open? false)))

;; --

(rf/reg-event-fx
 ::delete-success
 (fn-traced [{db :db} [_ result]]
            {:db (assoc db :beagle-location result)}))

(rf/reg-event-fx
 ::delete-failure
 (fn-traced [{db :db} [_ result path]]
            {:db (-> db
                     (fork/set-submitting path false)
                     (fork/set-server-message path "'Add location' failed"))}))

;; --


;; --

(rf/reg-event-fx
 :add-loction-success
 (fn-traced [{db :db} [_ result]]
            {:db (assoc db :beagle-location result)}))

(rf/reg-event-fx
 :add-location-failure
 (fn-traced [{db :db} [_ result path]]
            {:db (-> db
                     (fork/set-submitting path false)
                     (fork/set-server-message path "'Add location' failed"))}))

(rf/reg-event-fx
 :cancel-add
 (fn-traced [{db :db} [ig {:keys [values path]}]]
            {:db (fork/set-submitting db :form false)
             :dispatch [::close-add-dialog]}))

(rf/reg-event-fx
 ::add-submit-handler
 (fn-traced [{db :db} [ig {:keys [values path] :as props}]]
            (prn "add-submit-handler props" props)
            (prn "add-submit-handler" (:values props))
            {:db (fork/set-submitting db :form true)
             :dispatch [::add-local props]}))

(rf/reg-event-fx
 ::add-local
 (fn-traced [{:keys [db]} [_ {:keys [values] :as props}]]
            (prn "add-local props" props)
            (prn "add-local" values)
            {:db (assoc db :locations
                        (conj (:locations db) {:name (get values "name")
                                               :url (get values "url")
                                               :policy (get values "policy")
                                               :status [:success]
                                               :ts (t/now)
                                               :id (random-uuid)}))
             :dispatch [::add-resolved-form props]}))

(rf/reg-event-fx
 ::add-resolved-form
 (fn-traced [{db :db} [_ {:keys [values path]}]]
            (prn "resolved-form" values)
            {:db (fork/set-submitting db path false)
             :dispatch [::close-add-dialog]}))


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

(defn delete-dialog-form [{:keys [^js classes]}
                          {:keys [values
                                  props
                                  state
                                  form-id
                                  errors
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
                                        [:add-location/close-dialog])
                     :aria-labelledby :add-location-title}
      [:> mui/DialogTitle {:id :add-location-title} "Confirm"]
      [:> mui/DialogContent
       [:> mui/DialogContentText (str "Are you sure you want to delete "
                                      (values "name"))]
       #_[:div [:pre (with-out-str
                       (cljs.pprint/pprint touched)
                       (cljs.pprint/pprint values)
                       (cljs.pprint/pprint (or errors
                                               submitting?)))]]]
      [:> DialogActions
       [:> Button {:label    "No"
                   :on-blur   handle-blur
                   :color    "primary"
                   :on-click (fn [e]
                               (reset)
                               (rf/dispatch [:cancel-delete]))}
        "No"]
       [:> Button {:type     "submit"
                   :on-blur   handle-blur
                   :class    (.-submit classes)
                   :color    "primary"
                   :disabled (or errors submitting?)
                   :on-click handle-submit
                   :variant  "contained"
                   :label    "Yes"} "Yes"]]]]))

(defn delete-dialog [props]
  [fork/form {:form-id         "delete"
              :initial-values  (w/stringify-keys @(rf/subscribe
                                                   [:modal-data]))
              :validation #(-> (vlad/validate validation %)
                               (vlad/assign-names field-names)
                               (vlad/translate-errors vlad/english-translation))
              :path               :form
              :prevent-default?   true
              :clean-on-unmount?  true
              :props       {:is-open? (rf/subscribe
                                       [:delete-open?])}
              ;; :on-submit-response {400 "client error"
              ;;                      500 "server error"}
              :on-submit #(rf/dispatch [::delete-submit-handler %])}
   (partial delete-dialog-form props)])

(defn add-dialog-form [{:keys [^js classes]}
                       {:keys [values
                               props
                               state
                               form-id
                               dirty
                               errors
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
                                        [::close-add-dialog])
                     :aria-labelledby :add-location-title}
      [:> mui/DialogTitle {:id :add-location-title} "Add Location"]
      [:> mui/DialogContent
       [:> mui/DialogContentText "Enter a Sharepoint Name and URL "
        " to schedule search scans."]
       #_[:div [:pre (with-out-str
                       (cljs.pprint/pprint touched)
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
                    ;;   material ui does something funky with the
                    ;;   event target so a custom handle-change is
                    ;;   required
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
                               (rf/dispatch [:cancel-add]))}
        "Cancel"]
       [:> Button {:type     "submit"
                   :on-blur   handle-blur
                   :class    (.-submit classes)
                   :color    "primary"
                   :disabled (or errors submitting?)
                   :on-click handle-submit
                   :variant  "contained"
                   :label    "Save Location"} "Save"]]]]))

(defn add-dialog [props]
  [fork/form {:form-id         "id"
              :validation #(-> (vlad/validate validation %)
                               (vlad/assign-names field-names)
                               (vlad/translate-errors vlad/english-translation))
              :path               :form
              :prevent-default?   true
              :clean-on-unmount?  true
              :props       {:is-open? (rf/subscribe
                                       [:add-open?])}
              ;; :on-submit-response {400 "client error"
              ;;                      500 "server error"}
              :on-submit #(rf/dispatch [::add-submit-handler %])}
   (partial add-dialog-form props)])

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
                    ;;   material ui does something funky with the
                    ;;   event target so a custom handle-change is
                    ;;   required
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
  [fork/form {:form-id         "edit"
              :initial-values  (w/stringify-keys @(rf/subscribe
                                                   [:modal-data]))
              :validation #(-> (vlad/validate validation %)
                               (vlad/assign-names field-names)
                               (vlad/translate-errors vlad/english-translation))
              :path               :form
              :prevent-default?   true
              :clean-on-unmount?  true
              :props       {:is-open? (rf/subscribe
                                       [:edit-open?])}
              ;; :on-submit-response {400 "client error"
              ;;                      500 "server error"}
              :on-submit #(rf/dispatch [::edit-submit-handler %])}
   (partial edit-dialog-form props)])


(defn location-table [{:keys [^js classes] :as props}]
  (let [locs (rf/subscribe [:locations])]
    (fn []
      [:<>
       [:> Grid {:justify "space-between" :container true}
        [:> Grid {:item true}
         [:> mui/Typography {:component     "h2"
                             :variant       "h6"
                             :color         "primary"
                             :gutter-bottom true}
          "Scan Locations"]]
        [:> Grid {:item true}
         [:> Tooltip {:title "Add Location"}
          [:> Button {:classes    (.-button classes)
                      :style      {:float "right"}
                      :color      "primary"
                      :aria-label "Add Location"
                      :on-click   #(rf/dispatch [::open-add-dialog %])
                      :startIcon  (reagent/create-element icons/AddLocation)}

           "Add"]]
         ]]
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
                  [:> icons/Edit]]]
                [:> Tooltip {:title (str "Delete " name)}
                 [::> IconButton {:aria-label "delete"
                                  :color      "primary"
                                  :class      (.-avatar classes)
                                  :on-click   #(rf/dispatch
                                                [::delete-location loc])}
                  [:> icons/Delete]]]
                ]
               ]]))]] ])))


(defn location-table-widget [kv]
  [:> (with-styles
        (reagent/reactify-component
         (location-table kv)))])

(defn main [{:keys [^js classes]}]
  [:<>
   [add-dialog {:classes classes}]
   [edit-dialog {:classes classes}]
   [delete-dialog {:classes classes}]

   [:> Container {:max-width "lg" :class (.-container classes)}
    [:> Grid {:container true :spacing 3}
     [:> Grid {:item true :xs 12}
      [:> Paper {:max-width "lg"
                 :spacing 2
                 :class (.-paper classes)}
       #_[hover-sample {:classes classes}]
       #_[add-location-button-widget {:classes classes}]
       [location-table {:classes classes}]
       ]]
     [:> Grid {:container true :spacing 3}]]]])
