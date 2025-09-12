(ns clojure.gdx.scene2d.ui.widget
  (:require [clojure.gdx.scene2d.actor.opts :as opts]))

(defn set-opts! [widget opts]
  (opts/set-actor-opts! widget opts))
