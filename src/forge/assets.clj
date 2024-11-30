(ns forge.assets
  (:refer-clojure :exclude [get])
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.assets :as assets]
            [clojure.string :as str]
            [forge.utils.files :as files])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

(declare ^:private manager)

(defn init [folder]
  (bind-root #'manager (assets/manager))
  (doseq [[class exts] [[Sound   #{"wav"}]
                        [Texture #{"png" "bmp"}]]
          file (map #(str/replace-first % folder "")
                    (files/recursively-search (gdx/internal-file folder) exts))]
    (assets/load manager file class))
  (assets/finish-loading manager))

(defn dispose []
  (.dispose manager))

(defn get [asset-path]
  (clojure.core/get manager asset-path))

(defn- all-of-class
  "Returns all asset paths with the specific class."
  [class]
  (filter #(= (assets/asset-type manager %) class)
          (assets/asset-names manager)))

(defn all-sounds   [] (all-of-class Sound))
(defn all-textures [] (all-of-class Texture))
