(ns gdl.assets
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.assets.manager :as manager]
            [clojure.gdx.files :as files]
            [clojure.gdx.files.file-handle :as fh]
            [clojure.string :as str]
            [gdl.audio.sound])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

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

(defn create [{:keys [folder asset-type-extensions]}]
  (manager/create
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (recursively-search (files/internal (gdx/files) folder) extensions))]
     [file (case asset-type
             :sound Sound
             :texture Texture)])))

(defn sound [assets path]
  (let [sound (manager/safe-get assets path)]
    (reify gdl.audio.sound/Sound
      (play! [_]
        (Sound/.play sound)))))

(defn texture [assets path]
  (manager/safe-get assets path))

(defn all-sounds [assets]
  (manager/all-of-type assets Sound))

(defn all-textures [assets]
  (manager/all-of-type assets Texture))
