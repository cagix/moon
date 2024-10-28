(ns moon.assets
  (:refer-clojure :exclude [load])
  (:require [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.utils :as utils :refer [dispose]]
            [moon.app :as app])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

(declare manager)

(defn- search [folder]
  (for [[class exts] [[Sound   #{"wav"}]
                      [Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (utils/recursively-search folder exts))]
    [file class]))

(defc :moon.assets
  (app/create [[_ folder]]
    (bind-root #'manager (assets/manager (search folder))))

  (app/dispose [_]
    (dispose manager)))

(defn play-sound! [path]
  (Sound/.play (get manager path)))

(defn all-of-class [class]
  (assets/of-class manager class))
