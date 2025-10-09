(ns clojure.scene2d.build.group
  (:require [clojure.gdx.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn set-opts! [group opts]
  (run! (fn [actor]
          (Group/.addActor group actor))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defn create [opts]
  (doto (Group.)
    (set-opts! opts)))
