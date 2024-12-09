(ns anvil.files
  (:require [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as f]))

(defn recursively-search [folder extensions]
  (loop [[file & remaining] (f/list (files/internal folder))
         result []]
    (cond (nil? file)
          result

          (f/directory? file)
          (recur (concat remaining (f/list file)) result)

          (extensions (f/extension file))
          (recur remaining (conj result (f/path file)))

          :else
          (recur remaining result))))
