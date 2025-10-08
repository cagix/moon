(ns cdq.files
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Files)
           (com.badlogic.gdx.files FileHandle)))

(defn- recursively-search
  "Returns all files in the folder (a file-handle) which match the set of extensions e.g. `#{\"png\" \"bmp\"}`."
  [^FileHandle folder extensions]
  (loop [[^FileHandle file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn search [files {:keys [folder extensions]}]
  (map (fn [path]
         [(str/replace-first path folder "") (Files/.internal files path)])
       (recursively-search (Files/.internal files folder) extensions)))
