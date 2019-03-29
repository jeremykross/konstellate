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

(defn format-ports
  [ps]
  (string/join "\n\n"
               (map 
                 #(str "Port : " (:port %))
                 ps)))

(defn format-subjects
  [subjects]
  (string/join "\n\n"
               (map #(str "Kind : " (:kind %)
                          "\nName : " (:name %)) subjects)))

(defn format-role-rules
  [rules]
  (string/join "\n\n"
               (map
                 (fn [r]
                   (str (if (:apuGroups r) (str "Api Groups - " (string/join ", " (:apiGroups r)) "\n"))
                        (if (:resources r) (str "Resources - " (string/join ", " (:resources r)) "\n"))
                        (if (:resourceNames r) (str "Resource Names - " (string/join ", " (:resourceNames r)) "\n"))
                        (if (:verbs r) (string/join ", " (map string/upper-case (:verbs r))))))
                 rules)))


(defn general
  [r]
  {"Kind" (:kind r)
   "Name" (get-in r [:metadata :name])
   "Namespace" (or (get-in r [:metadata :namespace]) "default")
   "Labels" (format-kv (get-in r [:metadata :labels]))})

(defn pod-template
  [r]
  {"Pod Template Labels" (format-kv (get-in r [:metadata :labels]))})

(defmethod describe :default [r] (general r))

(defdesc Deployment
  [r]
  (merge
    (pod-template (get-in r [:spec :template]))
    {"Replicas" (get-in r [:spec :replicas])}))

(defdesc Service
  [r]
  {"Ports" (format-ports (get-in r [:spec :ports]))})

(defdesc ConfigMap
  [r]
  {"Data" (format-kv (:data r))})

(defdesc Secret
  [r]
  {"Data" (format-kv (:data r))
   "Type" (:type r)})

(defdesc Role
  [r]
  {"Rules" (format-role-rules (:rules r))})

(defdesc RoleBinding
  [r]
  {"Role Reference" (get-in r [:roleRef :name])
   "Subjects" (format-subjects (:subjects r))})
