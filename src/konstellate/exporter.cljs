(ns konstellate.exporter
  (:require
    cljsjs.js-yaml
    cljsjs.jszip
    [clojure.data :as data]
    [clojure.string :as string]))

(defn clj->yaml
  [x]
  (.safeDump js/jsyaml (clj->js x)))

(def a {:name "dev"
        :yaml
        {:deployment {:kind "Deployment"
                      :image "wordpress"}
         :replica {:kind "ReplicaSet"}
         :config {:kind "ConfigMap"}}})
  
(def b {:name "prod"
        :yaml
        {:deployment {:kind "Deploment1"
                      :image "wordpress"}
         :replica {:kind "ReplicaSet"}}})

(def c {:name "staging"
        :yaml
        {:deployment {:kind "Deploment1"
                      :image "wordpress"}
         :replica {:kind "ReplicaSet"}}})


(def workspaces [a b c])

; returns [common '(overlay for each workspace)]

(defn extract-common
  [workspace-yaml-list]
  (let [common
        (reduce (fn [c workspace]
                  (nth (data/diff c workspace) 2)) workspace-yaml-list)]
    [common (map (fn [workspace]
                   (second (data/diff common workspace)))
                 workspace-yaml-list)]))

(defn resources->kustomize-files
  [make-path resources]
  (map (fn [[k r]]
         {:file (make-path k r)
          :content r})
       resources))


(defn kustomize-overlay
  [workspace overlay-yaml]
  (let [overlays
        (resources->kustomize-files
          (fn [k r]
            (str "/overlays/" (:name workspace) "/"
                 (or (get-in r [:metadata :name])
                     (get-in workspace [:yaml k :metadata :name])
                     (name k))
                 ".yml"))
          overlay-yaml)]
    (conj overlays
          {:file (str "/overlays/" (:name workspace) "/kustomize.yml")
           :content {:base "../../"
                     :patches (map (fn [overlay]
                                     (last
                                       (string/split (:file overlay) "/")))
                                   overlays)}})))

  
(defn workspaces->kustomize
  [workspaces]
  (let [[common overlays] (extract-common (map :yaml workspaces))
        overlay-dirs (map kustomize-overlay
                          workspaces overlays)
        base (resources->kustomize-files
               (fn [k r] (str "/" (or (get-in r [:metadata :name])
                                      (name k)) ".yml"))
               common)]
    (flatten
      (conj overlay-dirs
            base
            {:file "/kustomize.yml"
             :content {:resources (map (fn [base]
                                         (last (string/split (:file base) "/")))
                                       base)}}))))


(defn zip!
  [desc]
  (let [zip (js/JSZip.)]
    (doseq [file desc]
      (.file zip
             (:file file)
             (clj->yaml (:content file))))
    zip))

(defn save-kustomize!
  [workspaces]
  (let [zip (zip! (workspaces->kustomize workspaces))]
    (->
      (.generateAsync zip #js {:type "blob"})
      (.then #(js/saveAs % "kustomize.zip")))))





