(ns cdq.ui.build.group
  (:require [cdq.ui.build.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn set-opts! [group opts]
  (run! (fn [actor]
          (Group/.addActor group actor))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defn create [opts]
  (doto (Group.)
    (set-opts! opts)))
