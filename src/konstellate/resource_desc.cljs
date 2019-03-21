(ns konstellate.resource-desc
  (:require [clojure.string :as string])
  (:require-macros [konstellate.resource-desc :refer [defdesc]]))

(defmulti describe :kind)

(defn format-kv
  [m]
  (string/join "\n" 
               (map (fn [[k v]]
                      (str (name k) " : " v))
                    m)))

(defn general
  [r]
  {"Kind" (:kind r)
   "Namespace" (or (get-in r [:metadata :namespace]) "default")
   "Labels" (format-kv (get-in r [:metadata :labels]))})

(defmethod describe :default [r] {})

(defdesc Deployment
  [r]
  "Pod Template Labels" (format-kv (get-in r [:spec :template :metadata :labels]))
  "Replicas" (get-in r [:spec :replicas]))



