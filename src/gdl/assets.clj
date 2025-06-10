(ns gdl.assets
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.files FileHandle)))

(defn recursively-search [^FileHandle folder extensions]
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

(defn find-assets [{:keys [folder extensions]}]
  (map #(str/replace-first % folder "")
       (recursively-search (.internal Gdx/files folder)
                           extensions)))
