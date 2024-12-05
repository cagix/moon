(ns forge.utils
  (:require [clojure.gdx :as gdx])
  (:import (com.badlogic.gdx.files FileHandle)))

(defn recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (gdx/internal-file folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))
