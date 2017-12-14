(ns lein-tools-deps.plugin
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [leiningen.core.project :as p]
            [leiningen.core.main :as lein]))

(require 'clojure.tools.deps.alpha.providers.maven)
(require 'clojure.tools.deps.alpha.providers.git)
(require 'clojure.tools.deps.alpha.providers.local)

(def system-deps (io/file "/usr/local/Cellar/clojure/1.9.0.273/deps.edn"))

(def deps-file (io/file "deps.edn"))

(defn home-deps []
  (io/file (System/getProperty "user.home") ".clojure" deps-file))

(def location->dep-paths
  "Map deps.edn location names to paths"
  {:system system-deps
   :home (home-deps)
   :project deps-file})

(def default-deps [:system :home :project])

(defn canonicalise-dep-refs [dep-refs]
  (->> dep-refs
       (map location->dep-paths)
       (map io/file)))

(defn leinize [[proj coord]]
  [proj (:mvn/version coord)])

(defn resolve-deps [deps]
  (let [all-deps (->> deps
                      (filter #(.exists %)))
        tdeps-map (-> all-deps
                      reader/read-deps
                      (deps/resolve-deps {}))
        
        resolved-deps (deps/resolve-deps tdeps-map {})
        lein-deps-vector (->> resolved-deps
                              (mapv leinize))

        project-deps {:dependencies lein-deps-vector }]

    project-deps))


(defn middleware
  "Inject dependencies from deps.edn files into the
  leiningen :dependencies vector."
  [{deps-files :tools/deps :as project}]
  (if (seq deps-files)
    (->> deps-files
         canonicalise-dep-refs
         resolve-deps
         (merge project))
    project))

(comment
  (resolve-deps (canonicalise-dep-refs default-deps))

  
  
  )