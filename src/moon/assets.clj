(ns moon.assets
  (:require [gdl.assets :as assets]
            [gdl.utils :as utils])
  (:import (com.badlogic.gdx.audio Sound)))

(declare manager)

(defn init [folder]
  (bind-root #'manager (assets/manager (assets/search folder))))

(defn dispose []
  (utils/dispose manager))

(defn play-sound! [path]
  (Sound/.play (get manager path)))

(defn all-of-class [class]
  (assets/of-class manager class))
