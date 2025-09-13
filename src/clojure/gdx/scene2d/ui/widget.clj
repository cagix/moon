(ns clojure.gdx.scene2d.ui.widget
  (:require [clojure.gdx.scene2d :as scene2d]))

(defn set-opts! [widget opts]
  (scene2d/set-actor-opts! widget opts))
