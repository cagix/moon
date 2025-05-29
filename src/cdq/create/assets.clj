(ns cdq.create.assets
  (:require [clojure.gdx.assets.asset-manager :as asset-manager]
            [clojure.string :as str]
            [gdl.assets]
            [gdl.files :as files])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)))

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

(defn do! [{:keys [ctx/files
                   ctx/config]
            :as ctx}]
  (assoc ctx :ctx/assets
         (let [{:keys [folder
                       asset-type-extensions]} (:assets config)
               manager (asset-manager/create
                        (for [[asset-type extensions] asset-type-extensions
                              file (map #(str/replace-first % folder "")
                                        (recursively-search (files/internal files folder) extensions))]
                          [file (case asset-type
                                  :sound Sound
                                  :texture Texture)]))]
           (reify gdl.assets/Assets
             (sound [assets path]
               (asset-manager/safe-get manager path))

             (texture [assets path]
               (asset-manager/safe-get manager path))

             (all-sounds [assets]
               (asset-manager/all-of-type manager Sound))

             (all-textures [assets]
               (asset-manager/all-of-type manager Texture))))))
