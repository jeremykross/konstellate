(ns konstellate.components
  (:require 
    recurrent.drivers.rum
    [recurrent.core :as recurrent :include-macros true]
    [ulmus.keyboard :as keyboard]
    [ulmus.signal :as ulmus]))

(recurrent/defcomponent TitleBar
  [props sources]
  {:recurrent/dom-$
   (ulmus/signal-of [:div {:class "title-bar"}
                     [:img {:src "images/logo.svg"}]
                     [:div {:class "import"} "Import"]
                     [:div {:class "more button"} "Export"
                      [:img {:src "images/down.svg"}]]])})

(recurrent/defcomponent FloatingMenu
  [props sources]
  (let [selection-$ ((:recurrent/dom-$ sources) "li" "click")]
    {:recurrent/dom-$
     (ulmus/map
       (fn [[items pos open?]]
         `[:div {:class ~(str "floating-menu " (if open? "open"))
                 :style ~pos}
           [:ol {}
            ~@(map (fn [item] [:li {:class (str "floating-menu-item " item)} item]) items)]])
       (ulmus/zip
         (:items-$ sources)
         (:pos-$ sources)
         (:open?-$ sources)))}))

(recurrent/defcomponent WorkspaceLabel
  [props sources]
  (let [selected?-$ (ulmus/map (fn [selected-id] (= selected-id (:id props))) (:selected-$ sources))

        open?-$ selected?-$

        menu-open?-$ 
        (ulmus/merge
          (ulmus/reduce
            (fn [v e] (.stopPropagation e) true)
            false
            ((:recurrent/dom-$ sources) ".workspace-more" "click"))
          (ulmus/map (constantly false) ((:recurrent/dom-$ sources) :root "click")))

        resource-fn (fn [id selected? r] [:div {:data-id (str (name id))
                                                :class (str "workspace-label-resource " (if selected? "selected"))
                                                :draggable true} 
                                       [:div {:class "dot"}]
                                       (str r)])

        value-$ 
        (ulmus/start-with!
          (:name @(:workspace-$ sources))
          (ulmus/map  
            (fn [e]
              (println "Value:" (.-value (.-target e)))
              (.-value (.-target e)))
            ((:recurrent/dom-$ sources) "input" "input")))


        editing?-$ 
        (ulmus/start-with! true
          (ulmus/merge
            (ulmus/map (constantly true)
                       ((:recurrent/dom-$ sources) ".action-rename" "click"))
            (ulmus/map (constantly false)
                       (keyboard/press 13))))]

    {:rename-$ 
     (ulmus/map (fn [new-value] 
                  (println "new-value:" new-value)
                  {:id (:id props)
                   :new-value new-value})
                (ulmus/sample-on value-$ (ulmus/filter false? editing?-$)))
     :recurrent/dom-$ (ulmus/map
                        (fn [[workspace open? menu-open? selected? editing?]]
                          [:div {:data-id (str (name (:id props)))
                                 :class "workspace-label"
                                 :draggable (not editing?)}
                           [:div {:class (str "workspace-label-content "
                                              (if selected? "selected ")
                                              (if open? "open"))}
                            [:div {:class (str "floating-menu " (if menu-open? "open"))}
                             [:ol {}
                              [:li {:class "floating-menu-item action-rename"} "Rename"]
                              [:li {:class "floating-menu-item action-delete"} "Delete"]]]
                            (if (not editing?)
                              [:div {:class "label-standard-content"}
                               [:div {:class "outer"}
                                 [:img {:class "label-open-arrow" :src "images/down.svg"}]
                                 [:div {:class "the-name"} (:name workspace)]
                                 [:i {:class "material-icons workspace-more"} "more_vert"]]
                               [:div (assoc {:class "inner"}
                                            :style (if open?
                                                     {:height (str (* 40 (count (:yaml workspace))) "px")}))
                                (map #(apply resource-fn %)
                                     (map (fn [[id r]]
                                            [id
                                             (some #{id} (:selected-nodes workspace))
                                             (get-in r [:metadata :name])])
                                          (:yaml workspace)))]]
                              [:div {:class "label-edit-content text-input"}
                               [:div {:class "enter-to-continue"} "Enter"]
                               [:input {:autofocus true
                                        :placeholder "Name Workspace"
                                        :type "text"
                                        :default-value (if (= (:name workspace) "New Workspace") "" (:name workspace))}]])]])
                        (ulmus/zip
                          (:workspace-$ sources)
                          open?-$
                          menu-open?-$
                          selected?-$
                          editing?-$))}))

; workspaces-$ {:gensym "Foo"}
; delete-workspace -> gensym
; save-workspace -> gensym

(recurrent/defcomponent WorkspaceList
  [props sources]
  (let [selected-$ (ulmus/map
                     (fn [e] (keyword (.getAttribute (.-currentTarget e) "data-id")))
                     ((:recurrent/dom-$ sources) ".workspace-label" "click"))
        labels-$ (ulmus/reduce (fn [acc [added removed]]
                                 (println (keys acc))
                                 (as-> acc a
                                   (apply dissoc a (keys removed))
                                   (merge
                                     a
                                     (into 
                                       {}
                                       (map (fn [[k v]]
                                              [k (WorkspaceLabel 
                                                   {:id k}
                                                   {:workspace-$ (ulmus/map #(get % k) (:workspaces-$ sources))
                                                    :selected-$ selected-$
                                                    :recurrent/dom-$ (:recurrent/dom-$ sources)})]) added)))))
                               (sorted-map)
                               (ulmus/changed-keys (:workspaces-$ sources)))
        kw-id (fn [e]
                (.stopPropagation e)
                (keyword (.getAttribute (.-currentTarget e) "data-id")))]
    {:delete-$ (ulmus/map kw-id ((:recurrent/dom-$ sources) ".workspace-label .yes" "click"))
     :selected-$ (ulmus/map
                   kw-id
                   ((:recurrent/dom-$ sources) ".workspace-label" "click"))
     :rename-$ (ulmus/pickmerge :rename-$ (ulmus/map vals labels-$))
     :recurrent/dom-$
     (ulmus/map (fn [labels-dom]
                  `[:div {:class "workload-panel"}
                    ~@labels-dom])
                (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals labels-$)))}))



(recurrent/defcomponent SidePanel
  [props sources]
  (let [open?-$ (ulmus/reduce not true ((:recurrent/dom-$ sources) ".open-arrow" "click"))]
    {:recurrent/dom-$ (ulmus/map
                        (fn [[open? dom]]
                          `[:div {:class ~(str "side-panel " (if open? "open"))}
                            [:img {:class ~(str "open-arrow " (if open? "open")) :src "images/collapse.svg"}]
                            [:div {:class "side-panel-content"}
                              ~dom]])
                        (ulmus/zip
                          open?-$
                          (:dom-$ sources)))}))


(recurrent/defcomponent InfoPanel
  [props sources]
  {:recurrent/dom-$
   (ulmus/map
     (fn [[open? dom]]
       [:div {:class (str "info-panel " (if open? "open"))}
        [:div {:class "info-panel-content"}
         dom]])
     (ulmus/zip
       (:open?-$ sources)
       (:dom-$ sources)))})
