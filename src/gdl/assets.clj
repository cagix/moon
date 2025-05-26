(ns gdl.assets
  (:require [clojure.gdx.assets.asset-manager :as asset-manager]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.files FileHandle)))

(defn- recursively-search [^FileHandle folder extensions]
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

(defn create [{:keys [folder
                      asset-type-extensions]}]
  (asset-manager/create
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (recursively-search (.internal Gdx/files folder) extensions))]
     [file asset-type])))

(defn sound [assets path]
  (assets path))

(defn texture [assets path]
  (assets path))

(defn all-sounds [assets]
  (asset-manager/all-of-type assets :sound))

(defn all-textures [assets]
  (asset-manager/all-of-type assets :texture))
