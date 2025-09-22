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

(comment

 ; java heap space 512m required
 (hiera/graph
  {:sources #{"src"}
   :output "target/hiera"
   :layout :horizontal
   :external false
   :ignore (set (mapv symbol (locked-namespaces)))
   })

 )
