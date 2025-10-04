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

(def finished
  '#{
     dev,

     com.badlogic.gdx.math.vector2
     com.badlogic.gdx.utils.align

     com.kotcrab,

     gdl.throwable
     gdl.utils
     gdl.malli
     gdl.math
     gdl.scene2d.actor,
     gdl.graphics.viewport


     }
  )

(comment

 ; java heap space 512m required
 (hiera/graph
  {:sources #{"src"}
   :output "target/hiera"
   :layout :horizontal
   :external false
   ;:ignore #_(set (mapv symbol (locked-namespaces)))
   :ignore finished
   })

 (hiera/graph
  {:sources #{"src"}
   :output "target/hiera"
   :layout :horizontal
   :external false
   :cluster-depth 1})

 )
