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
                     ;[:h1 {} "konstellate"]
                     [:icon {:class "material-icons more"} "more_vert"]])})

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
  (let [value-$ (ulmus/map  
                  #(.-value (.-target %))
                  ((:recurrent/dom-$ sources) "input" "keyup"))
        editing?-$ 
        (ulmus/start-with! true
          (ulmus/merge
                       (ulmus/map (constantly true)
                                  ((:recurrent/dom-$ sources) ".label-standard-content" "dblclick"))
                       (ulmus/map (constantly false)
                                  (keyboard/press 13))))

        confirm?-$ 
        (ulmus/merge
          (ulmus/map (constantly true)
                     ((:recurrent/dom-$ sources) ".close" "click"))
          (ulmus/map (constantly false)
                     ((:recurrent/dom-$ sources) ".no" "click")))]
    ; external
    ; name-$
    ; dirty?-$

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
                        (fn [[the-name selected? editing? confirm? dirty?]]
                          [:div {:attributes {:data-id (str (name (:id props)))}
                                 :class "workspace-label"
                                 :draggable (not editing?)}
                           [:div {:class (str "workspace-label-content "
                                              (if confirm? "confirming ")
                                              (if selected? "selected"))}
                            (if (not editing?)
                              [:div {:class "label-standard-content"}
                               (if dirty?
                                 [:icon {:class "material-icons md-18 save"} "save"])
                               [:div {:class "the-name"} the-name]
                               [:icon {:class "material-icons md-18 close"} "close"]]
                              [:div {:class "label-edit-content text-input"}
                               [:input {:autofocus true :type "text"}]])]
                           [:div {:class "confirm"}
                            [:label {} "Sure?"]
                            [:div {:attributes {:data-id (str (name (:id props)))}
                                   :class "btn yes"} "Yes"]
                            [:div {:class "btn no"} "No"]]])
                        (ulmus/zip
                          (:name-$ sources)
                          (ulmus/map (fn [selected-id] (= selected-id (:id props))) (:selected-$ sources))
                          editing?-$
                          confirm?-$
                          (:dirty?-$ sources)))}))

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
                                                 {:name-$
                                                  (ulmus/map
                                                    #(get-in % [k :name]) (:workspaces-$ sources))
                                                  :dirty?-$
                                                  (ulmus/map
                                                    #(get-in % [k :dirty?]) (:workspaces-$ sources))
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
                            [:icon {:class ~(str "material-icons open-arrow " (if open? "open"))} "arrow_back_ios"]
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
