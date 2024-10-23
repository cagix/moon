(ns moon.assets
  (:refer-clojure :exclude [load])
  (:require [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.utils :as utils]))

(declare manager)

(defn- search [folder]
  (for [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                      [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (utils/recursively-search folder exts))]
    [file class]))

(defn load [folder]
  (.bindRoot #'manager (assets/manager (search folder))))

(defn dispose []
  (utils/dispose manager))
