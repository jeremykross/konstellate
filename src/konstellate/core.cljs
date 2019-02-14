(ns konstellate.core
  (:require
    recurrent.core
    recurrent.drivers.dom
    recurrent.state
    [konstellate.components :as components]
    [ulmus.signal :as ulmus]))

(def initial-state
  {:workspaces
   {:gensym {:canonical {:name "Foo"
                         :yaml {:gensym {}}}
             :edited {:name "Foo"
                      :yaml  {:gensym {:foo "bar"}}}}
    :gensym1 {:edited {:name "Bar"}}}})

(defn Main
  [props sources]
  (let [title-bar (components/TitleBar {}
                                       {:recurrent/dom-$ (:recurrent/dom-$ sources)})
        workload-list (components/WorkspaceList
                        {} 
                        {:recurrent/dom-$ (:recurrent/dom-$ sources)
                         :workspaces-$ (ulmus/map 
                                         (fn [state]
                                           (into {}
                                             (map (fn [[id workspace]]
                                                    [id {:name (get-in workspace [:edited :name])
                                                         :dirty? (not= (get-in workspace [:canonical :yaml])
                                                                       (get-in workspace [:edited :yaml]))}])
                                                  (:workspaces state))))
                                         (:recurrent/state-$ sources))})
        side-panel (components/SidePanel
                     {}
                     {:dom-$ 
                      (ulmus/map (fn [workload-list-dom]
                                   [:div {:class "workspaces"}
                                    [:h4 "Workspaces"]
                                    workload-list-dom
                                    [:div {:class "add-workspace"}
                                     [:icon {:class "material-icons"}
                                      "add"] 
                                     [:div "Add Workspace"]]])
                      (:recurrent/dom-$ workload-list))
                      :recurrent/dom-$ (:recurrent/dom-$ sources)})]
    {:recurrent/state-$ (ulmus/merge
                          (ulmus/signal-of (fn [] initial-state))
                          (ulmus/map (fn [] 
                                       (fn [state]
                                         (println "Update:" state)
                                         (assoc-in
                                           state
                                           [:workspaces
                                            (keyword (gensym))]
                                           {:edited {:name "New Workspace"}})))
                                     ((:recurrent/dom-$ sources)
                                      ".add-workspace"
                                      "click")))
     :recurrent/dom-$
      (ulmus/map
        (fn [[title-bar-dom side-panel-dom]]
          [:div {:class "main"}
           title-bar-dom
           [:div {:class "main-content"}
            side-panel-dom
            [:div {:class "graffle"}]]])
        (ulmus/zip (:recurrent/dom-$ title-bar)
                   (:recurrent/dom-$ side-panel)))}))

(defn start!
  []
  (recurrent.core/start!
    (recurrent.state/with-state Main)
    {}
    {:recurrent/dom-$ (recurrent.drivers.dom/for-id! "app")}))
