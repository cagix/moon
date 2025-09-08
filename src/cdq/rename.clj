(ns cdq.rename
  (:require [clojure.java.io :as io]))

; vimgrep/ctx\/delta-time/gj src/** resources/*.edn test/**
; cfdo %s/ctx\/delta-time/world\/delta-time/gce | update

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

(defn -main [& _]
  ;; like your vim example:
  ;; replace "world/delta-time" with "world/delta-time"
  ;; in src/**, resources/*.edn, test/**
  (let [from "world/delta-time"
        to   "world/delta-time"
        files (matching-files ["src" "resources" "test"])]
    (doseq [f files]
      (replace-in-file! f from to))))
