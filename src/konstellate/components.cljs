(ns konstellate.components
  (:require 
    recurrent.drivers.dom
    [recurrent.core :as recurrent :include-macros true]
    [ulmus.signal :as ulmus]))

(recurrent/defcomponent TitleBar
  [props sources]
  {:recurrent/dom-$
   (ulmus/signal-of [:div {:class "title-bar"}
                     [:h1 "konstellate"]
                     [:icon {:class "material-icons more"} "more_vert"]])})

(recurrent/defcomponent FloatingMenu
  [props sources]
  (let [selection-$ ((:recurrent/dom-$ sources) "li" "click")]
    {:recurrent/dom-$
     (ulmus/map
       (fn [open?]
         (println open?)
         [:div {:class (str "floating-menu " (if open? "open"))}
          [:ol
           [:li "Foo"]
           [:li "Bar"]]])
       (ulmus/start-with! true
         (ulmus/merge
           (ulmus/map (constantly true) (:open-$ sources))
           (ulmus/map (constantly false) (:close-$ sources))
           (ulmus/map (constantly false) selection-$))))}))

(recurrent/defcomponent WorkspaceLabel
  [props sources]
  (let [confirm?-$ 
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

    {:recurrent/dom-$ (ulmus/map
                        (fn [[the-name confirm? dirty?]]
                          (println confirm?)
                          [:div {:class "workspace-label"}
                           [:div {:class (str "content " (if confirm? "confirming"))}
                            (if dirty?
                              [:icon {:class "material-icons save"} "save"])
                            [:div {:class "the-name"} the-name]
                            [:icon {:class "material-icons close"} "close"]]
                           [:div {:class "confirm"}
                            [:label "Sure?"]
                            [:div {:class "btn yes"} "Yes"]
                            [:div {:class "btn no"} "No"]]])
                        (ulmus/zip
                          (:name-$ sources)
                          confirm?-$
                          (:dirty?-$ sources)))}))

; workspaces-$ {:gensym "Foo"}
; delete-workspace -> gensym
; save-workspace -> gensym

(recurrent/defcomponent WorkspacePanel
  [props sources]
  (let [labels-$ (ulmus/reduce (fn [acc [added removed]]
                                 (merge
                                   acc
                                   (into 
                                     {}
                                     (map (fn [[k v]]
                                            [k (WorkspaceLabel 
                                                 {}
                                                 {:name-$
                                                  (ulmus/map
                                                    #(get-in % [k :name]) (:workspaces-$ sources))
                                                  :dirty?-$
                                                  (ulmus/map
                                                    #(get-in % [k :dirty?]) (:workspaces-$ sources))
                                                  :recurrent/dom-$ (:recurrent/dom-$ sources)})]) added))))
                               {}
                               (ulmus/changed-keys (:workspaces-$ sources)))]
    {:recurrent/dom-$
     (ulmus/map (fn [labels-dom]
                  `[:div {:class "workspace-panel"}
                    ~@labels-dom])
                (ulmus/pickzip :recurrent/dom-$ (ulmus/map vals labels-$)))}))


