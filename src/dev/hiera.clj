(ns dev.hiera
  (:require [hiera.main :as hiera]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import [java.io File]))

(defn locked-files [^File dir]
  (->> (file-seq dir)
       (filter #(.isFile ^File %))
       (remove #(.canWrite ^File %))))

(defn file->ns [^File f]
  (let [path (.getPath f)
        rel  (second (re-find #"src[/\\](.*)" path))
        no-ext (str/replace rel #"\.(clj|cljc|cljs)$" "")]
    (-> no-ext
        (str/replace #"[\\/]" ".")   ;; / or \ → .
        (str/replace "_" "-"))))     ;; _ → -

(defn locked-namespaces []
  (sort (map file->ns (locked-files (File. "src")))))

(comment
 (clojure.pprint/pprint
  (mapv symbol (locked-namespaces))))

(def finished-namespaces
  '[clojure.java.awt.taskbar
    com.badlogic.gdx.input.buttons
    com.badlogic.gdx.input.keys
    com.badlogic.gdx.maps.map-properties
    com.badlogic.gdx.math.circle
    com.badlogic.gdx.math.intersector
    com.badlogic.gdx.math.rectangle
    com.badlogic.gdx.math.vector3
    com.badlogic.gdx.utils.align
    com.badlogic.gdx.utils.shared-library-loader
    org.lwjgl.system.configuration
    space.earlygrey.shape-drawer])

(def ignore-for-now
  '[cdq.ui.dev-menu
    gdl ])

(comment

 ; java heap space 512m required
 (hiera/graph
  {:sources #{"src"}
   :output "target/hiera"
   :layout :horizontal
   :external false
   :ignore (set/union (set finished-namespaces)
                      (set ignore-for-now))
   })

 )
