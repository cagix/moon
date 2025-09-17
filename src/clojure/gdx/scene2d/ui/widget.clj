(ns clojure.gdx.scene2d.ui.widget
  (:require [clojure.gdx.scene2d]))

(defn set-opts! [widget opts]
  (clojure.gdx.scene2d/actor-opts! widget opts))
