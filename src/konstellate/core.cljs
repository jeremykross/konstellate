(ns konstellate.core
  (:require
    recurrent.core
    recurrent.drivers.rum
    [clojure.string :as string]
    [konstellate.components :as components]
    [konstellate.editor.core :as editor]
    [konstellate.exporter :as exporter]
    [konstellate.graffle.core :as graffle]
    [konstellate.resource-desc :as desc]
    [recurrent.state :as state]
    [ulmus.signal :as ulmus]))

(comment def initial-state
  {:preferences {}
   :selected-nodes #{}
   :workspaces
   {:gensym {:canonical {:name "Foo"
                         :yaml {:gensym-a {}}}
             :edited {:name "Foo"
                      :yaml  {:gensym-b {:foo "bar"}}}}
    :gensym1 {:edited {:name "Bar"}}}})

(def initial-state {:workspaces {}})


(defn Main
  [props sources]
  (let [import-$ (ulmus/signal)
        definitions-$ (ulmus/map (fn [swagger-text]
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
                 {:pos-$ (ulmus/signal-of {:top "80px" :right "32px"})
                  :open?-$ (ulmus/reduce not false ((:recurrent/dom-$ sources) ".more" "click"))
                  :items-$ (ulmus/signal-of ["Export To Yaml" "Export To Kustomize" "Export To Helm"])}))

        workspaces-$
        (ulmus/map 
          (fn [state]
            (into (sorted-map)
                  (map (fn [[id workspace]]
                         [id {:name (get-in workspace [:edited :name])
                              :yaml (get-in workspace [:edited :yaml])
                              :selected-nodes (get-in workspace [:edited :selected-nodes])
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
                                                                       {} 
                                                                       (assoc 
                                                                         (select-keys sources [:recurrent/dom-$ :recurrent/state-$])
                                                                         :selected-nodes-$ 
                                                                         (ulmus/map
                                                                           (fn [state]
                                                                             (get-in state
                                                                                     [:workspaces
                                                                                      k
                                                                                      :edited
                                                                                      :selected-nodes]))
                                                                           (:recurrent/state-$ sources))))])
                                                          gained))))
                                          {}
                                          (ulmus/distinct
                                            (ulmus/map #(map keys %)
                                                       (ulmus/changed-keys workspaces-$))))

        workspace-list
        (components/WorkspaceList
          {} 
          {:recurrent/dom-$ (:recurrent/dom-$ sources)
           :workspaces-$ workspaces-$})

        selected-workspace-$ (ulmus/map
                               #(apply get %)
                               (ulmus/zip workspaces-$
                                          (:selected-$ workspace-list)))
        selected-graffle-$
        (ulmus/distinct
          (ulmus/map
            #(apply get %) (ulmus/zip workspace-graffle-$ (:selected-$ workspace-list))))

        kind-picker
        (editor/KindPicker
          {} 
          (assoc
            (select-keys sources
                         [:recurrent/dom-$
                          :swagger-$])
            :definitions-$ definitions-$))

        editor-$
        (ulmus/merge
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
          (ulmus/map (fn [edit-id]
                       (let [path [:workspaces
                                   @(:selected-$ workspace-list)
                                   :edited
                                   :yaml
                                   edit-id]
                             r (get-in @(:recurrent/state-$ sources) path)]
                         (println "PATH:" path)
                         (println "R:" r)
                       ((state/isolate editor/Editor path)
                        {:kind 
                         (str 
                           "io.k8s.api."
                           (cond 
                             (string/includes? (:apiVersion r) "apps")
                             "apps.v1"
                             (string/includes? (:apiVersion r) "rbac")
                             "rbac.v1"
                             :else
                             (str "core." (:apiVersion r)))
                           "."
                           (:kind r))
                         :initial-value r}
                        (assoc
                          (select-keys sources [:recurrent/dom-$
                                                :recurrent/state-$
                                                :swagger-$])
                          :definitions-$ definitions-$))))
                     (ulmus/sample-on
                       (ulmus/map
                         first
                         (ulmus/pickmap :selected-nodes-$ selected-graffle-$))
                       ((:recurrent/dom-$ sources) ".edit-resource" "click"))))

        side-panel
        (components/SidePanel
          {}
          {:dom-$ 
           (ulmus/map (fn [workspace-list-dom]
                        [:div {:class "workspaces"}
                         [:h4 {} "Workspaces"]
                         workspace-list-dom
                         [:div {:class "add-workspace"}
                          [:div {} "Add Workspace"]
                          [:icon {:class "material-icons"}
                           "add"]]])
                      (:recurrent/dom-$ workspace-list))
           :recurrent/dom-$ (:recurrent/dom-$ sources)})

        info-panel-open?-$
           (ulmus/map 
             #(not (empty? %))
             (ulmus/pickmap :selected-nodes-$ selected-graffle-$))

        info-panel
        (components/InfoPanel
          {}
          {:dom-$ (ulmus/map (fn [resource]
                               (let [description (desc/deployment resource)]
                                 (println description)
                                 `[:div {}
                                   [:div {:class "heading"}
                                    [:h3 {} ~(get-in resource [:metadata :name])]
                                    [:div {:class "edit-resource"} "Edit"]]
                                   [:div {:class "info"}
                                    ~(map (fn [{:keys [label value]}]
                                            [:div {:key label}
                                             [:h4 {} label]
                                             [:div {} 
                                              (if (empty? (str value))
                                                "--"
                                                (str value))]])
                                          description)]]))
                             (ulmus/map #(first (vals %))
                                        (ulmus/pickmap :selected-resources-$ selected-graffle-$)))
           :open?-$ info-panel-open?-$
           :recurrent/dom-$ (:recurrent/dom-$ sources)})]

    (ulmus/subscribe!
      (ulmus/merge
        ((:recurrent/dom-$ sources) ".workspace-label" "dragstart")
        ((:recurrent/dom-$ sources) ".workspace-label-resource" "dragstart"))
      (fn [e]
        (.stopPropagation e)
        (.setData (.-dataTransfer e)
                  "text" 
                  (.getAttribute (.-currentTarget e) "data-id"))))

    (ulmus/subscribe!
      ((:recurrent/dom-$ sources) ".graffle" "dragover")
      (fn [e]
        (.preventDefault e)))

    (ulmus/subscribe!
      ((:recurrent/dom-$ sources) ".floating-menu-item.Export-Kustomize" "click")
      (fn []
        (exporter/save-kustomize!
          (map (fn [[k workspace]]
                 (:edited workspace))
               (:workspaces
                 @(:recurrent/state-$ sources))))))

    (ulmus/subscribe!
      ((:recurrent/dom-$ sources) ".floating-menu-item.Export-Helm" "click")
      (fn []
        (exporter/save-helm!
          (map (fn [[k workspace]]
                 (:edited workspace))
               (:workspaces
                 @(:recurrent/state-$ sources))))))

    (ulmus/subscribe!
      ((:recurrent/dom-$ sources) ".import" "click")
      (fn []
        (.click (.getElementById js/document "import-file-input"))))

    (ulmus/subscribe!
      ((:recurrent/dom-$ sources) "#import-file-input" "change")
      (fn [e]
        (let [reader (js/FileReader.)
              files (.-files (.-target e))
              file (.item files 0)]
          (set! (.-onload reader)
                (fn [load-evt]
                  (let [workspace (keyword (gensym))]
                    (.safeLoadAll js/jsyaml (.-result (.-target load-evt))
                                  (fn [js-yaml]
                                    (let [yaml (js->clj js-yaml :keywordize-keys true)]
                                      (ulmus/>! import-$
                                                {:name (first (string/split (.-name file) "."))
                                                 :workspace-id workspace
                                                 :yaml (assoc {} (keyword (gensym)) yaml)})))))))
          (.readAsText reader file))))

    {:swagger-$ (ulmus/signal-of [:get])
     :state-$ (:recurrent/state-$ sources)
     :recurrent/state-$ (ulmus/merge
                          (ulmus/signal-of (fn [] initial-state))
                          (ulmus/pickmap :recurrent/state-$ editor-$)
                          (ulmus/map (fn [import-data]
                                       (fn [state]
                                         (-> state
                                             (assoc-in
                                               [:workspaces
                                                (:workspace-id import-data)
                                                :edited
                                                :name]
                                               (or 
                                                 (:name import-data)
                                                 "Imported Workspace"))
                                             (update-in
                                               [:workspaces
                                                (:workspace-id import-data)
                                                :edited
                                                :yaml]
                                               merge (:yaml import-data)))))
                                     import-$)
                          (ulmus/map (fn [selected]
                                       (fn [state]
                                         (assoc-in state
                                                [:workspaces
                                                 @(:selected-$ workspace-list)
                                                 :edited
                                                 :selected-nodes]
                                                selected)))
                                     (ulmus/distinct
                                       (ulmus/pickmap :selected-nodes-$ selected-graffle-$)))
                          (ulmus/map (fn [e]
                                       (fn [state]
                                         (let [id (keyword (.getAttribute (.-currentTarget e) "data-id"))]
                                           (assoc-in state
                                                  [:workspaces
                                                   @(:selected-$ workspace-list)
                                                   :edited
                                                   :selected-nodes]
                                                  #{id}))))
                                     ((:recurrent/dom-$ sources) ".workspace-label-resource" "click"))
                          (ulmus/map (fn [e]
                                       (fn [state]
                                         (let [from-id (keyword (.getData (.-dataTransfer e) "text"))
                                               src-yaml
                                               (into {}
                                                     (map (fn [[k v]] [k (with-meta v {:konstellate/antecedant from-id})])
                                                          (get-in state [:workspaces
                                                                         from-id
                                                                         :edited
                                                                         :yaml])))]
                                           (update-in state
                                                      [:workspaces
                                                       @(:selected-$ workspace-list)
                                                       :edited
                                                       :yaml]
                                                      #(merge % src-yaml)))))
                                     ((:recurrent/dom-$ sources) ".graffle" "drop"))
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
                      (ulmus/merge
                        ((:recurrent/dom-$ sources) ".edit-resource" "click")
                        (:selected-$ kind-picker)))
           (ulmus/map (constantly :workspace)
                      (ulmus/pickmap :save-$ editor-$))))
       {:workspace
        (ulmus/map
          (fn [[title-bar-dom side-panel-dom info-panel-dom menu-dom info-panel-open? workspace graffle]]
            [:div {:class "main"}
             [:input {:id "import-file-input" :type "file" :style {:display "none"}}]
             [:div {:class (str "action-button add-resource " (if info-panel-open? "panel-open"))} "+"]
             title-bar-dom
             menu-dom
             [:div {:class "main-content"}
              side-panel-dom
              [:div {:class "graffle" :key (:name workspace)}
               [:h4 {:class "workspace-title"} (get workspace :name)]
               graffle]
              info-panel-dom]])
          (ulmus/distinct
            (ulmus/zip (:recurrent/dom-$ title-bar)
                       (:recurrent/dom-$ side-panel)
                       (:recurrent/dom-$ info-panel)
                       (:recurrent/dom-$ menu)
                       info-panel-open?-$
                       selected-workspace-$
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
       :recurrent/dom-$ (recurrent.drivers.rum/create! "app")})))

(set! (.-onerror js/window) #(println %))

(.addEventListener js/document "DOMContentLoaded" start!)
