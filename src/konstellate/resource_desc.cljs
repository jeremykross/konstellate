(ns konstellate.resource-desc
  (:require [clojure.string :as string]))

(defn general
  [r]
  [{:label "Kind"
    :value (:kind r)}
   {:label "Labels"
    :value (string/join "\n" 
                        (map (fn [[k v]]
                               (str (name k) " : " v))
                             (get-in r [:metadata :labels])))}])

(defn deployment
  [r]
  (into
    (general r)
    [{:label "Replicas"
      :value (get-in r [:spec :replicas])}]))
