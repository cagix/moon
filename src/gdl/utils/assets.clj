(ns gdl.utils.assets
  (:require [clojure.string :as str]
            [gdl.files.file-handle :as fh]))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list folder)
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn search [folder extensions]
  (map #(str/replace-first % (str (fh/path folder) "/") "")
       (recursively-search folder extensions)))
