(ns clojure.rename
  (:require [clojure.java.io :as io]))

(defn replace-in-file! [file from to]
  (let [content (slurp file)
        new-content (.replaceAll content (java.util.regex.Pattern/quote from) to)]
    (when (not= content new-content)
      (spit file new-content)
      (println "Updated:" (.getPath file)))))

(defn matching-files [patterns]
  (->> patterns
       (mapcat #(file-seq (io/file %)))
       (filter #(.isFile %))))

(comment
 (let [from "gdl.tiled"
       to   "gdl.tiled"
       files (matching-files ["src" "resources" "test"])]
   (doseq [f files]
     (replace-in-file! f from to)))
 )
