(ns cdq.create.assets
  (:require [cdq.g :as g]
            [clojure.gdx.assets.asset-manager :as asset-manager]
            [clojure.string :as str]
            [gdl.audio.sound])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.audio Sound)
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

(defn- make-assets [{:keys [folder
                            asset-type-extensions]}]
  (asset-manager/create
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (recursively-search (.internal Gdx/files folder) extensions))]
     [file (case asset-type
             :sound Sound
             :texture Texture)])))

(def ^:private -k :ctx/assets)

(defn do! [{:keys [ctx/config]
            :as ctx}]
  (extend (class ctx)
    g/Assets
    {:sound (fn [ctx path]
              (let [sound (asset-manager/safe-get (-k ctx) path)]
                (reify gdl.audio.sound/Sound
                  (play! [_]
                    (Sound/.play sound)))))
     :texture (fn [ctx path]
                (asset-manager/safe-get (-k ctx) path))
     :all-sounds (fn [ctx]
                   (asset-manager/all-of-type (-k ctx) Sound))
     :all-textures (fn [ctx]
                     (asset-manager/all-of-type (-k ctx) Texture))})
  (assoc ctx -k (make-assets (:assets config))))
