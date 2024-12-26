(ns gdl.assets
  (:require [clojure.gdx :refer [asset-manager dispose]]
            [clojure.gdx.assets :as assets]
            [clojure.gdx.file-handle :as fh]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)))

(defn- load-all [manager assets]
  (doseq [[file asset-type] assets]
    (assets/load manager file asset-type))
  (assets/finish-loading manager))

(defn- recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list (.internal Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(def ^:private asset-type-exts {:sound   #{"wav"}
                                :texture #{"png" "bmp"}})

(defn manager [folder]
  (doto (asset-manager)
    (load-all (for [[asset-type exts] asset-type-exts
                    file (map #(str/replace-first % folder "")
                              (recursively-search folder exts))]
                [file asset-type]))))

(defn cleanup [manager]
  (dispose manager))

(defn all-of-type
  "Returns all asset paths with the specific asset-type."
  [manager asset-type]
  (filter #(= (assets/type manager %) asset-type)
          (assets/names manager)))
