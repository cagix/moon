(ns dev.rename
  (:require [clojure.java.io :as io]))

(defn replace-in-file! [^java.io.File file from to]
  (let [content (slurp file)
        new-content (.replaceAll content (java.util.regex.Pattern/quote from) to)]
    (when (not= content new-content)
      (spit file new-content)
      (println "Updated:" (.getPath file)))))

(defn matching-files [patterns]
  (->> patterns
       (mapcat #(file-seq (io/file %)))
       (filter java.io.File/.isFile)))

(comment
 (let [from "com.badlogic.gdx.scenes.scene2d.ui.label"
       to   "com.badlogic.gdx.scenes.scene2d.ui.label"
       files (matching-files ["src" "resources" "test"])]
   (doseq [f files]
     (replace-in-file! f from to)))
 )
