(ns konstellate.resource-desc
  (:require [clojure.string :as string])
  (:require-macros [konstellate.resource-desc :refer [defdesc]]))

(defmulti describe :kind)


(defn general
  [r]
  {"Kind" (:kind r)
   "Labels" (string/join "\n" 
                         (map (fn [[k v]]
                                (str (name k) " : " v))
                              (get-in r [:metadata :labels])))})

(defmethod describe :default [r] {})

(defdesc Deployment
  [r]
  "Replicas" (get-in r [:spec :replicas]))



