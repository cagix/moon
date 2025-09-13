(ns clojure.gdx.scene2d.ui.widget
  (:require [clojure.gdx.scene2d.actor.decl :as actor.decl]))

(defn set-opts! [widget opts]
  (actor.decl/set-actor-opts! widget opts))
