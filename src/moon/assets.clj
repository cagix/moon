(ns moon.assets
  (:require [gdl.assets :as assets])
  (:import (com.badlogic.gdx.audio Sound)))

(declare manager)

(defn init []
  (bind-root #'manager (assets/manager (assets/search "resources/"))))

(defn dispose []
  (.dispose manager))

(defn play-sound! [path]
  (Sound/.play (get manager path)))

(defn all-of-class [class]
  (assets/of-class manager class))
