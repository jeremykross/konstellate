(ns konstellate.core
  (:require
    recurrent.core
    recurrent.drivers.dom
    [konstellate.components :as components]
    [ulmus.signal :as ulmus]))

(def initial-state
  {:gensym {:canonical {:name "Foo"
                        :yaml {:gensym {}}}
            :edited {:name "Foo"
                     :yaml  {:gensym {}}}}})

(defn Main
  [props sources]
  (let [c (components/WorkspacePanel {} 
                                     {:recurrent/dom-$ (:recurrent/dom-$ sources)
                                      :workspaces-$ (ulmus/signal-of {:foo {:name "Foo"
                                                                            :dirty? false}
                                                                      :bar {:name "Bar"
                                                                            :dirty? false}})})]
    c))

(defn start!
  []
  (recurrent.core/start!
    Main
    {}
    {:recurrent/dom-$ (recurrent.drivers.dom/for-id! "app")}))
