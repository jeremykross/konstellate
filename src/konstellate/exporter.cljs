(ns konstellate.exporter
  (:require
    cljsjs.js-yaml
    cljsjs.jszip
    [clojure.data :as data]
    [clojure.string :as string]))

(def test-data
  [{:foo {:kind "Deployment"
          :metadata {:name "foo"}
          :bar "baz"}}
   {:foo {:kind "Deployment"
          :metadata {:name "foo"}
          :bar "baz" :x 1 :y 2}}
   {:foo {:kind "Deployment"
          :metadata {:name "foo"}
          :bar "baz" :x 1 :y 2 :z 3}}])

(defn clj->yaml
  [x]
  (.safeDump js/jsyaml (clj->js x)))

(defn extract-common
  ([workspace-yaml-list]
   (let [common
         (reduce (fn [c workspace]
                   (nth (data/diff c workspace) 2)) (remove empty? workspace-yaml-list))]
     [common (map (fn [workspace]
                    (second (data/diff common workspace)))
                  workspace-yaml-list)])))

(defn extract-depth-common
  ([acc n depth workspace-yaml-list]
   (println n ":" workspace-yaml-list)
   (let [extraction (extract-common workspace-yaml-list)]
     (if (or (= n depth)
             (= (second extraction) workspace-yaml-list))
       acc
       (extract-depth-common
         (conj acc extraction)
         (inc n)
         depth
         (second extraction)))))
  ([depth workspace-yaml-list]
   (extract-depth-common [] 0 depth workspace-yaml-list)))

(defn kustomize-overlay
  [base prefix resources patches]
  (let [file-name (fn [[k r]] (str prefix (get-in r [:metadata :name]) ".yml"))
        file-desc (fn [[k r]] {:file (file-name [k r])
                               :content r})]
    (conj
      (concat
        (map file-desc resources)
        (map file-desc patches))
      {:file (str prefix "kustomization.yml")
       :content {:resources (map file-name resources)
                 :patches (map file-name patches)}})))


(defn workspaces->kustomize
  [workspaces]
  (let [base-path (fn [n] (repeat n "../"))
        prefix-path (fn [n] (map (fn [n] (str "base_" n "/")) (range n)))


        workspace-yaml (map :yaml workspaces)

        all-resources (apply merge workspace-yaml)

        associate (fn [path [k r]]
                    [k (assoc-in r
                                 path
                                 (get-in all-resources (into [k] path)))])

        with-name-kind #(into
                          {}
                          (map (comp
                                 (partial associate [:kind])
                                 (partial associate [:metadata :name]))
                               %))

        group-patch-resource
        (fn [established resources] 
          (group-by
            (fn [[k]]
              (if (contains? established k)
                :patch
                :resource))
            resources))

        extractions 
        (extract-depth-common 10 workspace-yaml)

        extractions-indexed (map-indexed vector extractions)

        [_ overlays] (last extractions)

        base-files 
        (reduce
          (fn [acc [n [common]]]
            (let [established (apply merge (take n (map first extractions)))
                  grouped (group-patch-resource established common)]
              (conj acc
                    (kustomize-overlay
                      (string/join
                        (base-path n))
                      (string/join
                        (prefix-path n))
                      (with-name-kind (:resource grouped))
                      (with-name-kind (:patch grouped))))))
          []
          extractions-indexed)

        overlay-files
        (reduce
          (fn [acc [ws overlay]]
            (if (empty? overlay)
              acc
              (let [grouped (group-by (constantly :patch) overlay)]
                (conj acc
                  (kustomize-overlay
                    (string/join
                      (base-path (dec (count extractions))))
                    (str
                      (string/join
                        (prefix-path (dec (count extractions))))
                      "overlay/" (:name ws) "/")
                    (with-name-kind (:resource grouped))
                    (with-name-kind (:patch grouped)))))))
          []
          (map vector
               workspaces
               overlays))]

    (println extractions)

    (flatten (concat base-files overlay-files))))


 
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
  (.log js/console (str workspaces))
  (let [kustomize (workspaces->kustomize workspaces)
        zip (zip! kustomize)]
    (->
      (.generateAsync zip #js {:type "blob"})
      (.catch #(println %))
      (.then #(js/saveAs % "kustomize.zip")))))





