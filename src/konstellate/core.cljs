(ns konstellate.core
  (:require
    recurrent.core
    recurrent.drivers.vdom
    [konstellate.components :as components]
    [konstellate.editor.core :as editor]
    [konstellate.graffle.core :as graffle]
    [recurrent.state :as state]
    [ulmus.signal :as ulmus]))

(comment def initial-state
  {:preferences {}
   :workspaces
   {:gensym {:canonical {:name "Foo"
                         :yaml {:gensym-a {}}}
             :edited {:name "Foo"
                      :yaml  {:gensym-b {:foo "bar"}}}}
    :gensym1 {:edited {:name "Bar"}}}})

(def initial-state {:workspaces {}})

(defn Main
  [props sources]
  (let [definitions-$ (ulmus/map (fn [swagger-text]
                                   (:definitions (js->clj
                                                   (.parse js/JSON swagger-text)
                                                   :keywordize-keys true)))
                                 ((:swagger-$ sources) [:get]))

        title-bar (components/TitleBar {}
                                       {:recurrent/dom-$ (:recurrent/dom-$ sources)})
        menu (components/FloatingMenu
               {}
               (merge
                 (select-keys sources [:recurrent/dom-$])
                 {:pos-$ (ulmus/signal-of {:top "68px" :right "32px"})
                  :open?-$ (ulmus/reduce not false ((:recurrent/dom-$ sources) ".more" "click"))
                  :items-$ (ulmus/signal-of ["New" "Open" "Save" "Export"])}))

        workspaces-$
        (ulmus/map 
          (fn [state]
            (into {}
                  (map (fn [[id workspace]]
                         [id {:name (get-in workspace [:edited :name])
                              :dirty? (not= (get-in workspace [:canonical :yaml])
                                            (get-in workspace [:edited :yaml]))}])
                       (:workspaces state))))
          (:recurrent/state-$ sources))


        workspace-graffle-$ (ulmus/reduce (fn [acc [gained lost]]
                                            (merge acc
                                                   (into 
                                                     {}
                                                     (map (fn [k] [k ((state/isolate graffle/Graffle
                                                                                      [:workspaces k :edited :yaml])
                                                                       {} (select-keys sources [:recurrent/dom-$ :recurrent/state-$]))]) gained))))
                                          {}
                                          (ulmus/distinct
                                            (ulmus/map #(map keys %)
                                                       (ulmus/changed-keys workspaces-$))))

        workspace-list
        (components/WorkspaceList
          {} 
          {:recurrent/dom-$ (:recurrent/dom-$ sources)
           :workspaces-$ workspaces-$})

        selected-graffle-$
        (ulmus/map
          #(apply get %) (ulmus/zip workspace-graffle-$ (:selected-$ workspace-list)))

        kind-picker
        (editor/KindPicker
          {} 
          (assoc
            (select-keys sources
                         [:recurrent/dom-$
                          :swagger-$])
            :definitions-$ definitions-$))

        editor-$
        (ulmus/map (fn [[kind workspace]]
                     ((state/isolate editor/Editor
                                     [:workspaces
                                      workspace
                                      :edited
                                      :yaml
                                      (keyword (gensym))])
                      {:kind (:property kind)}
                      (assoc
                        (select-keys sources [:recurrent/dom-$
                                              :recurrent/state-$
                                              :swagger-$])
                        :definitions-$ definitions-$)))
                   (ulmus/distinct
                     (ulmus/filter
                       #(every? identity %)
                       (ulmus/zip
                         (:selected-$ kind-picker)
                         (ulmus/sample-on
                           (:selected-$ workspace-list)
                           (:selected-$ kind-picker))))))

        side-panel
        (components/SidePanel
          {}
          {:dom-$ 
           (ulmus/map (fn [workspace-list-dom]
                        [:div {:class "workspaces"}
                         [:h4 {} "Workspaces"]
                         workspace-list-dom
                         [:div {:class "add-workspace"}
                          [:icon {:class "material-icons"}
                           "add"] 
                          [:div {} "Add Workspace"]]])
                      (:recurrent/dom-$ workspace-list))
           :recurrent/dom-$ (:recurrent/dom-$ sources)})

        info-panel
        (components/InfoPanel
          {}
          {:dom-$ (ulmus/map (fn [resources]
                               (println "Resources:" resources)
                               `[:div {}
                                 ~@(map (fn [[k v]]
                                          [:div {} (str v)]) resources)])
                             (ulmus/pickmap :selected-resources-$ selected-graffle-$))
           :open?-$ (ulmus/map 
                      #(not (empty? %))
                      (ulmus/pickmap :selected-nodes-$ selected-graffle-$))
           :recurrent/dom-$ (:recurrent/dom-$ sources)})]

    {:swagger-$ (ulmus/signal-of [:get])
     :state-$ (:recurrent/state-$ sources)
     :recurrent/state-$ (ulmus/merge
                          (ulmus/signal-of (fn [] initial-state))
                          (ulmus/pickmap :recurrent/state-$ editor-$)
                          (ulmus/map (fn [name-change]
                                       (fn [state]
                                         (assoc-in state
                                                   [:workspaces
                                                    (:id name-change)
                                                    :edited
                                                    :name]
                                                   (:new-value name-change))))
                                     (:rename-$ workspace-list))
                          (ulmus/map (fn [id]
                                       (fn [state]
                                         (update-in state [:workspaces]
                                                    (fn [workloads]
                                                      (dissoc workloads id)))))
                                     (:delete-$ workspace-list))
                          (ulmus/map (fn [] 
                                       (fn [state]
                                         (assoc-in
                                           state
                                           [:workspaces
                                            (keyword (gensym))]
                                           {:edited {:name "New Workspace"
                                                     :yaml {}}})))
                                     ((:recurrent/dom-$ sources)
                                      ".add-workspace"
                                      "click")))
     :recurrent/dom-$
     (ulmus/choose
       (ulmus/start-with!
         :workspace
         (ulmus/merge
           (ulmus/map (constantly :kind-picker)
                      ((:recurrent/dom-$ sources) ".add-resource" "click"))
           (ulmus/map (constantly :editor)
                      (:selected-$ kind-picker))
           (ulmus/map (constantly :workspace)
                      (ulmus/pickmap :save-$ editor-$))))
       {:workspace
        (ulmus/map
          (fn [[title-bar-dom side-panel-dom info-panel-dom menu-dom graffle]]
            [:div {:class "main"}
             [:div {:class "action-button add-resource"} "+"]
             title-bar-dom
             menu-dom
             [:div {:class "main-content"}
              side-panel-dom
              [:div {:class "graffle"} graffle]
              info-panel-dom]])
          (ulmus/distinct
            (ulmus/zip (:recurrent/dom-$ title-bar)
                       (:recurrent/dom-$ side-panel)
                       (:recurrent/dom-$ info-panel)
                       (:recurrent/dom-$ menu)
                       (ulmus/pickmap :recurrent/dom-$ selected-graffle-$))))
        :editor (ulmus/pickmap :recurrent/dom-$ editor-$)
        :kind-picker (:recurrent/dom-$ kind-picker)})}))

(defn start!
  []
  (let [swagger-path "https://raw.githubusercontent.com/kubernetes/kubernetes/master/api/openapi-spec/swagger.json"] 
    (recurrent.core/start!
      (state/with-state Main)
      {}
      {:swagger-$                                                                      (recurrent.drivers.http/create!                                                   swagger-path {:with-credentials? false}) 
       :recurrent/dom-$ (recurrent.drivers.vdom/for-id! "app")})))

(set! (.-onerror js/window) #(println %))
