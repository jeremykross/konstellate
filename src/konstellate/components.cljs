(ns konstellate.components
  (:require 
    recurrent.drivers.vdom
    [recurrent.core :as recurrent :include-macros true]
    [ulmus.keyboard :as keyboard]
    [ulmus.signal :as ulmus]))

(recurrent/defcomponent TitleBar
  [props sources]
  {:recurrent/dom-$
   (ulmus/signal-of [:div {:class "title-bar"}
                     [:img {:src "images/logo.svg"}]
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
  (let [resource-fn (fn [r] [:div {:class "resource" :draggable true} 
                             [:div {:class "dot"}]
                             (str r)])
        value-$ (ulmus/map  
                  #(.-value (.-target %))
                  ((:recurrent/dom-$ sources) "input" "keypress"))

        editing?-$ 
        (ulmus/start-with! true
          (ulmus/merge
            (ulmus/map (constantly true)
                       ((:recurrent/dom-$ sources) ".label-standard-content" "dblclick"))
            (ulmus/map (constantly false)
                       (keyboard/press 13))))]

    ; external
    ; name-$

    ; own
    ; confirm?-$

    ; out
    ; rename-$
    ; save-$
    ; remove-$

    {:rename-$ 
     (ulmus/map (fn [new-value] {:id (:id props)
                                 :new-value new-value})
                (ulmus/sample-on value-$ (ulmus/filter not editing?-$)))
     :recurrent/dom-$ (ulmus/map
                        (fn [[workspace selected? editing?]]
                          (println workspace)
                          [:div {:attributes {:data-id (str (name (:id props)))}
                                 :class "workspace-label"
                                 :draggable (not editing?)}
                           [:div {:class (str "workspace-label-content "
                                              (if selected? "selected"))}
                            [:div {:class "floating-menu"}
                             [:ol {}
                              [:li {:class "floating-menu-item"} "Rename"]
                              [:li {:class "floating-menu-item"} "Delete"]
                              [:li {:class "floating-menu-item"} "Clone"]]]
                            (if (not editing?)
                              [:div {:class "label-standard-content"}
                               [:div {:class "outer"}
                                 [:img {:class "label-open-arrow" :src "images/down.svg"}]
                                 [:div {:class "the-name"} (:name workspace)]]
                               [:div {:class "inner"}
                                (map resource-fn (map #(get-in % [:metadata :name])
                                                      (vals (:yaml workspace))))]]
                              [:div {:class "label-edit-content text-input"}
                               [:input {:autofocus true :type "text"}]])]])
                        (ulmus/zip
                          (:workspace-$ sources)
                          (ulmus/map (fn [selected-id] (= selected-id (:id props))) (:selected-$ sources))
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
                               {}
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
  (let [open?-$ (ulmus/reduce not false ((:recurrent/dom-$ sources) ".open-arrow" "click"))]
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
