(ns cdq.files
  (:require [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as fh]
            [clojure.string :as str]))

(defn- recursively-search
  [folder extensions]
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

(defn search [files {:keys [folder extensions]}]
  (map (fn [path]
         (str/replace-first path folder ""))
       (recursively-search (files/internal files folder) extensions)))
