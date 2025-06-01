(ns gdl.create.assets
  (:require [clojure.gdx.assets.manager :as manager]
            [clojure.gdx.utils.files :as utils.files]
            [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.files :as files]
            [gdl.audio.sound])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.utils Disposable)))

(defn- create-assets [files {:keys [folder asset-type-extensions]}]
  (let [manager (manager/create
                 (for [[asset-type extensions] asset-type-extensions
                       file (map #(str/replace-first % folder "")
                                 (utils.files/recursively-search (files/internal files folder)
                                                                 extensions))]
                   [file (case asset-type
                           :sound Sound
                           :texture Texture)]))]
    (reify
      Disposable
      (dispose [_]
        (Disposable/.dispose manager))

      ;clojure.lang.IFn
      ;(invoke [_ path])
      ; => but then how 2 do with sounds?

      assets/Assets
      (sound [_ path]
        (let [sound (manager/safe-get manager path)]
          (reify gdl.audio.sound/Sound
            (play! [_]
              (Sound/.play sound)))))

      (texture [_ path]
        (manager/safe-get manager path))

      (all-sounds [_]
        (manager/all-of-type manager Sound))

      (all-textures [_]
        (manager/all-of-type manager Texture)))))

(defn do! [{:keys [ctx/config
                   ctx/files]
            :as ctx}]
  (assoc ctx :ctx/assets (create-assets files (:assets config))))
