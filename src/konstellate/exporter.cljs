(ns konstellate.exporter
  (:require
    cljsjs.js-yaml
    cljsjs.jszip
    clojure.walk
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

(defn flat-keys
  ([m ks r]
  (if (map? m)
    (apply merge
      (map (fn [[k v]]
             (flat-keys v (conj ks k) r))
           m))
    (assoc r ks m)))
  ([m] (flat-keys m [] {})))

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


(defn helm-templates
  [resources overrides]
  (let [flat-overrides (map flat-keys overrides)
        override-keys (keys (apply merge flat-overrides))
        file-name (fn [r] (string/lower-case (str (get-in r [:metadata :name]) "-" (:kind r) ".yml")))
        inflate-template (fn [r-id]
                           (fn [acc [path value]]
                             (assoc-in acc path
                                       (if (some #{(into [r-id] path)} override-keys)
                                         (str "{{ Values." (string/join "." (map name path)) " }}")
                                         value))))]


    (concat
      (map (fn [[k r]]
             {:file (str "templates/" (file-name r))
              :content (reduce (inflate-template k)
                               {} (flat-keys r))})
           resources)
      (map (fn [[idx override]]
             {:file (string/lower-case (str "values_" (:workspace-name (meta (get (into [] overrides) idx))) ".yml"))
              :content (reduce (fn [acc [k v]] (assoc acc 
                                                      (string/join "." (map name (rest k))) v))
                               {} override)})

           (remove #(every? empty? (second %) )
                   (map-indexed vector flat-overrides))))))




(defn resource-assoc-fn
  [all-resources]
  (fn [path [k r]]
       [k (assoc-in r
                    path
                    (get-in all-resources (into [k] path)))]))

(defn with-name-kind-fn
  [all-resources]
  (let [resource-assoc (resource-assoc-fn all-resources)]
    #(into {}
           (map (comp
                  (partial resource-assoc [:kind])
                  (partial resource-assoc [:metadata :name]))
                %))))


(defn workspaces->kustomize
  [workspaces]
  (let [base-path (fn [n] (repeat n "../"))
        prefix-path (fn [n] (map (fn [n] (str "overlay/")) (range n)))


        workspace-yaml (map :yaml workspaces)

        all-resources (apply merge workspace-yaml)

        with-name-kind (with-name-kind-fn all-resources)
        
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

    (flatten (concat base-files overlay-files))))

(defn workspaces->helm
  [workspaces]
  (let [workspace-yaml (map :yaml workspaces)
        extractions 
        (extract-depth-common 10 workspace-yaml)
        all-resources (apply merge workspace-yaml)
        index-meta (fn [i overlay]
                     (with-meta overlay
                                {:workspace-name (get-in (into [] workspaces) [i :name])}))
        overlays 
        (map-indexed
          index-meta
          (reduce (fn [acc [_ overlay]]
                    (map merge acc overlay))
                  (repeat (count workspace-yaml) {})
                  extractions))]

    (helm-templates all-resources overlays)))
    
(defn zip!
  [desc]
  (let [zip (js/JSZip.)]
    (doseq [file desc]
      (.file zip
             (:file file)
             (clj->yaml (:content file))))
    zip))

(defn save-zip-as!
  [zip filename]
  (-> (.generateAsync zip #js {:type "blob"})
      (.catch #(println %))
      (.then #(js/saveAs % filename))))


(defn save-kustomize!
  [workspaces]
  (let [kustomize (workspaces->kustomize workspaces)
        zip (zip! kustomize)]
    (save-zip-as! zip "kustomize.zip")))

(defn save-helm!
  [workspaces]
  (let [helm (workspaces->helm workspaces)
        zip (zip! helm)]
    (save-zip-as! zip "helm.zip")))

